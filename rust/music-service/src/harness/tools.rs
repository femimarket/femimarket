//! The real tools: femi's own `handle_*` generation fns, in-process.
//!
//! Each tool call becomes an `ApiAction`, dispatched through femi's public
//! `api_handler` (so all provider/config logic is reused unchanged), then the
//! returned base64 is decoded, persisted to a `MediaStore`, and reduced to a
//! **reference** (item id + metadata). Bytes never re-enter the transcript.

use crate::api::{ApiAction, Model};
use crate::harness::agent::{HarnessError, Item, ToolOutcome, Tools};
use crate::harness::spine::FaultClass;
use base64::Engine;
use serde_json::{json, Value};
use std::path::PathBuf;
use uuid::Uuid;

/// Where produced media bytes live, addressed by item id (the file name).
pub trait MediaStore: Send + Sync {
    fn save(&self, bytes: &[u8], ext: &str) -> std::io::Result<String>;
    fn load(&self, item_id: &str) -> std::io::Result<Vec<u8>>;
}

/// Disk-backed store under a directory.
pub struct DiskStore {
    pub dir: PathBuf,
}
impl MediaStore for DiskStore {
    fn save(&self, bytes: &[u8], ext: &str) -> std::io::Result<String> {
        std::fs::create_dir_all(&self.dir)?;
        let name = format!("{}.{ext}", Uuid::now_v7());
        std::fs::write(self.dir.join(&name), bytes)?;
        Ok(name)
    }
    fn load(&self, item_id: &str) -> std::io::Result<Vec<u8>> {
        std::fs::read(self.dir.join(item_id))
    }
}

pub struct FemiTools<S: MediaStore> {
    pub store: S,
}

fn b64(bytes: &[u8]) -> String {
    base64::engine::general_purpose::STANDARD.encode(bytes)
}
fn permanent(msg: impl Into<String>) -> HarnessError {
    HarnessError::new(FaultClass::ToolPermanent, msg)
}
fn transient(msg: impl Into<String>) -> HarnessError {
    HarnessError::new(FaultClass::ToolTransient, msg)
}

impl<S: MediaStore> FemiTools<S> {
    /// Build the `ApiAction` for a tool call, resolving any input item ids to bytes.
    fn action_for(&self, name: &str, args: &Value) -> Result<ApiAction, HarnessError> {
        let s = |k: &str| args.get(k).and_then(|v| v.as_str()).map(str::to_string);
        let load_b64 = |id: &str| -> Result<String, HarnessError> {
            self.store
                .load(id)
                .map(|b| b64(&b))
                .map_err(|e| permanent(format!("unknown item '{id}': {e}")))
        };
        match name {
            "generate_image" => {
                let prompt = s("prompt").ok_or_else(|| permanent("missing 'prompt'"))?;
                Ok(ApiAction::Flux2Pro { prompt, fal_request_id: String::new(), file: String::new() })
            }
            "edit_image" => {
                let id = s("item_id").ok_or_else(|| permanent("missing 'item_id'"))?;
                let instruction = s("instruction").ok_or_else(|| permanent("missing 'instruction'"))?;
                Ok(ApiAction::Flux2DevI2I {
                    image: load_b64(&id)?,
                    prompt: instruction,
                    comfy_request_id: String::new(),
                    file: String::new(),
                })
            }
            "combine" => {
                let a = s("item_id_a").ok_or_else(|| permanent("missing 'item_id_a'"))?;
                let b = s("item_id_b").ok_or_else(|| permanent("missing 'item_id_b'"))?;
                Ok(ApiAction::Flux2KleinI2I {
                    image: load_b64(&a)?,
                    image2: load_b64(&b)?,
                    prompt: s("instruction").unwrap_or_default(),
                    comfy_request_id: String::new(),
                    file: String::new(),
                })
            }
            "make_video" => {
                let prompt = s("prompt").ok_or_else(|| permanent("missing 'prompt'"))?;
                let image = match s("item_id") {
                    Some(id) => load_b64(&id)?,
                    None => String::new(),
                };
                Ok(ApiAction::Ltx2_3A2V {
                    image,
                    audio: String::new(),
                    prompt,
                    comfy_request_id: String::new(),
                    file: String::new(),
                })
            }
            "transcribe" => {
                let id = s("item_id").ok_or_else(|| permanent("missing 'item_id'"))?;
                Ok(ApiAction::Qwen3AsrFlash { audio: load_b64(&id)?, lyrics: String::new() })
            }
            other => Err(permanent(format!("unknown tool '{other}'"))),
        }
    }
}

/// Given the action returned by `api_handler`, produce the (summary, item) outcome —
/// decoding + persisting any base64 to the store, so only a reference escapes.
fn outcome_from<S: MediaStore>(
    store: &S,
    action: ApiAction,
) -> Result<ToolOutcome, HarnessError> {
    let save_media = |file: String, ext: &str, kind: &str, mime: &str| -> Result<ToolOutcome, HarnessError> {
        let bytes = base64::engine::general_purpose::STANDARD
            .decode(file.as_bytes())
            .map_err(|e| transient(format!("bad base64 from provider: {e}")))?;
        let item_id = store
            .save(&bytes, ext)
            .map_err(|e| transient(format!("persist failed: {e}")))?;
        Ok(ToolOutcome {
            summary: format!("produced {item_id} ({mime}, {} bytes) — reference", bytes.len()),
            is_error: false,
            item: Some(Item { item_id, kind: kind.to_string(), mime: mime.to_string() }),
        })
    };
    match action {
        ApiAction::Flux2Pro { file, .. }
        | ApiAction::ZImageTurbo { file, .. }
        | ApiAction::NanoBanana2 { file, .. }
        | ApiAction::Flux2DevI2I { file, .. }
        | ApiAction::Flux2KleinI2I { file, .. } => save_media(file, "png", "image", "image/png"),
        ApiAction::Ltx2_3A2V { file, .. } => save_media(file, "mp4", "video", "video/mp4"),
        ApiAction::Qwen3AsrFlash { lyrics, .. } => Ok(ToolOutcome {
            summary: format!("transcript: {lyrics}"),
            is_error: false,
            item: None,
        }),
        _ => Err(transient("unexpected action returned")),
    }
}

impl<S: MediaStore> Tools for FemiTools<S> {
    fn run(
        &self,
        _id: &str,
        name: &str,
        args: &str,
    ) -> impl std::future::Future<Output = Result<ToolOutcome, HarnessError>> + Send {
        // Parse + resolve inputs synchronously; only the owned Model + &store cross await.
        let parsed: Result<ApiAction, HarnessError> = (|| {
            let v: Value = serde_json::from_str(args)
                .map_err(|e| permanent(format!("bad tool args: {e}")))?;
            self.action_for(name, &v)
        })();
        let store = &self.store;

        async move {
            let action = parsed?;
            let model = Model { id: Uuid::now_v7(), user_id: "harness".to_string(), action };
            let out = crate::api::handler::api_handler(axum::Json(model))
                .await
                .map_err(|(_s, msg)| transient(format!("femi tool: {msg}")))?;
            outcome_from(store, out.0.action)
        }
    }
}

/// The OpenAI-format tool schema the model is told about (the five femi verbs).
pub fn femi_tool_schema() -> Value {
    json!([
        {"type":"function","function":{
            "name":"generate_image",
            "description":"Generate a NEW image from a text prompt.",
            "parameters":{"type":"object","properties":{"prompt":{"type":"string","description":"the image to generate, described in detail"}},"required":["prompt"]}}},
        {"type":"function","function":{
            "name":"edit_image",
            "description":"Edit an EXISTING image (by item_id) with an instruction; produces a new image.",
            "parameters":{"type":"object","properties":{"item_id":{"type":"string"},"instruction":{"type":"string"}},"required":["item_id","instruction"]}}},
        {"type":"function","function":{
            "name":"combine",
            "description":"Combine TWO existing images (by item_id) into one, optionally guided by an instruction.",
            "parameters":{"type":"object","properties":{"item_id_a":{"type":"string"},"item_id_b":{"type":"string"},"instruction":{"type":"string"}},"required":["item_id_a","item_id_b"]}}},
        {"type":"function","function":{
            "name":"make_video",
            "description":"Generate a short video from a prompt, optionally animating an existing image (by item_id).",
            "parameters":{"type":"object","properties":{"item_id":{"type":"string"},"prompt":{"type":"string"}},"required":["prompt"]}}},
        {"type":"function","function":{
            "name":"transcribe",
            "description":"Transcribe the audio of an existing item (by item_id) to text.",
            "parameters":{"type":"object","properties":{"item_id":{"type":"string"}},"required":["item_id"]}}}
    ])
}

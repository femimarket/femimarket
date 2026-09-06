use crate::api::upload_handler::*;
use crate::api::{Model};
use crate::upload_handler;
use axum::http::StatusCode;
use base64::Engine;
use qwen_asr::align::forced_align;
use qwen_asr::context::QwenCtx;
use serde::{Deserialize, Serialize};
use std::env::temp_dir;
use std::sync::{LazyLock, Mutex};
use axum::Json;
use utoipa::ToSchema;

// The local Qwen3-ASR-0.6B model (dir of *.safetensors + vocab.json, set via
// `--qwen-asr-0-6b-dir`). Loaded once on first use and reused — the local,
// no-network alternative to the Alibaba DashScope flash ASR. `transcribe_audio`
// and `forced_align` take `&mut`, so a single Mutex serializes the CPU-bound
// calls, which we only ever touch from inside `spawn_blocking`.
static ASR: LazyLock<Mutex<QwenCtx>> = LazyLock::new(|| {
    let dir = crate::commands::serve::QWEN_ASR_0_6B_DIR_LOCK.get().unwrap();
    Mutex::new(QwenCtx::load(dir).expect("failed to load Qwen ASR 0.6B model"))
});

/// One word with its start/end timestamps (seconds), from forced alignment.
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, ToSchema)]
pub struct WordAlignment {
    pub text: String,
    pub start: f64,
    pub end: f64,
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, ToSchema)]
pub struct DtoIn {
    /// input audio — 16 kHz mono f32, raw little-endian samples; data URI (web) or raw base64 (android/ios)
    audio: String,
    /// transcribed lyrics
    lyrics: String,
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, ToSchema)]
pub struct DtoOut {
    words: Vec<WordAlignment>,
}

pub async fn handle(
    req: DtoIn,
) -> Result<DtoOut, (StatusCode, String)> {

    let bytes = tokio::fs::read(temp_dir().join(&req.audio)).await
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, format!("reading audio file '{}': {e}", req.audio)))?;
    let lyrics = req.lyrics;

    let samples: Vec<f32> = bytes
        .chunks_exact(4)
        .map(|c| f32::from_le_bytes([c[0], c[1], c[2], c[3]]))
        .collect();

    let (_, words) = tokio::task::spawn_blocking(move || -> Result<(String, Vec<WordAlignment>), (StatusCode, String)> {
        let mut ctx = ASR.lock()
            .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, format!("asr mutex poisoned: {e}")))?;
        let words = forced_align(&mut ctx, &samples, &lyrics, "English")
            .ok_or_else(|| (StatusCode::INTERNAL_SERVER_ERROR, "forced_align returned None".to_string()))?
            .into_iter()
            .map(|r| WordAlignment {
                text: r.text,
                start: r.start_ms as f64 / 1000.0,
                end: r.end_ms as f64 / 1000.0,
            })
            .collect();
        Ok(("text".to_string(), words))
    }).await
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, format!("transcription task panicked: {e}")))??;
    // let lyrics = text.replace(".", ".\n").replace("!", "!\n").replace("?", "?\n");
    Ok(DtoOut{ words })
}

/// `POST /api` (JSON `Model`) entry point for the local Qwen3-ASR-0.6B forced
/// aligner. Same work as `handle` above, but audio arrives base64-encoded in the
/// `Qwen3Asr0_6B` action rather than as an uploaded file, and the aligned words
/// are written back into the action.
pub async fn handle_qwen3_asr_0_6b(mut req: Model) -> Result<axum::response::Response, (StatusCode, String)> {
    let Model::ForceAlignQwen306b { audio, mut lyrics, .. } = req.clone() else {
        return Err((StatusCode::BAD_REQUEST, "handle_qwen3_asr_0_6b requires a Qwen3Asr0_6B action".to_string()));
    };

    lyrics = lyrics
        .lines()
        .map(|line| {
            let trimmed = line.trim_end();
            if trimmed.is_empty() {
                String::new()
            } else {
                // Append a unique punctuation mark `|` to explicitly signal line breaks
                // to the downstream client without confusing it with inline periods.
                format!("{}|", trimmed)
            }
        })
        .collect::<Vec<_>>()
        .join("\n");

    let bytes = tokio::fs::read(temp_dir().join(&audio)).await
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, format!("reading audio file '{}': {e}", audio)))?;

    let samples: Vec<f32> = bytes
        .chunks_exact(4)
        .map(|c| f32::from_le_bytes([c[0], c[1], c[2], c[3]]))
        .collect();

    let (_, words) = tokio::task::spawn_blocking(move || -> Result<(String, Vec<WordAlignment>), (StatusCode, String)> {
        let mut ctx = ASR.lock()
            .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, format!("asr mutex poisoned: {e}")))?;
        let words = forced_align(&mut ctx, &samples, &lyrics, "English")
            .ok_or_else(|| (StatusCode::INTERNAL_SERVER_ERROR, "forced_align returned None".to_string()))?
            .into_iter()
            .map(|r| WordAlignment {
                text: r.text,
                start: r.start_ms as f64 / 1000.0,
                end: r.end_ms as f64 / 1000.0,
            })
            .collect();
        Ok(("text".to_string(), words))
    }).await
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, format!("transcription task panicked: {e}")))??;

    let mut final_words = Vec::new();
    for mut word in words {
        let end_time = word.end;
        let mut insert_newline = false;
        if word.text.ends_with('|') {
            insert_newline = true;
            word.text.pop(); // Remove the unique punctuation '|'
        }
        final_words.push(word);
        if insert_newline {
            final_words.push(WordAlignment {
                text: "\n".to_string(),
                start: end_time,
                end: end_time,
            });
        }
    }

    // let lyrics = text.replace(".", ".\n").replace("!", "!\n").replace("?", "?\n");
    if let Model::ForceAlignQwen306b { out_words: w, .. } = &mut req {
        *w = final_words;
    }

    Ok(axum::response::Response::builder()
        .header(axum::http::header::CONTENT_TYPE, "application/json")
        .body(axum::body::Body::from(serde_json::to_string(&req).unwrap()))
        .unwrap())
}



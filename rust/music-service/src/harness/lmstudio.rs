//! The real brain: local LM Studio (OpenAI-compatible), with the femi tool schema.
//!
//! Mirrors the existing `handle_qwen3_6_35b_a3b` transport but (a) advertises tools,
//! (b) parses `tool_calls`, (c) echoes the model's exact assistant message as
//! `provider_raw`, and (d) classifies transport failures into `FaultClass` so the
//! loop's bounded retry can engage on transient model-unavailability.

use crate::harness::agent::{Brain, HarnessError, Turn};
use crate::harness::spine::{ContentBlock, FaultClass, Msg};
use serde_json::{json, Value};

pub struct LmStudioBrain {
    pub base_url: String,
    pub model: String,
    /// OpenAI-format tools array the model may call (see `tools::femi_tool_schema`).
    pub tools: Value,
    pub client: reqwest::Client,
}

impl LmStudioBrain {
    pub fn new(tools: Value) -> Self {
        LmStudioBrain {
            base_url: "http://localhost:1234".to_string(),
            model: "qwen/qwen3.6-35b-a3b".to_string(), // the real loaded id
            tools,
            client: reqwest::Client::new(),
        }
    }
}

/// One transcript message → OpenAI wire shape. An assistant turn with opaque
/// `provider_raw` is echoed verbatim (KV-cache stability + exact-echo providers).
fn to_wire(m: &Msg) -> Value {
    if m.role == "assistant" {
        if let Some(raw) = &m.provider_raw {
            if let Ok(v) = serde_json::from_str::<Value>(raw) {
                return v;
            }
        }
    }
    if m.role == "tool" {
        if let Some(id) = &m.tool_call_id {
            let summary = m
                .blocks
                .iter()
                .find_map(|b| match b {
                    ContentBlock::ToolResult { summary, .. } => Some(summary.clone()),
                    _ => None,
                })
                .unwrap_or_default();
            return json!({ "role": "tool", "content": summary, "tool_call_id": id });
        }
    }
    let mut text = String::new();
    let mut tool_calls: Vec<Value> = Vec::new();
    for b in &m.blocks {
        match b {
            ContentBlock::Text { text: t } => text.push_str(t),
            ContentBlock::ToolUse { id, name, args } => tool_calls.push(json!({
                "id": id, "type": "function",
                "function": { "name": name, "arguments": args }
            })),
            _ => {}
        }
    }
    if !tool_calls.is_empty() {
        json!({ "role": m.role, "tool_calls": tool_calls, "content": Value::Null })
    } else {
        json!({ "role": m.role, "content": text })
    }
}

impl Brain for LmStudioBrain {
    fn turn(
        &self,
        messages: &[Msg],
    ) -> impl std::future::Future<Output = Result<Turn, HarnessError>> + Send {
        // Build everything the request needs synchronously, so the future owns it
        // (no borrow of `self`/`messages` held across the await → trivially Send).
        let wire: Vec<Value> = messages.iter().map(to_wire).collect();
        let url = format!("{}/v1/chat/completions", self.base_url);
        let body = json!({
            "model": self.model,
            "messages": wire,
            "tools": self.tools,
            "stream": false,
        });
        let client = self.client.clone();

        async move {
            let resp = client.post(&url).json(&body).send().await.map_err(|e| {
                // connect/timeout/transport → transient model-unavailability (retryable).
                HarnessError::new(FaultClass::ModelUnavailable, format!("POST {url}: {e}"))
            })?;
            let status = resp.status();
            if !status.is_success() {
                let fault = if status.is_server_error() || status.as_u16() == 429 {
                    FaultClass::ModelUnavailable // 5xx / rate-limit → retryable
                } else {
                    FaultClass::ModelProtocol // 4xx → our request is wrong; terminal
                };
                let text = resp.text().await.unwrap_or_default();
                return Err(HarnessError::new(fault, format!("lmstudio {status}: {text}")));
            }
            let v: Value = resp.json().await.map_err(|e| {
                HarnessError::new(FaultClass::ModelProtocol, format!("decode: {e}"))
            })?;
            let message = v.pointer("/choices/0/message").ok_or_else(|| {
                HarnessError::new(FaultClass::ModelProtocol, format!("no choices[0].message: {v}"))
            })?;

            let mut blocks: Vec<ContentBlock> = Vec::new();
            if let Some(content) = message.get("content").and_then(|c| c.as_str()) {
                if !content.trim().is_empty() {
                    blocks.push(ContentBlock::Text { text: content.to_string() });
                }
            }
            if let Some(tcs) = message.get("tool_calls").and_then(|t| t.as_array()) {
                for tc in tcs {
                    let id = tc.get("id").and_then(|x| x.as_str()).unwrap_or("").to_string();
                    let name = tc
                        .pointer("/function/name")
                        .and_then(|x| x.as_str())
                        .unwrap_or("")
                        .to_string();
                    let args = tc
                        .pointer("/function/arguments")
                        .and_then(|x| x.as_str())
                        .unwrap_or("{}")
                        .to_string();
                    if !name.is_empty() {
                        blocks.push(ContentBlock::ToolUse { id, name, args });
                    }
                }
            }

            Ok(Turn { blocks, provider_raw: Some(message.to_string()) })
        }
    }
}

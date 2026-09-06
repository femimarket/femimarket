use crate::api::{Model, ApiChatRole, ApiChatMessage};


// Same chat flow as handle_claude_sonnet4_6, but against a local LM Studio server
// (OpenAI-compatible, no auth) instead of the ngrok proxy. CHAT_MODEL must match
// the model id loaded in LM Studio; LMSTUDIO_URL is its default local endpoint.
const CHAT_MODEL: &str = "qwen3-6-35b-a3b";
const MAX_OUTPUT_TOKENS: u32 = 3000;
const LMSTUDIO_URL: &str = "http://localhost:1234";


pub async fn handle_qwen3_6_35b_a3b(mut row: Model) -> Result<Model, (axum::http::StatusCode, String)> {
    let Model::Qwen3_6_35bA3b { messages, .. } = row.clone()
    else {
        return Err((axum::http::StatusCode::BAD_REQUEST, "handle_qwen3_6_35b_a3b requires a Qwen3_6_35bA3b action".to_string()));
    };
    // count_tokens(&messages)?;

    // Map our typed enum to the OpenAI wire format (lowercase role strings).
    let wire: Vec<serde_json::Value> = messages
        .iter()
        .map(|m| {
            serde_json::json!({
                "role": match m.role { ApiChatRole::User => "user", ApiChatRole::Assistant => "assistant" },
                "content": m.content,
            })
        })
        .collect();

    let resp: serde_json::Value = reqwest::Client::new()
        .post(format!("{LMSTUDIO_URL}/v1/chat/completions"))
        .json(&serde_json::json!({
            "model": CHAT_MODEL,
            "messages": wire,
            "max_tokens": MAX_OUTPUT_TOKENS,
        }))
        .send()
        .await
        .map_err(|e| (axum::http::StatusCode::BAD_GATEWAY, format!("POST lmstudio /v1/chat/completions: {e}")))?
        .error_for_status()
        .map_err(|e| (axum::http::StatusCode::BAD_GATEWAY, format!("lmstudio non-2xx: {e}")))?
        .json()
        .await
        .map_err(|e| (axum::http::StatusCode::BAD_GATEWAY, format!("decoding lmstudio response: {e}")))?;

    let reply = resp.pointer("/choices/0/message/content")
        .and_then(|v| v.as_str())
        .map(str::to_string)
        .ok_or_else(|| (axum::http::StatusCode::BAD_GATEWAY, format!("lmstudio response missing choices[0].message.content: {resp}")))?;


    if let Model::Qwen3_6_35bA3b { messages, .. } = &mut row {
        messages.push(ApiChatMessage {
            role: ApiChatRole::Assistant,
            content: reply.clone(),
        });
        let prompt = messages.iter()
            .rev()
            .nth(1)                       // 0 = last, 1 = 2nd-to-last
            .map(|z| z.content.clone())
            .unwrap_or_default();
        tracing::Span::current().record("prompt", tracing::field::display(&prompt));
        tracing::Span::current().record("reply", tracing::field::display(&reply));
    }

    Ok(row)
}

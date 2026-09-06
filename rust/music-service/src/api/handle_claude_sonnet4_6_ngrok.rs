use crate::api::{ApiChatRole, ApiChatMessage, Model};


const CHAT_MODEL: &str = "anthropic/claude-sonnet-4-6";
const MAX_OUTPUT_TOKENS: u32 = 3000;
const THINKING_BUDGET: u32 = 2000;


pub async fn handle_claude_sonnet4_6(mut row: Model) -> Result<Model,(axum::http::StatusCode, String)> {
    let Model::ClaudeSonnet4_6 { messages, .. } = row.clone()
    else {
        return Err((axum::http::StatusCode::BAD_REQUEST, "handle_claude_sonnet4_6 requires a ClaudeSonnet4_6 action".to_string()));
    };
    // count_tokens(&messages)?;
    // Map our typed enum to the proxy's wire format (lowercase strings).
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
        .post(format!("{}/v1/chat/completions", crate::commands::serve::NGROK_AI_URL_LOCK.get().unwrap()))
        .bearer_auth(crate::commands::serve::NGROK_AI_LOCK.get().unwrap())
        .json(&serde_json::json!({
            "model": CHAT_MODEL,
            "messages": wire,
            "max_tokens": MAX_OUTPUT_TOKENS,
            "thinking": { "type": "enabled", "budget_tokens": THINKING_BUDGET },
        }))
        .send()
        .await
        .map_err(|e| (axum::http::StatusCode::BAD_GATEWAY, format!("POST llm-proxy /v1/chat/completions: {e}")))?
        .error_for_status()
        .map_err(|e| (axum::http::StatusCode::BAD_GATEWAY, format!("llm-proxy non-2xx: {e}")))?
        .json()
        .await
        .map_err(|e| (axum::http::StatusCode::BAD_GATEWAY, format!("decoding llm-proxy response: {e}")))?;

    let reply = resp.pointer("/choices/0/message/content")
        .and_then(|v| v.as_str())
        .map(str::to_string)
        .ok_or_else(|| (axum::http::StatusCode::BAD_GATEWAY, format!("llm-proxy response missing choices[0].message.content: {resp}")))?;


    if let Model::ClaudeSonnet4_6 { messages, .. } = &mut row {
        messages.push(ApiChatMessage{
            role: ApiChatRole::Assistant,
            content: reply,
        });
        let prompt = messages.iter()
            .rev()
            .nth(1)                       // 0 = last, 1 = 2nd-to-last
            .map(|z| z.content.clone())   // Option<String>
            .unwrap_or_default();
        let reply = messages.last().map(|z| z.content.clone()).unwrap_or_default();
        tracing::Span::current().record("prompt", tracing::field::display(&prompt));
        tracing::Span::current().record("reply", tracing::field::display(&reply));
    }

    Ok(row)
}




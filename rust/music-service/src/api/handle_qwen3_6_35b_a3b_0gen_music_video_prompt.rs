use crate::api::{Model};

// Same LM Studio (local, OpenAI-compatible) call as handle_qwen3_6_35b_a3b, but a
// fixed task instruction instead of relayed chat messages.
const CHAT_MODEL: &str = "qwen3-6-35b-a3b";
const MAX_OUTPUT_TOKENS: u32 = 3000;
const LMSTUDIO_URL: &str = "http://localhost:1234";

pub async fn handle_qwen3_6_35b_a3b_0gen_music_video_prompt(mut row: Model) -> Result<Model, (axum::http::StatusCode, String)> {
    let Model::Qwen3_6_35bA3b0GenMusicVideoPrompt { .. } = row.clone()
    else {
        return Err((axum::http::StatusCode::BAD_REQUEST, "handle_qwen3_6_35b_a3b_0gen_music_video_prompt requires a Qwen3_6_35bA3b0GenMusicVideoPrompt action".to_string()));
    };


    // let content = "Generate a single, vivid image-generation prompt for a music video shot. Reply with only the prompt.".to_string();
    let content = "Generate cinematic music video still, vivid color grade, dramatic lighting, expressive performer mid-motion, shallow depth of field, 35mm film grain, emotional and atmospheric. Reply with only the prompt. Under 100 words. Return only the prompt itself — no title, no preamble, no commentary, no trailing notes, no markdown".to_string();

    let resp: serde_json::Value = reqwest::Client::new()
        .post(format!("{LMSTUDIO_URL}/v1/chat/completions"))
        .json(&serde_json::json!({
            "model": CHAT_MODEL,
            "messages": [{ "role": "user", "content": content }],
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
    tracing::Span::current().record("reply", tracing::field::display(&reply));

    if let Model::Qwen3_6_35bA3b0GenMusicVideoPrompt { result, .. } = &mut row {
        *result = reply;
    }
    Ok(row)
}

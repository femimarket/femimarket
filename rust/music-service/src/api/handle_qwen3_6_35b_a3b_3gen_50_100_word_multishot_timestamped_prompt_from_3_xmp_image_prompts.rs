use crate::api::{Model};

const CHAT_MODEL: &str = "qwen3-6-35b-a3b";
const MAX_OUTPUT_TOKENS: u32 = 3000;
const LMSTUDIO_URL: &str = "http://localhost:1234";

pub async fn handle_qwen3_6_35b_a3b_3gen_50_100_word_multishot_timestamped_prompt_from_3_xmp_image_prompts(mut row: Model) -> Result<Model, (axum::http::StatusCode, String)> {
    let Model::Qwen3_6_35bA3b3Gen50100WordMultishotTimestampedPromptFrom3XmpImagePrompts { xmp_prompt, xmp_prompt2, xmp_prompt3, .. } = row.clone()
    else {
        return Err((axum::http::StatusCode::BAD_REQUEST, "handle_qwen3_6_35b_a3b_3gen_50_100_word_multishot_timestamped_prompt_from_3_xmp_image_prompts requires a Qwen3_6_35bA3b3Gen50100WordMultishotTimestampedPromptFrom3XmpImagePrompts action".to_string()));
    };

    let content = format!("Given these three image-generation prompts, write a 50-100 word multi-shot, timestamped video prompt (e.g. `00:00 ...`, `00:15 ...`, `00:30 ...`). Reply with only the video prompt.\n\nPrompt 1:\n{xmp_prompt}\n\nPrompt 2:\n{xmp_prompt2}\n\nPrompt 3:\n{xmp_prompt3}");

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

    if let Model::Qwen3_6_35bA3b3Gen50100WordMultishotTimestampedPromptFrom3XmpImagePrompts { result, .. } = &mut row {
        *result = reply;
    }
    Ok(row)
}

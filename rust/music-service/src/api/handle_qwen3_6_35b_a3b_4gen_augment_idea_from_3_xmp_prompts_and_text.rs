use crate::api::{Model};

const CHAT_MODEL: &str = "qwen3-6-35b-a3b";
const MAX_OUTPUT_TOKENS: u32 = 3000;
const LMSTUDIO_URL: &str = "http://localhost:1234";

pub async fn handle_qwen3_6_35b_a3b_4gen_augment_idea_from_3_xmp_prompts_and_text(mut row: Model) -> Result<Model, (axum::http::StatusCode, String)> {
    let Model::Qwen3_6_35bA3b4GenAugmentIdeaFrom3XmpPromptsAndText { xmp_prompt, xmp_prompt2, xmp_prompt3, text, .. } = row.clone()
    else {
        return Err((axum::http::StatusCode::BAD_REQUEST, "handle_qwen3_6_35b_a3b_4gen_augment_idea_from_3_xmp_prompts_and_text requires a Qwen3_6_35bA3b4GenAugmentIdeaFrom3XmpPromptsAndText action".to_string()));
    };

    let content = format!("Given these three image-generation prompts and this direction, augment the idea into a single new image-generation prompt. Reply with only the new prompt.\n\nPrompt 1:\n{xmp_prompt}\n\nPrompt 2:\n{xmp_prompt2}\n\nPrompt 3:\n{xmp_prompt3}\n\nDirection:\n{text}");

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

    if let Model::Qwen3_6_35bA3b4GenAugmentIdeaFrom3XmpPromptsAndText { result, .. } = &mut row {
        *result = reply;
    }
    Ok(row)
}

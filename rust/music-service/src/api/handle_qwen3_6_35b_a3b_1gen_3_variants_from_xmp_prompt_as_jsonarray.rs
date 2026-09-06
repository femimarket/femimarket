use crate::api::{Model};

const CHAT_MODEL: &str = "qwen3-6-35b-a3b";
const MAX_OUTPUT_TOKENS: u32 = 3000;
const LMSTUDIO_URL: &str = "http://localhost:1234";

pub async fn handle_qwen3_6_35b_a3b_1gen_3_variants_from_xmp_prompt_as_jsonarray(mut row: Model) -> Result<Model, (axum::http::StatusCode, String)> {
    let Model::Qwen3_6_35bA3b1Gen3VariantsFromXmpPromptAsJsonarray { xmp_prompt, .. } = row.clone()
    else {
        return Err((axum::http::StatusCode::BAD_REQUEST, "handle_qwen3_6_35b_a3b_1gen_3_variants_from_xmp_prompt_as_jsonarray requires a Qwen3_6_35bA3b1Gen3VariantsFromXmpPromptAsJsonarray action".to_string()));
    };

    let content = format!("Given this image-generation prompt, produce 3 distinct variant prompts. Reply with only a JSON array of 3 strings.\n\nPrompt:\n{xmp_prompt}");

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

    if let Model::Qwen3_6_35bA3b1Gen3VariantsFromXmpPromptAsJsonarray { result, .. } = &mut row {
        *result = reply;
    }
    Ok(row)
}

use crate::api::{Model};
use axum::http::StatusCode;
use base64::Engine;





// Shared Alibaba DashScope (Qwen3-ASR) transcription: take the row's TranscribeSong
// audio (base64 — data URI or raw), POST it as a base64 data URI to
// <base>/chat/completions, and get the transcription back on the same connection.
// Break after sentence punctuation so it reads as multi-line lyrics, then return it
// on the action's `lyrics`.
pub async fn qwen3_asr_flash(mut row: Model) -> Result<Model, (StatusCode, String)> {
    let Model::Qwen3AsrFlash { audio, .. } = &row
    else {
        return Err((StatusCode::BAD_REQUEST, "qwen3_asr_flash requires a TranscribeSong action".to_string()));
    };
    // Alibaba needs a real container (mp3/m4a/wav). Strip any data-URI prefix and sniff
    // the mime off the decoded bytes since there's no filename to key off.
    let b64 = audio.rsplit(',').next().unwrap_or(audio.as_str());
    let bytes = base64::engine::general_purpose::STANDARD.decode(b64)
        .map_err(|e| (StatusCode::BAD_REQUEST, format!("decoding base64 audio: {e}")))?;
    let mime = infer::get(&bytes).map(|t| t.mime_type()).unwrap_or("audio/mpeg");
    let body = serde_json::json!({
        "model": "qwen3-asr-flash-2026-02-10",
        "messages": [{
            "role": "user",
            "content": [{
                "type": "input_audio",
                "input_audio": { "data": format!("data:{mime};base64,{b64}") }
            }]
        }]
    });
    let url = format!("{}/chat/completions", crate::commands::serve::ALIBABA_STUDIO_BASE_LOCK.get().unwrap());
    let resp = reqwest::Client::new()
        .post(&url)
        .header(reqwest::header::AUTHORIZATION, format!("Bearer {}", crate::commands::serve::ALIBABA_STUDIO_API_LOCK.get().unwrap()))
        .json(&body)
        .send()
        .await
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, format!("POST {url}: {e}")))?;
    let status = resp.status();
    let payload: serde_json::Value = resp.json().await
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, format!("decoding dashscope response: {e}")))?;
    if !status.is_success() {
        return Err((StatusCode::INTERNAL_SERVER_ERROR, format!("provider HTTP {status}: {payload}")));
    }
    let text = payload.pointer("/choices/0/message/content").and_then(|v| v.as_str())
        .ok_or_else(|| (StatusCode::INTERNAL_SERVER_ERROR, format!("dashscope response missing choices[0].message.content: {payload}")))?;
    let lyrics = text.replace(".", ".\n").replace("!", "!\n").replace("?", "?\n");
    match &mut row {
        Model::Qwen3AsrFlash { lyrics: l, .. } => *l = lyrics,
        _ => return Err((StatusCode::INTERNAL_SERVER_ERROR, "qwen3_asr_flash called with a non-TranscribeSong action".to_string())),
    }
    Ok(row)
}

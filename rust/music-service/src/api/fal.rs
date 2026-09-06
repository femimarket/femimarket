use crate::api::{Model};
use axum::http::StatusCode;
use base64::Engine;
use serde::Serialize;





// Shared, model-agnostic fal SYNC call: POST the model fn's JSON body straight to
// fal.run/<model> and get the result back on the same connection — no queue, no
// webhook, no request id. Download the output image and return it as base64 on the
// action's `file`, marking the row Completed.
pub async fn fal_submit(mut row: Model, url: &str, body: impl Serialize) -> Result<axum::body::Body, (StatusCode, String)> {
    let resp = reqwest::Client::new()
        .post(url)
        .header(reqwest::header::AUTHORIZATION, format!("Key {}", crate::commands::serve::FAL_KEY_LOCK.get().unwrap()))
        .json(&body)
        .send()
        .await
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, format!("POST {url}: {e}")))?;
    let status = resp.status();
    let payload: serde_json::Value = resp.json().await
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, format!("decoding fal response: {e}")))?;
    if !status.is_success() {
        return Err((StatusCode::INTERNAL_SERVER_ERROR, format!("provider HTTP {status}: {payload}")));
    }
    let img_url = payload.pointer("/images/0/url").and_then(|v| v.as_str())
        .ok_or_else(|| (StatusCode::INTERNAL_SERVER_ERROR, format!("fal response missing images[0].url: {payload}")))?;
    // Download the result and return it as base64 (no disk, no re-host).
    let bytes = reqwest::get(img_url).await
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, format!("GET {img_url}: {e}")))?
        .bytes_stream();
    Ok(axum::body::Body::from_stream(bytes))



    // let bytes = client
    //     .get(format!(
    //         "{COMFY_BASE_URL}/api/view?filename={output_filename}&type=output"
    //     ))
    //     .header("X-API-Key", crate::commands::serve::COMFY_KEY_LOCK.get().unwrap())
    //     .send()
    //     .await
    //     .map_err(|e| {
    //         (
    //             StatusCode::INTERNAL_SERVER_ERROR,
    //             format!("GET output {output_filename}: {e}"),
    //         )
    //     })?.bytes_stream();
    // return Ok(axum::body::Body::from_stream(bytes))
    //
    //
    //
    // let data = base64::engine::general_purpose::STANDARD.encode(&bytes);
    // match &mut row {
    //     Model::ZImageTurbo { file, .. }
    //     | Model::NanoBanana2 { file, .. }
    //     | Model::Flux2Pro { file, .. } => *file = data,
    //     _ => return Err((StatusCode::INTERNAL_SERVER_ERROR, "fal_submit called with a non-fal action".to_string())),
    // }
    // Ok(row)
}



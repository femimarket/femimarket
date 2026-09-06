use axum::http::StatusCode;
use crate::api::{fal, Model, comfyui};


pub async fn handle_z_image_turbo(req: Model) -> Result<axum::response::Response, (StatusCode, String)> {
    let Model::ZImageTurbo { prompt, .. } = req.clone()
    else {
        return Err((StatusCode::BAD_REQUEST, "handle_z_image_turbo requires a ZImageTurbo action".to_string()));
    };
    let body = serde_json::json!({ "prompt": prompt, "output_format": "png" });
    let rr = fal::fal_submit(req, "https://fal.run/fal-ai/z-image/turbo", body).await?;
    Ok(axum::response::Response::builder()
        .header(axum::http::header::CONTENT_TYPE, "image/png")
        .body(rr)
        .unwrap())
}

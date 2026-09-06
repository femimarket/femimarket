use axum::http::StatusCode;
use crate::api::{comfyui, Model};




pub async fn handle_flux2_dev_i2i(req: Model) -> Result<axum::response::Response, (StatusCode, String)> {
    // let rr = comfyui::comfyui_flux2_dev_i2i(req).await?;
    Ok(axum::response::Response::builder()
        .header(axum::http::header::CONTENT_TYPE, "image/png")
        .body(comfyui::comfyui_flux2_dev_i2i(req).await?)
        .unwrap())
}

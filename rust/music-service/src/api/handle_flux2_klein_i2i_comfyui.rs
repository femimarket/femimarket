use axum::http::StatusCode;
use crate::api::{comfyui, Model};


pub async fn handle_flux2_klein_i2i(req: Model) -> Result<Model, (StatusCode, String)> {
    let rr = comfyui::comfyui_flux2_klein_i2i(req).await?;
    Ok(rr)
}

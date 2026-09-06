use axum::http::StatusCode;
use crate::api::{comfyui, Model};




pub async fn handle_ltx2_3a2v(req: Model) -> Result<Model, (StatusCode, String)> {
    let rr = comfyui::comfyui_ltx2_3a2v(req).await?;
    
    Ok(rr)
}

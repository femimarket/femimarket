use axum::http::StatusCode;
use crate::api::{alibaba, Model};


pub async fn handle_qwen3_asr_flash(req: Model) -> Result<Model, (StatusCode, String)> {
    let rr = alibaba::qwen3_asr_flash(req).await?;
    Ok(rr)
}

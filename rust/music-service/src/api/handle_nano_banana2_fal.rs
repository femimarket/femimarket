use axum::http::StatusCode;
use crate::api::{fal, Model};


pub async fn handle_nano_banana2(req: Model) -> Result<Model, (StatusCode, String)> {
    let Model::NanoBanana2 { prompt, .. } = req.clone()
    else {
        return Err((StatusCode::BAD_REQUEST, "handle_nano_banana2 requires a NanoBanana2 action".to_string()));
    };
    let body = serde_json::json!({ "prompt": prompt, "output_format": "png" });
    let rr = fal::fal_submit(req, "https://fal.run/fal-ai/nano-banana-2", body).await?;
    unimplemented!()
    // Ok(rr)
}

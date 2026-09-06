use axum::http::StatusCode;
use crate::api::{fal, Model};


pub async fn handle_flux2_pro(req: Model) -> Result<Model, (StatusCode, String)> {
    let Model::Flux2Pro { prompt, .. } = req.clone()
    else {
        return Err((StatusCode::BAD_REQUEST, "handle_flux2_pro requires a Flux2Pro action".to_string()));
    };
    let body = serde_json::json!({ "prompt": prompt, "output_format": "png" });
    let rr = fal::fal_submit(req, "https://fal.run/fal-ai/flux-2-pro", body).await?;
    unimplemented!()
    // Ok(rr)
}

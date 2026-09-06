use axum::extract::Path;
use axum::Json;
use serde::{Deserialize, Serialize};
use utoipa::ToSchema;

/// Mirrors market.femi.models.AudioQA
#[derive(Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct AudioQa {
    pub question: String,
    pub answer: Option<String>,
}

#[derive(Deserialize, ToSchema)]
pub struct AudioFaqs {
    /// Media file name, resolved in the served directory
    pub file: String,
    /// Value stored verbatim as the id3 TXXX frame (JSON string for list/object fields)
    pub value: String,
}

#[utoipa::path(post, operation_id = "postFaqs", path = "/audio/faqs", request_body = AudioFaqs,
    responses((status = 200, description = "written", body = String)))]
pub async fn post(Json(body): Json<AudioFaqs>) -> &'static str {
    crate::id3_write(&body.file, "faqs", &body.value);
    "ok"
}

#[utoipa::path(get, operation_id = "getFaqs", path = "/audio/faqs/{file}",
    params(("file" = String, Path, description = "Media file name, resolved in the served directory")),
    responses((status = 200, description = "the file's faqs tag, empty when absent", body = Vec<AudioQa>)))]
pub async fn get(Path(file): Path<String>) -> Json<Vec<AudioQa>> {
    Json(crate::id3_read(&file, "faqs")
        .map(|s| serde_path_to_error::deserialize(&mut serde_json::Deserializer::from_str(&s)).unwrap_or_else(|e| panic!("{file}: faqs: {e}")))
        .unwrap_or_default())
}

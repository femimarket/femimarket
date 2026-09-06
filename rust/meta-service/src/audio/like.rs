use axum::extract::Path;
use axum::http::StatusCode;
use axum::response::{IntoResponse, Response};
use axum::Json;
use serde::Deserialize;
use utoipa::ToSchema;

#[derive(Deserialize, ToSchema)]
pub struct AudioLike {
    /// Media file name, resolved in the served directory
    pub file: String,
    /// Value stored verbatim as the id3 TXXX frame (JSON string for list/object fields)
    pub value: String,
}

#[utoipa::path(post, operation_id = "postLike", path = "/audio/like", request_body = AudioLike,
    responses((status = 200, description = "written", body = String)))]
pub async fn post(Json(body): Json<AudioLike>) -> &'static str {
    crate::id3_write(&body.file, "like", &body.value);
    "ok"
}

#[utoipa::path(get, operation_id = "getLike", path = "/audio/like/{file}",
    params(("file" = String, Path, description = "Media file name, resolved in the served directory")),
    responses((status = 200, description = "the file's like tag as plain text", body = String), (status = 404, description = "absent")))]
pub async fn get(Path(file): Path<String>) -> Response {
    match crate::id3_read(&file, "like") {
        Some(v) => v.into_response(),
        None => StatusCode::NOT_FOUND.into_response(),
    }
}

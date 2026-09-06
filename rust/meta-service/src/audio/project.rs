use axum::extract::Path;
use axum::http::StatusCode;
use axum::response::{IntoResponse, Response};
use axum::Json;
use id3::TagLike;
use serde::Deserialize;
use utoipa::ToSchema;

#[derive(Deserialize, ToSchema)]
pub struct AudioProject {
    /// Media file name, resolved in the served directory
    pub file: String,
    /// Value stored verbatim as the id3 TXXX frame + the proper TALB frame (the
    /// established album ↔ project mapping)
    pub value: String,
}

#[utoipa::path(post, operation_id = "postProject", path = "/audio/project", request_body = AudioProject,
    responses((status = 200, description = "written", body = String)))]
pub async fn post(Json(body): Json<AudioProject>) -> &'static str {
    crate::id3_write(&body.file, "project", &body.value);
    crate::id3_edit(&body.file, |tag| tag.set_album(body.value.as_str()));
    "ok"
}

#[utoipa::path(get, operation_id = "getProject", path = "/audio/project/{file}",
    params(("file" = String, Path, description = "Media file name, resolved in the served directory")),
    responses((status = 200, description = "the file's project tag as plain text", body = String), (status = 404, description = "absent")))]
pub async fn get(Path(file): Path<String>) -> Response {
    match crate::id3_read(&file, "project") {
        Some(v) => v.into_response(),
        None => StatusCode::NOT_FOUND.into_response(),
    }
}

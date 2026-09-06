use axum::extract::Path;
use axum::http::StatusCode;
use axum::response::{IntoResponse, Response};
use axum::Json;
use id3::TagLike;
use serde::Deserialize;
use utoipa::ToSchema;

#[derive(Deserialize, ToSchema)]
pub struct AudioGenre {
    /// Media file name, resolved in the served directory
    pub file: String,
    /// Value stored verbatim as the id3 TXXX frame + the proper TCON frame
    pub value: String,
}

#[utoipa::path(post, operation_id = "postGenre", path = "/audio/genre", request_body = AudioGenre,
    responses((status = 200, description = "written", body = String)))]
pub async fn post(Json(body): Json<AudioGenre>) -> &'static str {
    crate::id3_write(&body.file, "genre", &body.value);
    crate::id3_edit(&body.file, |tag| tag.set_genre(body.value.as_str()));
    "ok"
}

#[utoipa::path(get, operation_id = "getGenre", path = "/audio/genre/{file}",
    params(("file" = String, Path, description = "Media file name, resolved in the served directory")),
    responses((status = 200, description = "the file's genre tag as plain text", body = String), (status = 404, description = "absent")))]
pub async fn get(Path(file): Path<String>) -> Response {
    match crate::id3_read(&file, "genre") {
        Some(v) => v.into_response(),
        None => StatusCode::NOT_FOUND.into_response(),
    }
}

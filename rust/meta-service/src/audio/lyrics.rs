use axum::extract::Path;
use axum::http::StatusCode;
use axum::response::{IntoResponse, Response};
use axum::Json;
use id3::frame::Lyrics;
use id3::TagLike;
use serde::Deserialize;
use utoipa::ToSchema;

#[derive(Deserialize, ToSchema)]
pub struct AudioLyrics {
    /// Media file name, resolved in the served directory
    pub file: String,
    /// Value stored verbatim as the id3 TXXX frame + the proper USLT frame (descriptor "")
    pub value: String,
}

#[utoipa::path(post, operation_id = "postLyrics", path = "/audio/lyrics", request_body = AudioLyrics,
    responses((status = 200, description = "written", body = String)))]
pub async fn post(Json(body): Json<AudioLyrics>) -> &'static str {
    crate::id3_write(&body.file, "lyrics", &body.value);
    crate::id3_edit(&body.file, |tag| {
        tag.add_frame(Lyrics {
            lang: "eng".to_string(),
            description: String::new(),
            text: body.value.clone(),
        });
    });
    "ok"
}

#[utoipa::path(get, operation_id = "getLyrics", path = "/audio/lyrics/{file}",
    params(("file" = String, Path, description = "Media file name, resolved in the served directory")),
    responses((status = 200, description = "the file's lyrics tag as plain text", body = String), (status = 404, description = "absent")))]
pub async fn get(Path(file): Path<String>) -> Response {
    match crate::id3_read(&file, "lyrics") {
        Some(v) => v.into_response(),
        None => StatusCode::NOT_FOUND.into_response(),
    }
}

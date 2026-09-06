use axum::extract::Path;
use axum::http::StatusCode;
use axum::response::{IntoResponse, Response};
use axum::Json;
use id3::frame::{Picture, PictureType};
use id3::TagLike;
use serde::Deserialize;
use utoipa::ToSchema;

#[derive(Deserialize, ToSchema)]
pub struct AudioImage {
    /// Media file name, resolved in the served directory
    pub file: String,
    /// Cover image file name, resolved in the served directory — stored verbatim as the
    /// id3 TXXX frame + the bytes embedded as the proper front-cover APIC picture
    pub value: String,
}

#[utoipa::path(post, operation_id = "postImage", path = "/audio/image", request_body = AudioImage,
    responses((status = 200, description = "written", body = String)))]
pub async fn post(Json(body): Json<AudioImage>) -> &'static str {
    crate::id3_write(&body.file, "image", &body.value);

    let picture_data = std::fs::read(crate::DIR.get().expect("dir not set").join(&body.value))
        .unwrap_or_else(|e| panic!("{} (image for {}): failed to read image file: {e}", body.value, body.file));
    let mime_type = match infer::get(&picture_data) {
        Some(kind) => kind.mime_type().to_string(),
        None => panic!("{} (image for {}): not a recognised image (type inference failed)", body.value, body.file),
    };
    crate::id3_edit(&body.file, |tag| {
        tag.add_frame(Picture {
            mime_type,
            picture_type: PictureType::CoverFront,
            description: String::new(),
            data: picture_data,
        });
    });
    "ok"
}

#[utoipa::path(get, operation_id = "getImage", path = "/audio/image/{file}",
    params(("file" = String, Path, description = "Media file name, resolved in the served directory")),
    responses((status = 200, description = "the file's image tag as plain text", body = String), (status = 404, description = "absent")))]
pub async fn get(Path(file): Path<String>) -> Response {
    match crate::id3_read(&file, "image") {
        Some(v) => v.into_response(),
        None => StatusCode::NOT_FOUND.into_response(),
    }
}

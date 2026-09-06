//! `protagonist` — TXXX custom metadata (the image file name, what the model read
//! returns) + the proper Lead-Artist APIC picture (port of `id3-write-protagonist`):
//! image bytes embedded with description "protagonist", mime inferred from the bytes.

use axum::extract::Path;
use axum::http::StatusCode;
use axum::response::{IntoResponse, Response};
use axum::Json;
use id3::frame::{Picture, PictureType};
use id3::{Tag, TagLike, Version};
use serde::Deserialize;
use utoipa::ToSchema;

#[derive(Deserialize, ToSchema)]
pub struct AudioProtagonist {
    /// Media file name, resolved in the served directory
    pub file: String,
    /// Image file name to embed, resolved in the served directory
    pub value: String,
}

#[utoipa::path(post, operation_id = "postProtagonist", path = "/audio/protagonist", request_body = AudioProtagonist,
    responses((status = 200, description = "written", body = String)))]
pub async fn post(Json(body): Json<AudioProtagonist>) -> &'static str {
    crate::id3_write(&body.file, "protagonist", &body.value);

    let picture_data = std::fs::read(crate::DIR.get().expect("dir not set").join(&body.value))
        .unwrap_or_else(|e| panic!("{} (protagonist for {}): failed to read image file: {e}", body.value, body.file));
    let mime_type = match infer::get(&picture_data) {
        Some(kind) => kind.mime_type().to_string(),
        None => panic!("{} (protagonist for {}): not a recognised image (type inference failed)", body.value, body.file),
    };
    let path = crate::DIR.get().expect("dir not set").join(&body.file);
    let mut tag = match Tag::read_from_path(&path) {
        Ok(tag) => tag,
        Err(id3::Error { kind: id3::ErrorKind::NoTag, .. }) => Tag::new(),
        Err(e) => panic!("{}: failed to read ID3 tag: {e}", body.file),
    };
    tag.add_frame(Picture {
        mime_type,
        picture_type: PictureType::LeadArtist,
        description: "protagonist".to_string(),
        data: picture_data,
    });
    tag.write_to_path(&path, Version::Id3v24)
        .unwrap_or_else(|e| panic!("{}: failed to write ID3 tag: {e}", body.file));
    "ok"
}

#[utoipa::path(get, operation_id = "getProtagonist", path = "/audio/protagonist/{file}",
    params(("file" = String, Path, description = "Media file name, resolved in the served directory")),
    responses((status = 200, description = "the file's protagonist tag as plain text", body = String), (status = 404, description = "absent")))]
pub async fn get(Path(file): Path<String>) -> Response {
    match crate::id3_read(&file, "protagonist") {
        Some(v) => v.into_response(),
        None => StatusCode::NOT_FOUND.into_response(),
    }
}

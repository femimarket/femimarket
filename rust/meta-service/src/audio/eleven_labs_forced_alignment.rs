use axum::extract::Path;
use axum::Json;
use serde::{Deserialize, Serialize};
use utoipa::ToSchema;

/// Mirrors the elevenlabs ForcedAlignmentCharacterResponseModel
#[derive(Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct ForcedAlignmentCharacter {
    pub text: String,
    pub start: f64,
    pub end: f64,
    pub loss: f64,
}

/// Mirrors the elevenlabs ForcedAlignmentWordResponseModel
#[derive(Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct ForcedAlignmentWord {
    pub text: String,
    pub start: f64,
    pub end: f64,
}

/// Mirrors the elevenlabs ForcedAlignmentResponseModel
#[derive(Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct ForcedAlignment {
    pub characters: Vec<ForcedAlignmentCharacter>,
    pub words: Vec<ForcedAlignmentWord>,
    pub loss: f64,
}

#[derive(Deserialize, ToSchema)]
pub struct AudioElevenLabsForcedAlignment {
    /// Media file name, resolved in the served directory
    pub file: String,
    /// Value stored verbatim as the id3 TXXX frame (JSON string for list/object fields)
    pub value: String,
}

#[utoipa::path(post, operation_id = "postElevenLabsForcedAlignment", path = "/audio/elevenLabsForcedAlignment", request_body = AudioElevenLabsForcedAlignment,
    responses((status = 200, description = "written", body = String)))]
pub async fn post(Json(body): Json<AudioElevenLabsForcedAlignment>) -> &'static str {
    crate::id3_write(&body.file, "elevenLabsForcedAlignment", &body.value);
    "ok"
}

#[utoipa::path(get, operation_id = "getElevenLabsForcedAlignment", path = "/audio/elevenLabsForcedAlignment/{file}",
    params(("file" = String, Path, description = "Media file name, resolved in the served directory")),
    responses((status = 200, description = "the file's elevenLabsForcedAlignment tag, null when absent", body = Option<ForcedAlignment>)))]
pub async fn get(Path(file): Path<String>) -> Json<Option<ForcedAlignment>> {
    Json(crate::id3_read(&file, "elevenLabsForcedAlignment")
        .map(|s| serde_path_to_error::deserialize(&mut serde_json::Deserializer::from_str(&s)).unwrap_or_else(|e| panic!("{file}: elevenLabsForcedAlignment: {e}"))))
}

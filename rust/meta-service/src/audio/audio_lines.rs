use axum::extract::Path;
use axum::Json;
use serde::{Deserialize, Serialize};
use utoipa::ToSchema;

/// Mirrors market.femi.models.AudioTheme
#[derive(Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct AudioTheme {
    pub id: Option<i32>,
    pub theme: String,
    pub expand: Option<String>,
    pub scene: Option<String>,
}

/// Mirrors market.femi.models.AudioLine
#[derive(Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct AudioLine {
    pub id: i32,
    pub text: String,
    pub start_ms: f64,
    pub context: Option<String>,
    pub goal: Option<String>,
    pub themes: Vec<AudioTheme>,
    pub expands: Option<Vec<String>>,
    pub scenes: Option<Vec<String>>,
}

#[derive(Deserialize, ToSchema)]
pub struct AudioAudioLines {
    /// Media file name, resolved in the served directory
    pub file: String,
    /// Value stored verbatim as the id3 TXXX frame (JSON string for list/object fields)
    pub value: String,
}

#[utoipa::path(post, operation_id = "postAudioLines", path = "/audio/audioLines", request_body = AudioAudioLines,
    responses((status = 200, description = "written", body = String)))]
pub async fn post(Json(body): Json<AudioAudioLines>) -> &'static str {
    crate::id3_write(&body.file, "audioLines", &body.value);
    "ok"
}

#[utoipa::path(get, operation_id = "getAudioLines", path = "/audio/audioLines/{file}",
    params(("file" = String, Path, description = "Media file name, resolved in the served directory")),
    responses((status = 200, description = "the file's audioLines tag, empty when absent", body = Vec<AudioLine>)))]
pub async fn get(Path(file): Path<String>) -> Json<Vec<AudioLine>> {
    Json(crate::id3_read(&file, "audioLines")
        .map(|s| serde_path_to_error::deserialize(&mut serde_json::Deserializer::from_str(&s)).unwrap_or_else(|e| panic!("{file}: audioLines: {e}")))
        .unwrap_or_default())
}

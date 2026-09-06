use axum::extract::Path;
use axum::Json;
use serde::Deserialize;
use utoipa::ToSchema;

#[derive(Deserialize, ToSchema)]
pub struct AudioLyricTokens {
    /// Media file name, resolved in the served directory
    pub file: String,
    /// Value stored verbatim as the id3 TXXX frame (JSON string for list/object fields)
    pub value: String,
}

#[utoipa::path(post, operation_id = "postLyricTokens", path = "/audio/lyricTokens", request_body = AudioLyricTokens,
    responses((status = 200, description = "written", body = String)))]
pub async fn post(Json(body): Json<AudioLyricTokens>) -> &'static str {
    crate::id3_write(&body.file, "lyricTokens", &body.value);
    "ok"
}

#[utoipa::path(get, operation_id = "getLyricTokens", path = "/audio/lyricTokens/{file}",
    params(("file" = String, Path, description = "Media file name, resolved in the served directory")),
    responses((status = 200, description = "the file's lyricTokens tag, empty when absent", body = Vec<String>)))]
pub async fn get(Path(file): Path<String>) -> Json<Vec<String>> {
    Json(crate::id3_read(&file, "lyricTokens")
        .map(|s| serde_path_to_error::deserialize(&mut serde_json::Deserializer::from_str(&s)).unwrap_or_else(|e| panic!("{file}: lyricTokens: {e}")))
        .unwrap_or_default())
}

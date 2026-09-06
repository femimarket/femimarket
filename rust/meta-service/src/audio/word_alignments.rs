//! `wordAlignments` — TXXX custom metadata (the JSON, what the model read returns)
//! + the proper "edited" SYLT frame (port of `id3-write-sylt`): start timestamp +
//! text, then end timestamp + empty token per word — the convention
//! `read_edited_synced_lyrics` decodes.

use axum::extract::Path;
use axum::Json;
use id3::frame::{SynchronisedLyrics, SynchronisedLyricsType, TimestampFormat};
use id3::{Tag, TagLike, Version};
use serde::{Deserialize, Serialize};
use utoipa::ToSchema;

/// Mirrors market.femi.models.WordAlignment
#[derive(Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct WordAlignment {
    pub text: String,
    pub start: f64,
    pub end: f64,
}

#[derive(Deserialize, ToSchema)]
pub struct AudioWordAlignments {
    /// Media file name, resolved in the served directory
    pub file: String,
    /// WordAlignment JSON, e.g. `[{"text":"hi","start":0.0,"end":0.5}]`
    pub value: String,
}

#[utoipa::path(post, operation_id = "postWordAlignments", path = "/audio/wordAlignments", request_body = AudioWordAlignments,
    responses((status = 200, description = "written", body = String)))]
pub async fn post(Json(body): Json<AudioWordAlignments>) -> &'static str {
    crate::id3_write(&body.file, "wordAlignments", &body.value);

    let words: Vec<WordAlignment> =
        serde_path_to_error::deserialize(&mut serde_json::Deserializer::from_str(&body.value))
            .unwrap_or_else(|e| panic!("{}: wordAlignments: {e}", body.file));
    let path = crate::DIR.get().expect("dir not set").join(&body.file);
    let mut tag = match Tag::read_from_path(&path) {
        Ok(tag) => tag,
        Err(id3::Error { kind: id3::ErrorKind::NoTag, .. }) => Tag::new(),
        Err(e) => panic!("{}: failed to read ID3 tag: {e}", body.file),
    };
    tag.add_frame(SynchronisedLyrics {
        lang: "eng".to_string(),
        timestamp_format: TimestampFormat::Ms,
        content_type: SynchronisedLyricsType::Lyrics,
        description: "edited".to_string(),
        content: words
            .into_iter()
            .flat_map(|w| {
                vec![
                    ((w.start * 1000.0) as u32, w.text),
                    ((w.end * 1000.0) as u32, String::new()),
                ]
            })
            .collect(),
    });
    tag.write_to_path(&path, Version::Id3v24)
        .unwrap_or_else(|e| panic!("{}: failed to write ID3 tag: {e}", body.file));
    "ok"
}

#[utoipa::path(get, operation_id = "getWordAlignments", path = "/audio/wordAlignments/{file}",
    params(("file" = String, Path, description = "Media file name, resolved in the served directory")),
    responses((status = 200, description = "the file's wordAlignments tag, empty when absent", body = Vec<WordAlignment>)))]
pub async fn get(Path(file): Path<String>) -> Json<Vec<WordAlignment>> {
    let Some(s) = crate::id3_read(&file, "wordAlignments") else { return Json(Vec::new()) };
        Json(serde_path_to_error::deserialize(&mut serde_json::Deserializer::from_str(&s)).unwrap_or_else(|e| panic!("{file}: wordAlignments: {e}")))
}

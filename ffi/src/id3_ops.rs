//! id3 read/write, ported from `wasm-id3/src/lib.rs`.
//!
//! Behaviour is identical to the web shim; the ONLY change is the error type
//! (`String` instead of `JsValue`) so the code is binding-agnostic. The web
//! shim now delegates here and maps `String` -> `JsValue`; the JNI/C-ABI shims
//! map `String` -> a thrown Java exception / a C error-out respectively.

use std::io::Cursor;
use id3::{Tag, TagLike, Version};
use id3::frame::{
    ExtendedText, Lyrics, Picture, PictureType, SynchronisedLyrics, SynchronisedLyricsType,
    TimestampFormat,
};

/// The descriptor under which we store USER-EDITED lyrics/alignments, distinct
/// from the original (`""`) frame so the source is never overwritten. Mirrors
/// the `MetadataService` port contract ("edited" descriptor, original intact).
const EDITED_DESCRIPTOR: &str = "edited";
const LANG_ENG: &str = "eng";

/// Mirror of Kotlin's WordAlignment — serializable and deserializable.
#[derive(serde::Serialize, serde::Deserialize)]
struct WordAlignment {
    text: String,
    start: f64,
    end: f64,
}

/// Compile updated tags and re-attach the original trailing MP3 audio stream so
/// the audio payload is never stripped. Identical to the web `save_tag`.
fn save_tag(original_bytes: &[u8], tag: Tag) -> Result<Vec<u8>, String> {
    let mut out = Vec::new();

    // 1. Write the ID3v2.4 tag block.
    tag.write_to(&mut out, Version::Id3v24)
        .map_err(|e| format!("ID3 compile write failure: {}", e))?;

    // 2. Skip the OLD tag structure on the source to find where audio begins.
    let mut reader = Cursor::new(original_bytes);
    let _ = id3::Tag::skip(&mut reader);
    let audio_payload_start = reader.position() as usize;

    // 3. Append the untouched raw audio stream onto the back.
    if audio_payload_start < original_bytes.len() {
        out.extend_from_slice(&original_bytes[audio_payload_start..]);
    }

    Ok(out)
}

// ── ALBUM / PROJECT NAME (TALB) ──────────────────────────────────────────────

pub fn read_album(mp3_bytes: &[u8]) -> Option<String> {
    Tag::read_from2(Cursor::new(mp3_bytes))
        .ok()
        .and_then(|t| t.album().map(String::from))
}

pub fn write_album(mp3_bytes: &[u8], value: &str) -> Result<Vec<u8>, String> {
    let mut tag = Tag::read_from2(Cursor::new(mp3_bytes)).unwrap_or_default();
    tag.set_album(value);
    save_tag(mp3_bytes, tag)
}

// ── UNSYNCHRONISED LYRICS (USLT) ─────────────────────────────────────────────

/// The ORIGINAL lyrics — the first USLT frame (descriptor `""`).
pub fn read_lyrics(mp3_bytes: &[u8]) -> Option<String> {
    Tag::read_from2(Cursor::new(mp3_bytes))
        .ok()
        .and_then(|t| t.lyrics().next().map(|l| l.text.clone()))
}

/// Overwrites ALL lyrics with a single original-descriptor frame. This is the
/// source-lyrics writer used at import time; the ports deliberately do NOT
/// expose it to the user (source is immutable once edited descriptors exist).
pub fn write_lyrics(mp3_bytes: &[u8], value: &str) -> Result<Vec<u8>, String> {
    let mut tag = Tag::read_from2(Cursor::new(mp3_bytes)).unwrap_or_default();
    tag.remove_all_lyrics();
    tag.add_frame(Lyrics {
        lang: LANG_ENG.to_string(),
        description: String::new(),
        text: value.to_string(),
    });
    save_tag(mp3_bytes, tag)
}

/// The EDITED lyrics — the USLT frame under the "edited" descriptor. Reading
/// this is how the journey proves `commitLyrics` wrote INTO the file.
pub fn read_edited_lyrics(mp3_bytes: &[u8]) -> Option<String> {
    Tag::read_from2(Cursor::new(mp3_bytes))
        .ok()
        .and_then(|t| {
            t.lyrics()
                .find(|l| l.description == EDITED_DESCRIPTOR)
                .map(|l| l.text.clone())
        })
}

/// `MetadataService.writeEditedLyrics` — adds/replaces ONLY the "edited" USLT
/// frame, leaving the original (`""`) frame and everything else byte-intact.
/// `add_frame` replaces a frame with the same unique key (lang + descriptor),
/// so the original descriptor `""` is untouched.
pub fn write_edited_lyrics(mp3_bytes: &[u8], text: &str) -> Result<Vec<u8>, String> {
    let mut tag = Tag::read_from2(Cursor::new(mp3_bytes)).unwrap_or_default();
    tag.add_frame(Lyrics {
        lang: LANG_ENG.to_string(),
        description: EDITED_DESCRIPTOR.to_string(),
        text: text.to_string(),
    });
    save_tag(mp3_bytes, tag)
}

// ── SYNCHRONISED LYRICS (SYLT) ───────────────────────────────────────────────

/// The EDITED alignment — the SYLT frame under the "edited" descriptor.
/// Parses SYLT `(timestamp_ms, token)` pairs, applies word-boundary detection
/// (empty token = word end, `\n` splits words), and returns `List<WordAlignment>`
/// as JSON. `None` when no aligned frame exists yet.
pub fn read_edited_synced_lyrics(mp3_bytes: &[u8]) -> Option<String> {
    let pairs = Tag::read_from2(Cursor::new(mp3_bytes))
        .ok()
        .and_then(|t| {
            t.synchronised_lyrics()
                .find(|s| s.description == EDITED_DESCRIPTOR)
                .map(|s| s.content.clone())
        })?;

    let mut result: Vec<WordAlignment> = Vec::new();

    let mut cur_text: Option<String> = None;
    let mut cur_start: Option<f64> = None;

    for (ms_u32, token) in pairs {
        let sec = ms_u32 as f64 / 1000.0;

        if token.is_empty() {
            // Empty token provides the end time and completes the word
            if let (Some(text), Some(start)) = (cur_text.take(), cur_start.take()) {
                result.push(WordAlignment {
                    text,
                    start,
                    end: sec,
                });
            }
        } else {
            // Non-empty token sets the text and start time for the next word
            cur_text = Some(token);
            cur_start = Some(sec);
        }
    }

    serde_json::to_string(&result).ok()
}
/// `MetadataService.writeEditedSyncedLyrics` — adds/replaces ONLY the "edited"
/// SYLT frame. `content` is a JSON string of `WordAlignment` list: `[{"text":"...","start":0.123,"end":0.456}, ...]`.
pub fn write_edited_synced_lyrics(
    mp3_bytes: &[u8],
    content: &str,
) -> Result<Vec<u8>, String> {
    let words: Vec<WordAlignment> = serde_json::from_str(content)
        .map_err(|e| format!("failed to parse synced lyrics JSON: {e}"))?;
    let mut tag = Tag::read_from2(Cursor::new(mp3_bytes)).unwrap_or_default();
    tag.add_frame(SynchronisedLyrics {
        lang: LANG_ENG.to_string(),
        timestamp_format: TimestampFormat::Ms,
        content_type: SynchronisedLyricsType::Lyrics,
        description: EDITED_DESCRIPTOR.to_string(),
        content: words
            .into_iter()
            .flat_map(|w| {
                vec![
                    ((w.start * 1000.0) as u32, w.text.clone()),
                    ((w.end * 1000.0) as u32, String::new()),
                ]
            })
            .collect(),
    });
    save_tag(mp3_bytes, tag)
}

// ── SUNO CLIP ID (TXXX: suno_clip_id) ────────────────────────────────────────

pub fn read_suno_clip_id(mp3_bytes: &[u8]) -> Option<String> {
    Tag::read_from2(Cursor::new(mp3_bytes)).ok().and_then(|t| {
        t.extended_texts()
            .find(|tx| tx.description == "suno_clip_id")
            .map(|tx| tx.value.clone())
    })
}

pub fn write_suno_clip_id(mp3_bytes: &[u8], value: &str) -> Result<Vec<u8>, String> {
    let mut tag = Tag::read_from2(Cursor::new(mp3_bytes)).unwrap_or_default();
    tag.remove_extended_text(Some("suno_clip_id"), None);
    tag.add_frame(ExtendedText {
        description: "suno_clip_id".to_string(),
        value: value.to_string(),
    });
    save_tag(mp3_bytes, tag)
}

// ── GENRE (TCON) ─────────────────────────────────────────────────────────────

pub fn read_genre(mp3_bytes: &[u8]) -> Option<String> {
eprintln!("[FFI DEBUG] input");
    Tag::read_from2(Cursor::new(mp3_bytes))
        .ok()
        .and_then(|t| t.genre().map(String::from))
}

pub fn write_genre(mp3_bytes: &[u8], value: &str) -> Result<Vec<u8>, String> {
    let mut tag = Tag::read_from2(Cursor::new(mp3_bytes)).unwrap_or_default();
    tag.set_genre(value);
    save_tag(mp3_bytes, tag)
}

// ── COVER ART (APIC: CoverFront) ─────────────────────────────────────────────

pub fn read_cover_bytes(mp3_bytes: &[u8]) -> Option<Vec<u8>> {
    Tag::read_from2(Cursor::new(mp3_bytes)).ok().and_then(|t| {
        t.pictures()
            .find(|p| p.picture_type == PictureType::CoverFront)
            .map(|p| p.data.clone())
    })
}

// ── LEAD ARTIST PROTAGONIST PICTURE (APIC: LeadArtist "protagonist") ─────────

pub fn read_picture_lead_artist_protagonist(mp3_bytes: &[u8]) -> Option<Vec<u8>> {
    Tag::read_from2(Cursor::new(mp3_bytes)).ok().and_then(|t| {
        t.pictures()
            .find(|p| {
                p.picture_type == PictureType::LeadArtist && p.description == "protagonist"
            })
            .map(|p| p.data.clone())
    })
}

pub fn write_picture_lead_artist_protagonist(
    mp3_bytes: &[u8],
    picture_data: &[u8],
) -> Result<Vec<u8>, String> {
    let mut tag = Tag::read_from2(Cursor::new(mp3_bytes)).unwrap_or_default();

    // Explode instantly if inference fails (matches the web behaviour).
    let mime_type = infer::get(picture_data)
        .map(|t| t.mime_type().to_string())
        .ok_or_else(|| "Image type inference failed".to_string())?;

    tag.add_frame(Picture {
        mime_type,
        picture_type: PictureType::LeadArtist,
        description: "protagonist".to_string(),
        data: picture_data.to_vec(),
    });

    save_tag(mp3_bytes, tag)
}

#[cfg(test)]
mod tests {
    use super::*;

    // A round-trip that needs NO real mp3: writing to empty bytes creates a
    // fresh tag; reading it back proves the edited descriptor survives to file.
    #[test]
    fn edited_lyrics_round_trip() {
        let written = write_edited_lyrics(b"", "hello\nworld").unwrap();
        // The "edited"-descriptor frame is present and readable back from file.
        assert_eq!(read_edited_lyrics(&written).as_deref(), Some("hello\nworld"));
        // (read_lyrics returns the FIRST USLT frame regardless of descriptor;
        //  with only the edited frame present it returns that text — see the
        //  original_and_edited_coexist test for the descriptor-isolation proof.)
    }

    #[test]
    fn edited_synced_round_trip() {
        // Timestamps are stored as u32 seconds (rounded from f64).
        let content = vec![
            WordAlignment { text: "hello".into(), start: 0.0, end: 1.0 },
            WordAlignment { text: "world".into(), start: 1.0, end: 2.0 },
        ];
        let written = write_edited_synced_lyrics(b"", &serde_json::to_string(&content).unwrap()).unwrap();
        let read_back: Vec<WordAlignment> = serde_json::from_str(&read_edited_synced_lyrics(&written).unwrap()).unwrap();
        assert_eq!(read_back.len(), 2);
        assert_eq!(read_back[0].text, "hello");
        assert_eq!(read_back[0].start, 0.0);
        assert_eq!(read_back[1].text, "world");
        assert_eq!(read_back[1].start, 1.0);
    }

    #[test]
    fn original_and_edited_coexist() {
        let with_original = write_lyrics(b"", "the source").unwrap();
        let with_both = write_edited_lyrics(&with_original, "the edit").unwrap();
        assert_eq!(read_lyrics(&with_both).as_deref(), Some("the source"));
        assert_eq!(read_edited_lyrics(&with_both).as_deref(), Some("the edit"));
    }
}

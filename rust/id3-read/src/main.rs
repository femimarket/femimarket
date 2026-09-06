//! `id3-read` — read Album/Genre/Lyrics/edited-Lyrics/edited-SYLT/Suno-clip-id from an MP3
//! and print them as one JSON object.
//!
//! Native replacement for the webhook's `exiftool -j -Album -Genre -UnsynchronizedLyrics ...`
//! read_id3 hook — exiftool has two real problems here, not just a wrong flag name: (1) this
//! exiftool install can't even WRITE to MP3 files at all ("Writing of MP3 files is not yet
//! supported"), and (2) exiftool's tag-suffix convention (`-Lyrics-eng`) disambiguates USLT
//! frames by LANGUAGE, but this app disambiguates the "edited" USLT/SYLT frames by
//! DESCRIPTOR (both stay language "eng") — something exiftool's CLI tag names can't select
//! at all. Mirrors `ffi::id3_ops::{read_album, read_genre, read_lyrics, read_edited_lyrics,
//! read_edited_synced_lyrics, read_suno_clip_id}` exactly, using the `id3` crate directly.

use std::path::PathBuf;

use clap::Parser;
use id3::{Tag, TagLike};
use serde::Serialize;

const EDITED_DESCRIPTOR: &str = "edited";

#[derive(Serialize)]
struct WordAlignment {
    text: String,
    start: f64,
    end: f64,
}

#[derive(Serialize)]
struct Id3Data {
    album: Option<String>,
    genre: Option<String>,
    lyrics: Option<String>,
    #[serde(rename = "editedLyrics")]
    edited_lyrics: Option<String>,
    #[serde(rename = "editedSyncedLyrics")]
    edited_synced_lyrics: Option<Vec<WordAlignment>>,
    #[serde(rename = "sunoId")]
    suno_id: Option<String>,
}

#[derive(Parser)]
#[command(name = "id3-read")]
#[command(about = "Read id3 tags from an MP3 and print them as JSON", long_about = None)]
struct Cli {
    /// MP3 to read (resolved against the current directory).
    audio: PathBuf,
}

fn main() {
    let cli = Cli::parse();

    let tag = match Tag::read_from_path(&cli.audio) {
        Ok(tag) => tag,
        Err(id3::Error { kind: id3::ErrorKind::NoTag, .. }) => Tag::new(),
        Err(e) => panic!("failed to read ID3 tag: {e}"),
    };

    let lyrics = tag.lyrics().next().map(|l| l.text.clone());
    let edited_lyrics = tag
        .lyrics()
        .find(|l| l.description == EDITED_DESCRIPTOR)
        .map(|l| l.text.clone());

    // Same empty-token word-boundary convention write_sylt writes and
    // read_edited_synced_lyrics (ffi/src/id3_ops.rs) decodes: (start, text) then (end, "").
    let edited_synced_lyrics = tag
        .synchronised_lyrics()
        .find(|s| s.description == EDITED_DESCRIPTOR)
        .map(|s| {
            let mut result = Vec::new();
            let mut cur_text: Option<String> = None;
            let mut cur_start: Option<f64> = None;
            for (ms, token) in &s.content {
                let sec = *ms as f64 / 1000.0;
                if token.is_empty() {
                    if let (Some(text), Some(start)) = (cur_text.take(), cur_start.take()) {
                        result.push(WordAlignment { text, start, end: sec });
                    }
                } else {
                    cur_text = Some(token.clone());
                    cur_start = Some(sec);
                }
            }
            result
        });

    let suno_id = tag
        .extended_texts()
        .find(|tx| tx.description == "suno_clip_id")
        .map(|tx| tx.value.clone());

    let data = Id3Data {
        album: tag.album().map(String::from),
        genre: tag.genre().map(String::from),
        lyrics,
        edited_lyrics,
        edited_synced_lyrics,
        suno_id,
    };

    println!(
        "{}",
        serde_json::to_string(&data).expect("failed to serialize id3 data")
    );
}

//! `id3-write-sylt` — upsert the "edited" SYLT (synchronised lyrics) frame into
//! an MP3 from JSON word-alignment content, in place.
//!
//! Native replacement for the webhook's `kid3-cli -c "import <lrc_path>" <file_path>`
//! — kid3's `import` only understands LRC-format text; it has no JSON parser, so it
//! can never consume the app's `WordAlignment` JSON contract. This mirrors
//! `ffi::id3_ops::write_edited_synced_lyrics(mp3_bytes, content: &str)` exactly:
//! `content` is the JSON directly (not a file to go read), parsed into the SYLT
//! frame under the "edited" descriptor (start timestamp + text, then end timestamp
//! + empty token marking the word boundary — the same convention
//! `read_edited_synced_lyrics` decodes), then upserted into the file.

use std::path::PathBuf;
use std::str::FromStr;

use clap::Parser;
use id3::frame::{SynchronisedLyrics, SynchronisedLyricsType, TimestampFormat};
use id3::{Tag, TagLike, Version};
use serde::Deserialize;

#[derive(Deserialize, Clone)]
struct WordAlignment {
    text: String,
    start: f64,
    end: f64,
}

// A newtype so clap can parse the CLI argument directly into it (`impl FromStr for
// Vec<WordAlignment>` isn't legal — orphan rules: neither `Vec` nor `FromStr` is local).
// `Clone` is required by clap's derive value-parsing machinery.
#[derive(Clone)]
struct WordAlignments(Vec<WordAlignment>);

impl FromStr for WordAlignments {
    type Err = String;
    fn from_str(s: &str) -> Result<Self, Self::Err> {
        serde_json::from_str(s).map(WordAlignments).map_err(|e| e.to_string())
    }
}

#[derive(Parser)]
#[command(name = "id3-write-sylt")]
#[command(about = "Upsert the \"edited\" SYLT frame into an MP3 from JSON word-alignment content", long_about = None)]
struct Cli {
    /// JSON word-alignment content, e.g. `[{"text":"hi","start":0.0,"end":0.5}]`.
    content: WordAlignments,
    /// MP3 to write the SYLT frame into, in place (resolved against the current directory).
    audio: PathBuf,
}

fn main() {
    let cli = Cli::parse();
    let words = cli.content.0;

    let mut tag = match Tag::read_from_path(&cli.audio) {
        Ok(tag) => tag,
        Err(id3::Error { kind: id3::ErrorKind::NoTag, .. }) => Tag::new(),
        Err(e) => panic!("failed to read ID3 tag: {e}"),
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

    tag.write_to_path(&cli.audio, Version::Id3v24)
        .expect("failed to write ID3 tag");
}

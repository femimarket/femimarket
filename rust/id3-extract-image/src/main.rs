//! `id3-extract-image` — extract the front-cover (APIC CoverFront) picture from
//! an MP3 into the current directory, named with the picture's REAL extension
//! (sniffed from its bytes via `infer`), not an assumed one.
//!
//! Native replacement for the webhook's
//! `kid3-cli -c "get picture:'<name>_cover.jpg'" <audio>` — that hook hardcodes
//! `.jpg` regardless of what the picture actually is (kid3 never transcodes; it
//! writes the frame's raw bytes verbatim), so a PNG cover ends up in a file named
//! `..._cover.jpg`. Mirrors `ffi::id3_ops::read_cover_bytes` (PictureType::CoverFront,
//! no description filter) and the naming State.kt already derives for the cover
//! image (`filename.replace(".mp3", ".$ext")` — same stem, real extension).
//!
//! The printed filename on success IS the webhook response body (this fork
//! returns command stdout verbatim) — the caller reads it directly instead of
//! re-deriving the extension itself.

use std::path::PathBuf;

use clap::Parser;
use id3::frame::PictureType;
use id3::Tag;

#[derive(Parser)]
#[command(name = "id3-extract-image")]
#[command(about = "Extract the front-cover picture from an MP3 into the current directory, named with its real extension", long_about = None)]
struct Cli {
    /// MP3 to read the cover picture from (resolved against the current directory).
    audio: PathBuf,
}

fn main() {
    let cli = Cli::parse();

    let tag = Tag::read_from_path(&cli.audio).expect("failed to read ID3 tag");
    let picture = tag
        .pictures()
        .find(|p| p.picture_type == PictureType::CoverFront)
        .expect("no front-cover picture in this file");
    let ext = infer::get(&picture.data)
        .map(|kind| kind.extension())
        .expect("cover picture type inference failed");

    // Same base name as the audio file, current directory, real extension.
    let stem = cli.audio.file_stem().expect("no file name").to_string_lossy();
    let filename = format!("{stem}.{ext}");

    std::fs::write(&filename, &picture.data).expect("failed to write cover image");

    println!("{filename}");
}

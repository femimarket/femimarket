//! `id3-extract-protagonist` — extract the Lead-Artist/"protagonist" picture
//! (APIC LeadArtist, description "protagonist") from an MP3 into the current
//! directory, named with the picture's REAL extension (sniffed from its bytes
//! via `infer`), not an assumed one.
//!
//! Native replacement for the webhook's
//! `kid3-cli -c "get picture:'<name>_protagonist.jpg' 'Lead Artist' 'protagonist'" <audio>`
//! — same hardcoded-`.jpg` problem as `id3-extract-image`. Mirrors
//! `ffi::id3_ops::read_picture_lead_artist_protagonist` and the naming State.kt
//! already derives for the protagonist image (`filename.replace(".mp3", "-protagonist.$ext")`).
//!
//! The printed filename on success IS the webhook response body (this fork
//! returns command stdout verbatim) — the caller reads it directly instead of
//! re-deriving the extension itself.

use std::path::PathBuf;

use clap::Parser;
use id3::frame::PictureType;
use id3::Tag;

#[derive(Parser)]
#[command(name = "id3-extract-protagonist")]
#[command(about = "Extract the Lead-Artist/\"protagonist\" picture from an MP3 into the current directory, named with its real extension", long_about = None)]
struct Cli {
    /// MP3 to read the protagonist picture from (resolved against the current directory).
    audio: PathBuf,
}

fn main() {
    let cli = Cli::parse();

    let tag = Tag::read_from_path(&cli.audio).expect("failed to read ID3 tag");
    let picture = tag
        .pictures()
        .find(|p| p.picture_type == PictureType::LeadArtist && p.description == "protagonist")
        .expect("no Lead-Artist/\"protagonist\" picture in this file");
    let ext = infer::get(&picture.data)
        .map(|kind| kind.extension())
        .expect("protagonist picture type inference failed");

    // "<audio-stem>-protagonist.<ext>", current directory, real extension.
    let stem = cli.audio.file_stem().expect("no file name").to_string_lossy();
    let filename = format!("{stem}-protagonist.{ext}");

    std::fs::write(&filename, &picture.data).expect("failed to write protagonist image");

    println!("{filename}");
}

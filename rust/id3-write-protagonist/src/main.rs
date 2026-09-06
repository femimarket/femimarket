//! `id3-write-protagonist` — upsert the Lead-Artist / "protagonist" APIC picture
//! into an MP3, in place.
//!
//! Native replacement for the webhook's
//! `kid3-cli -c "set picture:'<image>' 'Lead Artist' 'protagonist'" <audio>` call.
//! Behaviour mirrors `ffi::id3_ops::write_picture_lead_artist_protagonist`
//! (frame: APIC, `PictureType::LeadArtist`, description `"protagonist"`, mime
//! inferred from the image bytes) — the only difference is that this operates on
//! files in the current directory instead of in-memory byte buffers.

use std::path::PathBuf;

use clap::Parser;
use id3::frame::{Picture, PictureType};
use id3::{Tag, TagLike, Version};

#[derive(Parser)]
#[command(name = "id3-write-protagonist")]
#[command(about = "Upsert the Lead-Artist/\"protagonist\" picture into an MP3", long_about = None)]
struct Cli {
    /// Image file to embed (resolved against the current directory).
    image: PathBuf,
    /// MP3 to write the picture into, in place (resolved against the current directory).
    audio: PathBuf,
}

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let cli = Cli::parse();

    // 1. Image bytes from the current dir.
    let picture_data = std::fs::read(&cli.image)?;

    // 2. Infer the mime from the bytes — refuse anything that isn't an image
    //    (mirrors the FFI/web "Image type inference failed" guard).
    let mime_type = match infer::get(&picture_data) {
        Some(kind) => kind.mime_type().to_string(),
        None => {
            eprintln!(
                "error: '{}' is not a recognised image (type inference failed)",
                cli.image.display()
            );
            std::process::exit(1);
        }
    };

    // 3. Read the file's existing tag so every OTHER frame — cover art, lyrics,
    //    album — survives. A file with no tag starts from an empty one; any other
    //    read error is fatal on purpose: we must NOT silently drop a partial tag
    //    and then overwrite the file with those frames missing. (The in-memory FFI
    //    can afford `unwrap_or_default()` because it returns fresh bytes rather
    //    than overwriting the source.)
    let mut tag = match Tag::read_from_path(&cli.audio) {
        Ok(tag) => tag,
        Err(id3::Error { kind: id3::ErrorKind::NoTag, .. }) => Tag::new(),
        Err(err) => return Err(err.into()),
    };

    // 4. Upsert: `add_frame` replaces the APIC frame carrying the same content
    //    descriptor ("protagonist"), so re-running just swaps the image while the
    //    cover-front and any other-description pictures are left intact.
    tag.add_frame(Picture {
        mime_type,
        picture_type: PictureType::LeadArtist,
        description: "protagonist".to_string(),
        data: picture_data,
    });

    // 5. Write the ID3v2.4 tag back into the file in place; the trailing audio
    //    stream is preserved by the id3 crate.
    tag.write_to_path(&cli.audio, Version::Id3v24)?;

    Ok(())
}

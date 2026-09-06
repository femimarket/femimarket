pub mod audio;

use std::path::PathBuf;
use std::sync::OnceLock;

use id3::frame::ExtendedText;
use id3::{Tag, TagLike, Version};
use xmp_toolkit::{OpenFileOptions, XmpFile, XmpMeta, XmpValue};

/// The served media directory (the dufs dir) — set once at startup from the CLI.
pub static DIR: OnceLock<PathBuf> = OnceLock::new();

/// One id3 tag session: open the file's tag (or start one), apply the edit, write it back.
pub fn id3_edit(file: &str, edit: impl FnOnce(&mut Tag)) {
    let path = DIR.get().expect("dir not set").join(file);
    let mut tag = match Tag::read_from_path(&path) {
        Ok(tag) => tag,
        Err(id3::Error { kind: id3::ErrorKind::NoTag, .. }) => Tag::new(),
        Err(e) => panic!("{file}: failed to read ID3 tag: {e}"),
    };
    edit(&mut tag);
    tag.write_to_path(&path, Version::Id3v24)
        .unwrap_or_else(|e| panic!("{file}: failed to write ID3 tag: {e}"));
}

/// The custom-metadata write every audio field endpoint goes through — one TXXX frame
/// per field, description = the field name. This is what the model read returns; fields
/// with an interoperable frame (TCON, USLT, TALB, APIC, SYLT) also write that proper
/// place for other apps.
pub fn id3_write(file: &str, tag_name: &str, value: &str) {
    id3_edit(file, |tag| {
        tag.add_frame(ExtendedText {
            description: tag_name.to_string(),
            value: value.to_string(),
        });
    });
}

/// The file's id3 tag — None when the file has none. The field GETs use this to read
/// the proper interoperable frames (TCON, TALB, USLT, SYLT, APIC) the POSTs write.
pub fn id3_tag(file: &str) -> Option<Tag> {
    match Tag::read_from_path(DIR.get().expect("dir not set").join(file)) {
        Ok(tag) => Some(tag),
        Err(id3::Error { kind: id3::ErrorKind::NoTag, .. }) => None,
        Err(e) => panic!("{file}: failed to read ID3 tag: {e}"),
    }
}

/// The custom-metadata read every audio field endpoint goes through — the TXXX frame's
/// value, None when the frame (or the whole tag) is absent.
pub fn id3_read(file: &str, tag_name: &str) -> Option<String> {
    id3_tag(file)?.extended_texts().find(|x| x.description == tag_name).map(|x| x.value.clone())
}

/// The XMP write — reserved for the non-audio models (video/image); audio is id3.
pub fn xmp_write(file: &str, namespace: &str, prefix: &str, tag: &str, value: &str) {
    XmpMeta::register_namespace(namespace, prefix).expect("failed to register namespace");
    let mut xmp_file = XmpFile::new().expect("failed to init XmpFile");
    xmp_file
        .open_file(DIR.get().expect("dir not set").join(file), OpenFileOptions::default().for_update())
        .unwrap_or_else(|e| panic!("{file}: failed to open file for update: {e}"));
    let mut meta = match xmp_file.xmp() {
        Some(existing) => existing,
        None => XmpMeta::new().expect("failed to create XmpMeta"),
    };
    meta.set_property(namespace, tag, &XmpValue::new(value.to_string()))
        .unwrap_or_else(|e| panic!("{file}: failed to set property {tag}: {e}"));
    xmp_file.put_xmp(&meta).unwrap_or_else(|e| panic!("{file}: failed to put xmp: {e}"));
    xmp_file.close();
}

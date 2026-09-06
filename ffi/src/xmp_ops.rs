//! xmp — XMP metadata reads over `xmpkit`, the native port of `wasm-xmp/src/lib.rs`.
//!
//! The web shim (`wasm-xmp`) is a single line — `pub use xmpkit::wasm::*;` — so the
//! behaviour the webMain `XmpItem.fromXmp` factory (XmpItemXmp.kt) actually relies on
//! is xmpkit's own `XmpFile::from_bytes` → `get_xmp` → `XmpMeta::get_property`
//! pipeline, plus the wasm binding's value→string projection
//! (`xmpkit::wasm::meta::XmpMeta::get_property`). This module reproduces EXACTLY that
//! pipeline over xmpkit's std API — the SAME crate and the SAME core parse the wasm
//! build runs — so the native `readXmp` actuals project the same field strings the
//! web path produces.
//!
//! Field set (the webMain `XmpItemXmp.kt` reference reads exactly these five):
//!   - `xmpMM:OriginalDocumentID` — required by the Kotlin caller; absence throws there
//!   - `xmpDM:genre` / `xmpDM:lyrics` / `xmpDM:projectName` — optional
//!   - `xmp:Rating` — optional, parsed to an Int (fallback 0) on the Kotlin side
//!
//! Each read is an independent full parse of the file bytes — the same
//! "one projection per call" shape as id3_ops, which keeps every FFI function a
//! trivial one-value marshaller (parsing is cheap relative to the import IO).

use xmpkit::{XmpFile, XmpValue};

/// Adobe XMP Dynamic Media namespace (`xmpDM`) — carries genre / lyrics / projectName.
pub const XMP_DYNAMIC_MEDIA_NAMESPACE: &str = "http://ns.adobe.com/xmp/1.0/DynamicMedia/";

/// Adobe XMP Basic namespace (`xmp`) — carries Rating.
pub const XMP_BASIC_NAMESPACE: &str = "http://ns.adobe.com/xap/1.0/";

/// Adobe XMP Media Management namespace (`xmpMM`) — carries OriginalDocumentID.
pub const XMP_MEDIA_MANAGEMENT_NAMESPACE: &str = "http://ns.adobe.com/xap/1.0/mm/";

/// Project an [`XmpValue`] to the string the web build hands JavaScript.
///
/// VERBATIM the mapping inside `xmpkit::wasm::meta::XmpMeta::get_property`
/// (String passes through; Integer/Boolean stringify; DateTime is already an
/// ISO-8601 string; complex types fall back to their Debug rendering) — so a
/// property read natively and the same property read on web yield the SAME text.
fn xmp_value_to_string(value: XmpValue) -> String {
    match value {
        XmpValue::String(text) => text,
        XmpValue::Integer(number) => number.to_string(),
        XmpValue::Boolean(flag) => flag.to_string(),
        XmpValue::DateTime(datetime) => datetime,
        // Fallback for complex types (arrays/structures) — same Debug formatting
        // the wasm binding falls back to.
        other => format!("{:?}", other),
    }
}

/// Parse `bytes` and read ONE property out of the file's XMP packet.
///
/// `None` covers all three "nothing there" cases exactly like the web path's
/// nulls/throws collapse at the Kotlin call site: the bytes are not a container
/// xmpkit can read, the container carries no XMP packet, or the packet lacks the
/// property.
fn read_xmp_property(bytes: &[u8], namespace: &str, property: &str) -> Option<String> {
    let mut xmp_file = XmpFile::new();
    xmp_file.from_bytes(bytes).ok()?;
    let meta = xmp_file.get_xmp()?;
    meta.get_property(namespace, property).map(xmp_value_to_string)
}

/// `xmpMM:OriginalDocumentID` — the one REQUIRED field: the Kotlin adapters throw
/// when this is `None`, mirroring the webMain factory's IllegalArgumentException.
pub fn read_xmp_original_document_id(bytes: &[u8]) -> Option<String> {
    read_xmp_property(bytes, XMP_MEDIA_MANAGEMENT_NAMESPACE, "OriginalDocumentID")
}

/// `xmpDM:genre` → `XmpItem.dmGenre` (absent → the model's null default).
pub fn read_xmp_genre(bytes: &[u8]) -> Option<String> {
    read_xmp_property(bytes, XMP_DYNAMIC_MEDIA_NAMESPACE, "genre")
}

/// `xmpDM:lyrics` → `XmpItem.dmLyrics` (absent → the model's null default).
pub fn read_xmp_lyrics(bytes: &[u8]) -> Option<String> {
    read_xmp_property(bytes, XMP_DYNAMIC_MEDIA_NAMESPACE, "lyrics")
}

/// `xmp:Rating` → `XmpItem.rating` (the Kotlin side applies `toIntOrNull() ?: 0`,
/// exactly like the web factory).
pub fn read_xmp_rating(bytes: &[u8]) -> Option<String> {
    read_xmp_property(bytes, XMP_BASIC_NAMESPACE, "Rating")
}

/// `xmpDM:projectName` → `XmpItem.projectName` (absent → the model's "" default).
pub fn read_xmp_project_name(bytes: &[u8]) -> Option<String> {
    read_xmp_property(bytes, XMP_DYNAMIC_MEDIA_NAMESPACE, "projectName")
}

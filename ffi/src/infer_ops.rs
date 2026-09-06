//! infer — magic-number file-type detection, ported from `wasm-infer/src/lib.rs`.
//!
//! Returns an owned `String` (rather than the web shim's `&'static str`) so the
//! JNI/C-ABI shims can hand it across the FFI boundary without lifetime games.

/// Reads the magic-number bytes and returns the inferred extension
/// (e.g. "mp3", "png", "mp4"), or `None` if the type is unrecognised.
pub fn read_extension(bytes: &[u8]) -> Option<String> {
    infer::get(bytes).map(|kind| kind.extension().to_string())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn detects_png_magic() {
        // PNG signature: 89 50 4E 47 0D 0A 1A 0A
        let png = [0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0];
        assert_eq!(read_extension(&png).as_deref(), Some("png"));
    }

    #[test]
    fn unknown_is_none() {
        assert_eq!(read_extension(&[0, 1, 2, 3]), None);
    }
}

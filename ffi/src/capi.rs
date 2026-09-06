//! iOS C-ABI shim over `metadata-core` (folded in from the former `metadata-ffi`
//! crate, then ported to the CORRECTED memory model — the one that matches
//! Api2's actual C-ABI and needs NO bespoke free functions).
//!
//! Two output shapes, each with an ownership rule the Kotlin/Native caller
//! relies on:
//!
//!  * STRING readers (`id3_read_lyrics` / `id3_read_edited_lyrics` / `id3_read_album`
//!    / `id3_read_genre` / `inf_read_extension`): CALLER-ALLOCATED out-buffer. The
//!    caller passes `(out, out_cap)`; we write the UTF-8 bytes + a NUL when they
//!    fit and ALWAYS return the number of bytes needed (excluding the NUL). No
//!    heap crosses the boundary, so there is nothing to free. Because a metadata
//!    string is always <= the source file length, the caller sizes `out` to the
//!    input length and calls exactly ONCE (no null-probe, no double parse).
//!
//!  * BYTE outputs (`id3_read_cover`, `id3_write_edited_lyrics`,
//!    `id3_write_protagonist`, `id3_write_edited_synced_lyrics`): OWNED-POINTER
//!    shape. We hand out a raw
//!    `*mut u8` produced by `Box::into_raw(bytes.into_boxed_slice())` and write
//!    the length to `*out_len`; `null` (with `*out_len = 0`) means "no value" /
//!    error. The caller copies the bytes out and then reclaims the buffer with
//!    `platform.posix.free(ptr)` — valid because Rust's default global allocator
//!    IS the system allocator (the same guarantee Api2 leans on with Swift's
//!    `Data(deallocator: .free)`). There is deliberately NO `md_free_*` fn.
//!
//! CONSOLIDATION NOTE: as with `jni.rs`, the bodies call the portable functions
//! via fully-qualified paths (`crate::id3_ops::read_lyrics(..)`); no glob
//! re-exports are used.

use std::os::raw::{c_char, c_int};

// The stateful audio engine, reached through `crate::audio_ops::AudioEngine`.
// The `aud_*` functions folded in at the bottom of this file box an `AudioEngine`
// and pass its raw pointer back and forth as an `int64_t`/`uint64_t` handle — the
// SAME `metadata_ffi.h` cinterop header now declares ALL surfaces.
use crate::audio_ops::AudioEngine;

// ── unsafe boundary helpers (the whole surface's `unsafe` lives here) ─────────

/// Borrow an input `(ptr, len)` as a Rust slice; empty when `ptr` is null.
unsafe fn slice_from<'a>(ptr: *const u8, len: usize) -> &'a [u8] {
    if ptr.is_null() || len == 0 {
        &[]
    } else {
        unsafe { std::slice::from_raw_parts(ptr, len) }
    }
}

/// Borrow an input C string as a Rust `String` (lossy); empty when null/invalid.
unsafe fn str_from(ptr: *const c_char) -> String {
    if ptr.is_null() {
        return String::new();
    }
    unsafe { std::ffi::CStr::from_ptr(ptr) }
        .to_string_lossy()
        .into_owned()
}

/// Copy `s` (UTF-8) into the caller's out-buffer with a trailing NUL when it
/// fits, and ALWAYS return the number of bytes needed (excluding the NUL). This
/// is Api2's `write_str` verbatim: it writes only when `buf_len > needed` (i.e.
/// there is room for the string AND its terminator), so a short buffer leaves
/// the caller's memory untouched and the returned length tells it how much to
/// allocate. Metadata strings are <= the input file, so the caller sizes to the
/// input length and this always fits on the first call.
unsafe fn write_str(buf: *mut c_char, buf_len: c_int, s: &str) -> c_int {
    let bytes = s.as_bytes();
    let needed = bytes.len();
    if !buf.is_null() && buf_len > 0 && (buf_len as usize) > needed {
        // Edition 2024: an `unsafe fn` body is NOT implicitly an unsafe block, so
        // the raw-pointer writes must be wrapped explicitly. The caller-supplied
        // `buf` is trusted to point at `buf_len` writable bytes; we only touch
        // `needed + 1` of them, which the `buf_len > needed` guard above proves
        // are in-bounds (room for the string AND its NUL terminator).
        unsafe {
            std::ptr::copy_nonoverlapping(bytes.as_ptr() as *const c_char, buf, needed);
            *buf.add(needed) = 0;
        }
    }
    needed as c_int
}

/// Move owned bytes out as a caller-owned `(ptr, len)` via `Box::into_raw`, and
/// write `len` to `out_len`. `None` yields a null pointer with `*out_len = 0`.
/// The caller frees `ptr` with `free()` (global allocator == system allocator).
fn out_bytes(value: Option<Vec<u8>>, out_len: *mut usize) -> *mut u8 {
    match value {
        Some(v) => {
            let boxed: Box<[u8]> = v.into_boxed_slice();
            let len = boxed.len();
            if !out_len.is_null() {
                unsafe { *out_len = len };
            }
            // Box::into_raw on a boxed slice yields a `*mut [u8]` fat pointer;
            // casting to `*mut u8` keeps the data pointer (drops the length
            // metadata, which we've already handed back via `out_len`).
            Box::into_raw(boxed) as *mut u8
        }
        None => {
            if !out_len.is_null() {
                unsafe { *out_len = 0 };
            }
            std::ptr::null_mut()
        }
    }
}

// ── id3 STRING readers (caller-allocated out-buffer) ─────────────────────────

#[unsafe(no_mangle)]
pub extern "C" fn id3_read_lyrics(
    input: *const u8,
    in_len: usize,
    out: *mut c_char,
    out_cap: c_int,
) -> c_int {
    let bytes = unsafe { slice_from(input, in_len) };
    match crate::id3_ops::read_lyrics(bytes) {
        Some(s) => unsafe { write_str(out, out_cap, &s) },
        None => -1,
    }
}

#[unsafe(no_mangle)]
pub extern "C" fn id3_read_edited_lyrics(
    input: *const u8,
    in_len: usize,
    out: *mut c_char,
    out_cap: c_int,
) -> c_int {
    let bytes = unsafe { slice_from(input, in_len) };
    match crate::id3_ops::read_edited_lyrics(bytes) {
        Some(s) => unsafe { write_str(out, out_cap, &s) },
        None => -1,
    }
}

#[unsafe(no_mangle)]
pub extern "C" fn id3_read_album(
    input: *const u8,
    in_len: usize,
    out: *mut c_char,
    out_cap: c_int,
) -> c_int {
    let bytes = unsafe { slice_from(input, in_len) };
    match crate::id3_ops::read_album(bytes) {
        Some(s) => unsafe { write_str(out, out_cap, &s) },
        None => -1,
    }
}

#[unsafe(no_mangle)]
pub extern "C" fn id3_read_genre(
    input: *const u8,
    in_len: usize,
    out: *mut c_char,
    out_cap: c_int,
) -> c_int {
    let bytes = unsafe { slice_from(input, in_len) };
    match crate::id3_ops::read_genre(bytes) {
        Some(s) => unsafe { write_str(out, out_cap, &s) },
        None => -1,
    }
}

#[unsafe(no_mangle)]
pub extern "C" fn id3_read_suno_clip_id(
    input: *const u8,
    in_len: usize,
    out: *mut c_char,
    out_cap: c_int,
) -> c_int {
    let bytes = unsafe { slice_from(input, in_len) };
    match crate::id3_ops::read_suno_clip_id(bytes) {
        Some(s) => unsafe { write_str(out, out_cap, &s) },
        None => -1,
    }
}

// ── infer STRING reader (caller-allocated out-buffer) ─────────────────────────

#[unsafe(no_mangle)]
pub extern "C" fn inf_read_extension(
    input: *const u8,
    in_len: usize,
    out: *mut c_char,
    out_cap: c_int,
) -> c_int {
    let bytes = unsafe { slice_from(input, in_len) };
    match crate::infer_ops::read_extension(bytes) {
        Some(s) => unsafe { write_str(out, out_cap, &s) },
        None => -1,
    }
}

// ── id3 BYTE outputs (owned pointer + out_len; caller frees with free()) ─────

#[unsafe(no_mangle)]
pub extern "C" fn id3_read_cover(
    input: *const u8,
    in_len: usize,
    out_len: *mut usize,
) -> *mut u8 {
    let bytes = unsafe { slice_from(input, in_len) };
    out_bytes(crate::id3_ops::read_cover_bytes(bytes), out_len)
}

#[unsafe(no_mangle)]
pub extern "C" fn id3_write_edited_lyrics(
    input: *const u8,
    in_len: usize,
    text: *const c_char,
    out_len: *mut usize,
) -> *mut u8 {
    let bytes = unsafe { slice_from(input, in_len) };
    let text = unsafe { str_from(text) };
    out_bytes(
        crate::id3_ops::write_edited_lyrics(bytes, &text).ok(),
        out_len,
    )
}

/// Protagonist-picture write. Embeds `picture` (raw image bytes) as the APIC
/// LeadArtist frame, leaving cover/lyrics/other frames intact. Both `input` and
/// `picture` are borrowed `(ptr, len)` inputs; returns the new file bytes via the
/// owned-pointer shape (null on error, e.g. image-type inference failure).
#[unsafe(no_mangle)]
pub extern "C" fn id3_write_protagonist(
    input: *const u8,
    in_len: usize,
    picture: *const u8,
    picture_len: usize,
    out_len: *mut usize,
) -> *mut u8 {
    let bytes = unsafe { slice_from(input, in_len) };
    let picture = unsafe { slice_from(picture, picture_len) };
    out_bytes(
        crate::id3_ops::write_picture_lead_artist_protagonist(bytes, picture).ok(),
        out_len,
    )
}

/// SYLT edited-alignment write. `content` is a JSON string of `WordAlignment`
/// list: `[{"text":"...","start":0.123,"end":0.456}, ...]`. Returns new file
/// bytes via the owned-pointer shape (null on error).
#[unsafe(no_mangle)]
pub extern "C" fn id3_write_edited_synced_lyrics(
    input: *const u8,
    in_len: usize,
    content: *const c_char,
    out_len: *mut usize,
) -> *mut u8 {
    let bytes = unsafe { slice_from(input, in_len) };
    let content = unsafe { str_from(content) };
    out_bytes(
        crate::id3_ops::write_edited_synced_lyrics(bytes, &content).ok(),
        out_len,
    )
}

// ── audio engine transport (folded in from the former `src/audio/capi.rs`) ────
//
// SCAFFOLD (M0/U01). Exposes the `aud_*` C functions consumed from iosMain via
// the SAME `metadata` cinterop that carries the id3 `id3_*`, infer `inf_*`,
// db `db_*`, and codec `cod_*` surface above — the crate consolidation gives ONE
// cinterop (metadata.def, package `metadata.ffi`, header metadata_ffi.h) for the
// whole native crate. The engine is STATEFUL: `aud_new` boxes an `AudioEngine`
// and returns its raw pointer as an `int64_t` handle; every later call receives
// that handle back (as `uint64_t`) and reconstructs a `&mut AudioEngine`;
// `aud_free` drops it. All time args/returns are f64 SECONDS. Operational engine
// methods are `todo!()` until M4; the shim itself is complete so the staticlib
// exports every `aud_*` symbol the header declares.
//
// The signatures below are BINDING (ANCHOR-naming-layout §2) — the generated
// `metadata_ffi.h` mirrors them exactly.

// ── handle marshalling (the audio surface's `unsafe` boundary) ───────────────

/// Reconstruct a mutable engine reference from the opaque `uint64_t` handle.
///
/// # Safety
/// `handle` must be a non-zero pointer previously returned by `aud_new` and not
/// yet passed to `aud_free`. The Kotlin/Native caller owns the lifetime.
unsafe fn engine_from<'a>(handle: u64) -> &'a mut AudioEngine {
    unsafe { &mut *(handle as *mut AudioEngine) }
}

// ── lifecycle ────────────────────────────────────────────────────────────────

/// Construct an engine and return its opaque handle. REAL (does not panic).
#[unsafe(no_mangle)]
pub extern "C" fn aud_new() -> i64 {
    Box::into_raw(Box::new(AudioEngine::new())) as i64
}

/// Drop the engine behind `handle` (no-op on a null handle).
#[unsafe(no_mangle)]
pub extern "C" fn aud_free(handle: i64) {
    if handle != 0 {
        unsafe { drop(Box::from_raw(handle as *mut AudioEngine)) };
    }
}

// ── load ─────────────────────────────────────────────────────────────────────

/// Decode `bytes` into a sound slot; returns duration SECONDS.
#[unsafe(no_mangle)]
pub extern "C" fn aud_load(handle: u64, input: *const u8, in_len: usize) -> f64 {
    let engine = unsafe { engine_from(handle) };
    let bytes = unsafe { std::slice::from_raw_parts(input, in_len) };
    match engine.load_bytes(bytes) {
        Ok(duration) => duration,
        Err(e) => panic!("aud_load failed: {e}"),
    }
}

// ── transport (all time args are f64 SECONDS) ────────────────────────────────

#[unsafe(no_mangle)]
pub extern "C" fn aud_play(handle: u64, from_sec: f64) {
    let engine = unsafe { engine_from(handle) };
    engine.play(from_sec);
}

#[unsafe(no_mangle)]
pub extern "C" fn aud_pause(handle: u64) {
    let engine = unsafe { engine_from(handle) };
    engine.pause();
}

#[unsafe(no_mangle)]
pub extern "C" fn aud_resume(handle: u64) {
    let engine = unsafe { engine_from(handle) };
    engine.resume();
}

#[unsafe(no_mangle)]
pub extern "C" fn aud_seek_to(handle: u64, sec: f64) {
    let engine = unsafe { engine_from(handle) };
    engine.seek_to(sec);
}

#[unsafe(no_mangle)]
pub extern "C" fn aud_seek_by(handle: u64, sec: f64) {
    let engine = unsafe { engine_from(handle) };
    engine.seek_by(sec);
}

// ── loop region (seconds) ────────────────────────────────────────────────────

#[unsafe(no_mangle)]
pub extern "C" fn aud_set_loop(handle: u64, a_sec: f64, b_sec: f64) {
    let engine = unsafe { engine_from(handle) };
    engine.set_loop(a_sec, b_sec);
}

#[unsafe(no_mangle)]
pub extern "C" fn aud_clear_loop(handle: u64) {
    let engine = unsafe { engine_from(handle) };
    engine.clear_loop();
}

// ── poll/pull readouts ───────────────────────────────────────────────────────

#[unsafe(no_mangle)]
pub extern "C" fn aud_position(handle: u64) -> f64 {
    let engine = unsafe { engine_from(handle) };
    engine.position()
}

#[unsafe(no_mangle)]
pub extern "C" fn aud_state(handle: u64) -> u8 {
    let engine = unsafe { engine_from(handle) };
    engine.state()
}


// ── codec frame extraction ────────────────────────────────────────────────────

/// Extract raw RGBA frames from `input_path` and write them to `output_path`.
///
/// Panics on any error — there is no recovery path for video extraction.
#[unsafe(no_mangle)]
pub extern "C" fn cod_extract_rgba(input_path: *const c_char, output_path: *const c_char) {
    let input_path = unsafe { str_from(input_path) };
    let output_path = unsafe { str_from(output_path) };
    crate::codec_ops::extract_video_to_rgba(&input_path, &output_path);
}

// ── SYLT edited-synced lyrics (single JSON string — caller-allocated buffer) ──
/// Returns a JSON string of `List<WordAlignment>`: `[{"text":"...","start":0.123,"end":0.456}, ...]`.
/// `null` (return -1) when no edited SYLT frame exists.
/// Rust does all the SYLT parsing and word-boundary plumbing; Kotlin just deserializes.
#[unsafe(no_mangle)]
pub extern "C" fn id3_read_edited_synced_lyrics(
    input: *const u8,
    in_len: usize,
    out: *mut c_char,
    out_cap: c_int,
) -> c_int {
    let bytes = unsafe { slice_from(input, in_len) };
    match crate::id3_ops::read_edited_synced_lyrics(bytes) {
        Some(json) => unsafe { write_str(out, out_cap, &json) },
        None => -1,
    }
}

// ── audio: load from file path ────────────────────────────────────────────────

/// Decode a file at `path` into a sound slot; returns duration SECONDS.
#[unsafe(no_mangle)]
pub extern "C" fn aud_load_path(handle: u64, path: *const c_char) -> f64 {
    let engine = unsafe { engine_from(handle) };
    let path = unsafe { str_from(path) };
    match engine.load_path(&path) {
        Ok(duration) => duration,
        Err(e) => panic!("aud_load_path failed: {e}"),
    }
}

// ── offline decode (scaffold — M4) ────────────────────────────────────────────

/// Decodes input file to headerless little-endian interleaved f32 PCM, 16000 Hz
/// mono, written to `out_path`. Returns the PCM byte count; NEGATIVE means error.
/// Scaffold — real decode-to-PCM lands in M4.
#[unsafe(no_mangle)]
pub extern "C" fn aud_decode_to_16k_mono_f32(
    _input_path: *const c_char,
    _out_path: *const c_char,
) -> i64 {
    // M4: real decode-to-PCM via symphonia + rubato resampler.
    -1
}

// ── xmp reads (metadata-core/src/xmp_ops.rs — xmpkit std API, U51 "+ XMP"). ──
// Five independent projections over the same file bytes (same shape as the id3
// reads above), one per field the webMain `XmpItem.fromXmp` factory reads.
// null == the file carries no XMP packet, or the packet lacks that property.

#[unsafe(no_mangle)]
pub extern "C" fn xmp_read_original_document_id(
    input: *const u8,
    in_len: usize,
    out: *mut c_char,
    out_cap: c_int,
) -> c_int {
    let bytes = unsafe { slice_from(input, in_len) };
    match crate::xmp_ops::read_xmp_original_document_id(bytes) {
        Some(s) => unsafe { write_str(out, out_cap, &s) },
        None => -1,
    }
}

#[unsafe(no_mangle)]
pub extern "C" fn xmp_read_genre(
    input: *const u8,
    in_len: usize,
    out: *mut c_char,
    out_cap: c_int,
) -> c_int {
    let bytes = unsafe { slice_from(input, in_len) };
    match crate::xmp_ops::read_xmp_genre(bytes) {
        Some(s) => unsafe { write_str(out, out_cap, &s) },
        None => -1,
    }
}

#[unsafe(no_mangle)]
pub extern "C" fn xmp_read_lyrics(
    input: *const u8,
    in_len: usize,
    out: *mut c_char,
    out_cap: c_int,
) -> c_int {
    let bytes = unsafe { slice_from(input, in_len) };
    match crate::xmp_ops::read_xmp_lyrics(bytes) {
        Some(s) => unsafe { write_str(out, out_cap, &s) },
        None => -1,
    }
}

#[unsafe(no_mangle)]
pub extern "C" fn xmp_read_rating(
    input: *const u8,
    in_len: usize,
    out: *mut c_char,
    out_cap: c_int,
) -> c_int {
    let bytes = unsafe { slice_from(input, in_len) };
    match crate::xmp_ops::read_xmp_rating(bytes) {
        Some(s) => unsafe { write_str(out, out_cap, &s) },
        None => -1,
    }
}

#[unsafe(no_mangle)]
pub extern "C" fn xmp_read_project_name(
    input: *const u8,
    in_len: usize,
    out: *mut c_char,
    out_cap: c_int,
) -> c_int {
    let bytes = unsafe { slice_from(input, in_len) };
    match crate::xmp_ops::read_xmp_project_name(bytes) {
        Some(s) => unsafe { write_str(out, out_cap, &s) },
        None => -1,
    }
}

//! JVM/Android JNI shim over `metadata-core`.
//!
//! Each `extern "system"` function maps 1:1 to the Kotlin `external` methods of
//! `market.femi.Ffi`. The JNI symbol name encodes
//! the fully-qualified class: `Java_<pkg with _>_Ffi_<method>`.
//!
//! The MetadataService JVM/Android adapter (jvmMain/androidMain — written by the
//! platform agents) constructs `Id3Tags` from the read* calls and returns the
//! byte arrays from the write* calls, exactly like the web `WasmId3` object.
//!
//! Edition 2024: `no_mangle` must be spelled `#[unsafe(no_mangle)]`.
//!
//! CONSOLIDATION NOTE: this module was folded VERBATIM from the former
//! `metadata-jni` crate. Its function bodies call portable ops via
//! fully-qualified paths (`crate::id3_ops::read_lyrics(..)`); no glob
//! re-exports are used.

use jni::objects::{JByteArray, JClass, JString};
use jni::sys::{jboolean, jbyteArray, jdouble, jint, jlong, jstring};
use jni::JNIEnv;

// The stateful audio engine, reached through the flat crate module (`audio_ops`,
// re-exported at the crate root). The audio transport methods folded in at the
// bottom of this file box an `AudioEngine` and pass its raw pointer back and forth
// as a `jlong` handle — the same ONE `market.femi.Ffi` JNI class now
// carries BOTH the stateless metadata reads/writes above AND the stateful audio
// transport below.
use crate::audio_ops::AudioEngine;

// ── marshalling helpers (the ONLY FFI-aware code) ────────────────────────────

/// `Option<String>` -> a Java `String` reference, or Java `null` for `None`.
fn opt_string_to_jstring(env: &mut JNIEnv, value: Option<String>) -> jstring {
    match value {
        Some(s) => env
            .new_string(s)
            .map(|j| j.into_raw())
            .unwrap_or(std::ptr::null_mut()),
        None => std::ptr::null_mut(),
    }
}

/// `Option<Vec<u8>>` -> a Java `byte[]` reference, or Java `null` for `None`.
fn opt_bytes_to_jbytearray(env: &mut JNIEnv, value: Option<Vec<u8>>) -> jbyteArray {
    match value {
        Some(b) => env
            .byte_array_from_slice(&b)
            .map(|a| a.into_raw())
            .unwrap_or(std::ptr::null_mut()),
        None => std::ptr::null_mut(),
    }
}

/// `Result<Vec<u8>, String>` -> a Java `byte[]`; on `Err`, throw a
/// `RuntimeException` (the Kotlin side surfaces it) and return `null`.
fn result_bytes_to_jbytearray(env: &mut JNIEnv, value: Result<Vec<u8>, String>) -> jbyteArray {
    match value {
        Ok(b) => env
            .byte_array_from_slice(&b)
            .map(|a| a.into_raw())
            .unwrap_or(std::ptr::null_mut()),
        Err(msg) => {
            let _ = env.throw_new("java/lang/RuntimeException", msg);
            std::ptr::null_mut()
        }
    }
}

/// Read a Java `byte[]` into a Rust `Vec<u8>` (empty on any failure — the core
/// treats empty/garbage input as "no tag", matching the web behaviour).
fn bytes_of(env: &mut JNIEnv, input: &JByteArray) -> Vec<u8> {
    env.convert_byte_array(input).unwrap_or_default()
}

/// Read a Java `String` into a Rust `String`.
fn string_of(env: &mut JNIEnv, input: &JString) -> String {
    match env.get_string(input) {
        Ok(js) => js.into(),
        Err(_) => String::new(),
    }
}

// ── id3 reads (metadata-core/src/id3_ops.rs) ─────────────────────────────────

#[unsafe(no_mangle)]
pub extern "system" fn Java_market_femi_Ffi_id3ReadLyrics<'l>(
    mut env: JNIEnv<'l>,
    _class: JClass<'l>,
    input: JByteArray<'l>,
) -> jstring {
    let bytes = bytes_of(&mut env, &input);
    opt_string_to_jstring(&mut env, crate::id3_ops::read_lyrics(&bytes))
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_market_femi_Ffi_id3ReadEditedLyrics<'l>(
    mut env: JNIEnv<'l>,
    _class: JClass<'l>,
    input: JByteArray<'l>,
) -> jstring {
    let bytes = bytes_of(&mut env, &input);
    opt_string_to_jstring(&mut env, crate::id3_ops::read_edited_lyrics(&bytes))
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_market_femi_Ffi_id3ReadAlbum<'l>(
    mut env: JNIEnv<'l>,
    _class: JClass<'l>,
    input: JByteArray<'l>,
) -> jstring {
    let bytes = bytes_of(&mut env, &input);
    opt_string_to_jstring(&mut env, crate::id3_ops::read_album(&bytes))
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_market_femi_Ffi_id3ReadGenre<'l>(
    mut env: JNIEnv<'l>,
    _class: JClass<'l>,
    input: JByteArray<'l>,
) -> jstring {
    let bytes = bytes_of(&mut env, &input);
    opt_string_to_jstring(&mut env, crate::id3_ops::read_genre(&bytes))
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_market_femi_Ffi_id3ReadSunoClipId<'l>(
    mut env: JNIEnv<'l>,
    _class: JClass<'l>,
    input: JByteArray<'l>,
) -> jstring {
    let bytes = bytes_of(&mut env, &input);
    opt_string_to_jstring(&mut env, crate::id3_ops::read_suno_clip_id(&bytes))
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_market_femi_Ffi_id3ReadCover<'l>(
    mut env: JNIEnv<'l>,
    _class: JClass<'l>,
    input: JByteArray<'l>,
) -> jbyteArray {
    let bytes = bytes_of(&mut env, &input);
    opt_bytes_to_jbytearray(&mut env, crate::id3_ops::read_cover_bytes(&bytes))
}

// ── SYLT read (edited descriptor — single JSON string) ───────────────────────
//
// Returns a JSON string of `List<WordAlignment>`: `[{"text":"...","start":0.123,"end":0.456}, ...]`.
// Rust does all the SYLT parsing and word-boundary plumbing; Kotlin just deserializes.

#[unsafe(no_mangle)]
pub extern "system" fn Java_market_femi_Ffi_id3ReadEditedSyncedLyrics<'l>(
    mut env: JNIEnv<'l>,
    _class: JClass<'l>,
    input: JByteArray<'l>,
) -> jstring {
    let bytes = bytes_of(&mut env, &input);

    let json_str = crate::id3_ops::read_edited_synced_lyrics(&bytes);
    opt_string_to_jstring(&mut env, json_str)
}

// ── id3 writes (metadata-core/src/id3_ops.rs) ─────────────────────────────────

#[unsafe(no_mangle)]
pub extern "system" fn Java_market_femi_Ffi_id3WriteEditedLyrics<'l>(
    mut env: JNIEnv<'l>,
    _class: JClass<'l>,
    input: JByteArray<'l>,
    text: JString<'l>,
) -> jbyteArray {
    let bytes = bytes_of(&mut env, &input);
    let text = string_of(&mut env, &text);
    result_bytes_to_jbytearray(&mut env, crate::id3_ops::write_edited_lyrics(&bytes, &text))
}

/// `writeProtagonist(bytes, picture: byte[])` — embeds `picture` as the APIC
/// LeadArtist frame, leaving cover/lyrics/other frames intact. Returns the new
/// file bytes (throws a RuntimeException on core error, e.g. inference failure).
#[unsafe(no_mangle)]
pub extern "system" fn Java_market_femi_Ffi_id3WriteProtagonist<'l>(
    mut env: JNIEnv<'l>,
    _class: JClass<'l>,
    input: JByteArray<'l>,
    picture: JByteArray<'l>,
) -> jbyteArray {
    let bytes = bytes_of(&mut env, &input);
    let picture = bytes_of(&mut env, &picture);
    result_bytes_to_jbytearray(
        &mut env,
        crate::id3_ops::write_picture_lead_artist_protagonist(&bytes, &picture),
    )
}

/// `writeEditedSyncedLyrics(bytes, content)` — `content` is a JSON string of
/// `[{"timestamp_ms":N,"token":"..."}, ...]`.
#[unsafe(no_mangle)]
pub extern "system" fn Java_market_femi_Ffi_id3WriteEditedSyncedLyrics<'l>(
    mut env: JNIEnv<'l>,
    _class: JClass<'l>,
    input: JByteArray<'l>,
    content: JString<'l>,
) -> jbyteArray {
    let bytes = bytes_of(&mut env, &input);
    let content = string_of(&mut env, &content);
    result_bytes_to_jbytearray(&mut env, crate::id3_ops::write_edited_synced_lyrics(&bytes, &content))
}

// ── infer (metadata-core/src/infer_ops.rs) ────────────────────────────────────

#[unsafe(no_mangle)]
pub extern "system" fn Java_market_femi_Ffi_inferExtension<'l>(
    mut env: JNIEnv<'l>,
    _class: JClass<'l>,
    input: JByteArray<'l>,
) -> jstring {
    let bytes = bytes_of(&mut env, &input);
    opt_string_to_jstring(&mut env, crate::infer_ops::read_extension(&bytes))
}

// ── audio engine transport (folded in from the former `src/audio/jni.rs`) ─────
//
// SCAFFOLD (M0/U01). The audio engine no longer has its own JNI class: the crate
// consolidation gives ONE native crate with ONE JNI class, `market.femi.Ffi`,
// that carries BOTH the stateless metadata reads/writes above AND these stateful
// audio transport methods — the same class that already owns the
// `decodeTo16kMonoF32` decode-to-PCM method (ANCHOR §5). Every `extern "system"`
// function therefore maps 1:1 to a Kotlin `external` method of
// `market.femi.Ffi`, and the JNI symbol name encodes that class:
// `Java_market_femi_Ffi_<method>`.
//
// The engine is STATEFUL: `new` boxes an `AudioEngine` and returns its raw
// pointer as a `jlong` handle; every later call receives that handle back and
// reconstructs a `&mut AudioEngine` from it; `free` drops it. All time
// args/returns are f64 SECONDS. Bodies delegate to the portable engine, whose
// operational methods are `todo!()` until M4 — the shim itself is complete so the
// symbols export and the host cdylib links.

// ── handle marshalling (the ONLY FFI-aware code) ─────────────────────────────

/// Reconstruct a mutable engine reference from the opaque `jlong` handle.
///
/// # Safety
/// `handle` must be a non-zero pointer previously returned by `new` and not
/// yet passed to `free`. The JNI boundary cannot enforce this; the Kotlin
/// `Ffi` actual owns the handle's lifetime.
unsafe fn engine_from<'a>(handle: jlong) -> &'a mut AudioEngine {
    unsafe { &mut *(handle as *mut AudioEngine) }
}

// ── lifecycle ────────────────────────────────────────────────────────────────

/// Construct an engine and return its opaque handle. REAL (does not panic) so the
/// Kotlin actual gets a usable handle in the scaffold.
#[unsafe(no_mangle)]
pub extern "system" fn Java_market_femi_Ffi_audNew<'l>(
    _env: JNIEnv<'l>,
    _class: JClass<'l>,
) -> jlong {
    Box::into_raw(Box::new(AudioEngine::new())) as jlong
}

/// Drop the engine behind `handle` (no-op on a null handle).
#[unsafe(no_mangle)]
pub extern "system" fn Java_market_femi_Ffi_audFree<'l>(
    _env: JNIEnv<'l>,
    _class: JClass<'l>,
    handle: jlong,
) {
    if handle != 0 {
        // Reclaim ownership of the box and let it drop.
        unsafe { drop(Box::from_raw(handle as *mut AudioEngine)) };
    }
}

// ── load ─────────────────────────────────────────────────────────────────────

/// Decode a `byte[]` into a sound slot; returns the slot id as a `jlong`.
#[unsafe(no_mangle)]
pub extern "system" fn Java_market_femi_Ffi_audLoad<'l>(
    mut env: JNIEnv<'l>,
    _class: JClass<'l>,
    handle: jlong,
    bytes: JByteArray<'l>,
) -> jdouble {
    let engine = unsafe { engine_from(handle) };
    let data = env.convert_byte_array(&bytes).unwrap_or_default();
    match engine.load_bytes(&data) {
        Ok(duration) => duration,
        Err(e) => panic!("audLoad failed: {e}"),
    }
}

// ── transport (all time args are f64 SECONDS) ────────────────────────────────

#[unsafe(no_mangle)]
pub extern "system" fn Java_market_femi_Ffi_audPlay<'l>(
    _env: JNIEnv<'l>,
    _class: JClass<'l>,
    handle: jlong,
    from_sec: jdouble,
) {
    let engine = unsafe { engine_from(handle) };
    engine.play(from_sec);
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_market_femi_Ffi_audPause<'l>(
    _env: JNIEnv<'l>,
    _class: JClass<'l>,
    handle: jlong,
) {
    let engine = unsafe { engine_from(handle) };
    engine.pause();
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_market_femi_Ffi_audResume<'l>(
    _env: JNIEnv<'l>,
    _class: JClass<'l>,
    handle: jlong,
) {
    let engine = unsafe { engine_from(handle) };
    engine.resume();
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_market_femi_Ffi_audSeekTo<'l>(
    _env: JNIEnv<'l>,
    _class: JClass<'l>,
    handle: jlong,
    sec: jdouble,
) {
    let engine = unsafe { engine_from(handle) };
    engine.seek_to(sec);
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_market_femi_Ffi_audSeekBy<'l>(
    _env: JNIEnv<'l>,
    _class: JClass<'l>,
    handle: jlong,
    sec: jdouble,
) {
    let engine = unsafe { engine_from(handle) };
    engine.seek_by(sec);
}

// ── loop region (seconds) ────────────────────────────────────────────────────

#[unsafe(no_mangle)]
pub extern "system" fn Java_market_femi_Ffi_audSetLoop<'l>(
    _env: JNIEnv<'l>,
    _class: JClass<'l>,
    handle: jlong,
    a_sec: jdouble,
    b_sec: jdouble,
) {
    let engine = unsafe { engine_from(handle) };
    engine.set_loop(a_sec, b_sec);
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_market_femi_Ffi_audClearLoop<'l>(
    _env: JNIEnv<'l>,
    _class: JClass<'l>,
    handle: jlong,
) {
    let engine = unsafe { engine_from(handle) };
    engine.clear_loop();
}

// ── poll/pull readouts (the FrameClock loop calls these per tick) ────────────

#[unsafe(no_mangle)]
pub extern "system" fn Java_market_femi_Ffi_audPosition<'l>(
    _env: JNIEnv<'l>,
    _class: JClass<'l>,
    handle: jlong,
) -> jdouble {
    let engine = unsafe { engine_from(handle) };
    engine.position()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_market_femi_Ffi_audState<'l>(
    _env: JNIEnv<'l>,
    _class: JClass<'l>,
    handle: jlong,
) -> jint {
    let engine = unsafe { engine_from(handle) };
    engine.state() as jint
}


// ── codec frame extraction (JNI) ──────────────────────────────────────────────

#[unsafe(no_mangle)]
pub extern "system" fn Java_market_femi_Ffi_codecExtractRgba<'l>(
    mut _env: JNIEnv<'l>,
    _class: JClass<'l>,
    input_path: JString<'l>,
    output_path: JString<'l>,
) {
    let input_path = string_of(&mut _env, &input_path);
    let output_path = string_of(&mut _env, &output_path);
    crate::codec_ops::extract_video_to_rgba(&input_path, &output_path);
}

// ── offline decode (JNI — symphonia+rubato, U51). ──
// Decodes inputPath to headerless little-endian interleaved f32 PCM, 16000 Hz mono,
// written to outPcmPath. Returns the PCM byte count; NEGATIVE means error.
#[unsafe(no_mangle)]
pub extern "system" fn Java_market_femi_Ffi_audDecodeTo16kMonoF32<'l>(
    mut env: JNIEnv<'l>,
    _class: JClass<'l>,
    input_path: JString<'l>,
    out_pcm_path: JString<'l>,
) -> jlong {
    let input_path = string_of(&mut env, &input_path);
    let out_pcm_path = string_of(&mut env, &out_pcm_path);
    // M4: real decode-to-PCM via symphonia + rubato resampler.
    let _ = (input_path, out_pcm_path);
    -1
}

// ── xmp reads (metadata-core/src/xmp_ops.rs — xmpkit std API, U51 "+ XMP"). ──
// Five independent projections over the same file bytes (same shape as the id3
// reads above), one per field the webMain `XmpItem.fromXmp` factory reads.
// null == the file carries no XMP packet, or the packet lacks that property.

#[unsafe(no_mangle)]
pub extern "system" fn Java_market_femi_Ffi_xmpReadOriginalDocumentId<'l>(
    mut env: JNIEnv<'l>,
    _class: JClass<'l>,
    input: JByteArray<'l>,
) -> jstring {
    let bytes = bytes_of(&mut env, &input);
    opt_string_to_jstring(&mut env, crate::xmp_ops::read_xmp_original_document_id(&bytes))
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_market_femi_Ffi_xmpReadGenre<'l>(
    mut env: JNIEnv<'l>,
    _class: JClass<'l>,
    input: JByteArray<'l>,
) -> jstring {
    let bytes = bytes_of(&mut env, &input);
    opt_string_to_jstring(&mut env, crate::xmp_ops::read_xmp_genre(&bytes))
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_market_femi_Ffi_xmpReadLyrics<'l>(
    mut env: JNIEnv<'l>,
    _class: JClass<'l>,
    input: JByteArray<'l>,
) -> jstring {
    let bytes = bytes_of(&mut env, &input);
    opt_string_to_jstring(&mut env, crate::xmp_ops::read_xmp_lyrics(&bytes))
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_market_femi_Ffi_xmpReadRating<'l>(
    mut env: JNIEnv<'l>,
    _class: JClass<'l>,
    input: JByteArray<'l>,
) -> jstring {
    let bytes = bytes_of(&mut env, &input);
    opt_string_to_jstring(&mut env, crate::xmp_ops::read_xmp_rating(&bytes))
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_market_femi_Ffi_xmpReadProjectName<'l>(
    mut env: JNIEnv<'l>,
    _class: JClass<'l>,
    input: JByteArray<'l>,
) -> jstring {
    let bytes = bytes_of(&mut env, &input);
    opt_string_to_jstring(&mut env, crate::xmp_ops::read_xmp_project_name(&bytes))
}

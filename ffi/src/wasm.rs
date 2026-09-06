use wasm_bindgen::prelude::*;
use crate::audio_ops::AudioEngine;

// ── id3 reads & writes ───────────────────────────────────────────────────────

#[wasm_bindgen]
pub fn id3_read_lyrics(bytes: &[u8]) -> Option<String> {
    crate::id3_ops::read_lyrics(bytes)
}

#[wasm_bindgen]
pub fn id3_read_edited_lyrics(bytes: &[u8]) -> Option<String> {
    crate::id3_ops::read_edited_lyrics(bytes)
}

#[wasm_bindgen]
pub fn id3_read_album(bytes: &[u8]) -> Option<String> {
    crate::id3_ops::read_album(bytes)
}

#[wasm_bindgen]
pub fn id3_read_genre(bytes: &[u8]) -> Option<String> {
    crate::id3_ops::read_genre(bytes)
}

#[wasm_bindgen]
pub fn id3_read_suno_clip_id(bytes: &[u8]) -> Option<String> {
    crate::id3_ops::read_suno_clip_id(bytes)
}

#[wasm_bindgen]
pub fn id3_read_cover(bytes: &[u8]) -> Option<Vec<u8>> {
    crate::id3_ops::read_cover_bytes(bytes)
}

#[wasm_bindgen]
pub fn id3_read_edited_synced_lyrics(bytes: &[u8]) -> Option<String> {
    crate::id3_ops::read_edited_synced_lyrics(bytes)
}

#[wasm_bindgen]
pub fn id3_write_edited_lyrics(bytes: &[u8], text: String) -> Result<Vec<u8>, String> {
    crate::id3_ops::write_edited_lyrics(bytes, &text)
}

#[wasm_bindgen]
pub fn id3_write_edited_synced_lyrics(bytes: &[u8], content: String) -> Result<Vec<u8>, String> {
    crate::id3_ops::write_edited_synced_lyrics(bytes, &content)
}

#[wasm_bindgen]
pub fn id3_write_protagonist(bytes: &[u8], picture: &[u8]) -> Result<Vec<u8>, String> {
    crate::id3_ops::write_picture_lead_artist_protagonist(bytes, picture)
}

// ── infer ────────────────────────────────────────────────────────────────────

#[wasm_bindgen]
pub fn inf_read_extension(bytes: &[u8]) -> Option<String> {
    crate::infer_ops::read_extension(bytes)
}

// ── xmp reads ────────────────────────────────────────────────────────────────

#[wasm_bindgen]
pub fn xmp_read_original_document_id(bytes: &[u8]) -> Option<String> {
    crate::xmp_ops::read_xmp_original_document_id(bytes)
}

#[wasm_bindgen]
pub fn xmp_read_genre(bytes: &[u8]) -> Option<String> {
    crate::xmp_ops::read_xmp_genre(bytes)
}

#[wasm_bindgen]
pub fn xmp_read_lyrics(bytes: &[u8]) -> Option<String> {
    crate::xmp_ops::read_xmp_lyrics(bytes)
}

#[wasm_bindgen]
pub fn xmp_read_rating(bytes: &[u8]) -> Option<String> {
    crate::xmp_ops::read_xmp_rating(bytes)
}

#[wasm_bindgen]
pub fn xmp_read_project_name(bytes: &[u8]) -> Option<String> {
    crate::xmp_ops::read_xmp_project_name(bytes)
}



///

// ── audio engine handle marshalling ──────────────────────────────────────────

/// Reconstruct a mutable engine reference from the opaque `i64` handle.
unsafe fn engine_from<'a>(handle: i64) -> &'a mut AudioEngine {
    unsafe { &mut *(handle as *mut AudioEngine) }
}

// ── audio engine transport ───────────────────────────────────────────────────

#[wasm_bindgen]
pub fn aud_new() -> i64 {
    Box::into_raw(Box::new(AudioEngine::new())) as i64
}

#[wasm_bindgen]
pub fn aud_free(handle: i64) {
    if handle != 0 {
        unsafe { drop(Box::from_raw(handle as *mut AudioEngine)) };
    }
}

#[wasm_bindgen]
pub fn aud_load(handle: i64, bytes: &[u8]) -> f64 {
    let engine = unsafe { engine_from(handle) };
    match engine.load_bytes(bytes) {
        Ok(duration) => duration,
        Err(e) => panic!("aud_load failed: {e}"),
    }
}

// Note: aud_load_path is strictly cfg-gated out of wasm32 in audio_ops.rs.
// We expose a stub here that panics if the Kotlin web target ever accidentally calls it.
#[wasm_bindgen]
pub fn aud_load_path(_handle: i64, _path: String) -> f64 {
    panic!("aud_load_path is not supported on wasm32");
}

#[wasm_bindgen]
pub fn aud_play(handle: i64, from_sec: f64) {
    let engine = unsafe { engine_from(handle) };
    match engine.play(from_sec) {
        Ok(_) => {},
        Err(e) => panic!("aud_play failed: {e}"),
    }
}

#[wasm_bindgen]
pub fn aud_pause(handle: i64) {
    let engine = unsafe { engine_from(handle) };
    engine.pause();
}

#[wasm_bindgen]
pub fn aud_resume(handle: i64) {
    let engine = unsafe { engine_from(handle) };
    engine.resume();
}

#[wasm_bindgen]
pub fn aud_seek_to(handle: i64, sec: f64) {
    let engine = unsafe { engine_from(handle) };
    engine.seek_to(sec);
}

#[wasm_bindgen]
pub fn aud_seek_by(handle: i64, sec: f64) {
    let engine = unsafe { engine_from(handle) };
    engine.seek_by(sec);
}

#[wasm_bindgen]
pub fn aud_set_loop(handle: i64, a_sec: f64, b_sec: f64) {
    let engine = unsafe { engine_from(handle) };
    engine.set_loop(a_sec, b_sec);
}

#[wasm_bindgen]
pub fn aud_clear_loop(handle: i64) {
    let engine = unsafe { engine_from(handle) };
    engine.clear_loop();
}

#[wasm_bindgen]
pub fn aud_position(handle: i64) -> f64 {
    let engine = unsafe { engine_from(handle) };
    engine.position()
}

#[wasm_bindgen]
pub fn aud_state(handle: i64) -> u8 {
    let engine = unsafe { engine_from(handle) };
    engine.state()
}

// Scaffold decode to PCM — matching the JNI/C-ABI shims.
#[wasm_bindgen]
pub fn aud_decode_to_16k_mono_f32(_input_path: String, _out_pcm_path: String) -> i64 {
    -1
}

// ── codec frame extraction ───────────────────────────────────────────────────

#[wasm_bindgen]
pub fn cod_extract_rgba(input_path: String, output_path: String) {
    crate::codec_ops::extract_video_to_rgba(&input_path, &output_path);
}
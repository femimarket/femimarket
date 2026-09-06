//! metadata-core — the portable metadata engine shared by web/JVM/Android/iOS.
//!
//! The web build historically kept the id3/xmp/infer logic INSIDE the
//! `#[wasm_bindgen]` shims (see `wasm-id3/src/lib.rs`). That logic is pure Rust
//! and portable; only the `JsValue` marshalling was web-specific. This crate is
//! that logic extracted verbatim in behaviour, but returning plain Rust types so
//! the JNI (JVM/Android) and C-ABI (iOS) shims can reuse the exact same code.
//!
//! Every function here is binding-agnostic. The three shims each translate
//! plain Rust <-> their FFI representation and do nothing else.
pub mod xmp_ops;

pub mod id3_ops;
pub mod infer_ops;
pub mod codec_ops;

// The STATEFUL kira playback engine ops, folded in from the former standalone
// `audio-engine` crate so metadata-core is the SINGLE native crate. Like
// id3_ops/infer_ops this is a FLAT, portable, binding-agnostic ops module holding
// the engine (`audio_ops::AudioEngine`, re-exported below). Its FFI is NOT a
// submodule of its own: the audio JNI methods and the `ae_*` C-ABI functions are
// folded into the crate-level `jni` / `capi` shims below (ONE JNI class, ONE
// cinterop header carrying both the metadata AND the audio surface). It is NOT on
// the web path — kira/cpal do not build for wasm32 — so the wasm-* crates (which
// pull id3/infer directly) never see it. Kept always-declared (not cfg-gated)
// because the portable `AudioEngine` type exists on every target.
pub mod audio_ops;

// The audio engine is available at `audio_ops::AudioEngine` for shims that
// need the type directly (JNI/capi box/unbox it).

// ── cfg-gated native binding shims ───────────────────────────────────────────
//
// These modules are the ONLY binding-aware code in the crate; each is a thin
// translation layer over the portable functions/engine re-exported above. They
// are mutually exclusive by target and ALL excluded from the wasm32 web build
// (the web binding stays in the separate wasm-* crates). Each shim carries BOTH
// the metadata surface AND the audio-engine surface — the audio FFI was folded in
// from the former `src/audio/{jni,capi}.rs`, so there is no separate audio FFI
// module:
//
//   jni  — JVM desktop host + Android. Gated to every non-wasm, non-iOS target.
//          Exposes the metadata `Java_market_femi_Ffi_*` reads/writes
//          AND the audio transport methods under that SAME symbol prefix — ONE JNI
//          class, `market.femi.Ffi`, carries both the stateless
//          metadata reads/writes AND the stateful audio engine (the same class
//          that owns the `decodeTo16kMonoF32` decode-to-PCM method per ANCHOR §5).
//          Pulls in the `jni` crate (also gated identically in Cargo.toml).
//   capi — iOS only. Exposes the metadata C-ABI `md_*` functions AND the audio
//          C-ABI `ae_*` functions into the SAME cinterop header (metadata_ffi.h,
//          package `metadata.ffi`).
//
// The gate `all(not(wasm32), not(ios))` deliberately INCLUDES the desktop host
// (target_os = "macos"/"linux"/"windows") so the host `cargo build` links and
// type-checks the JNI shims — that host cdylib is what the desktop JVM loads.
#[cfg(all(not(target_arch = "wasm32"), not(target_os = "ios")))]
mod jni;

#[cfg(target_os = "ios")]
mod capi;

#[cfg(target_arch = "wasm32")]
pub mod wasm;

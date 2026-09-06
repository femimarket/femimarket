//! Portable, binding-agnostic audio engine — REAL kira transport (M4/U50).
//!
//! Folded in from the former `audio-engine` crate when that crate was dissolved
//! into metadata-core. This is the stateful playback engine behind the
//! `AudioEngine` commonMain port: a kira `AudioManager` plus a `SlotMap` of
//! decoded `StaticSoundData` sounds (the loaded master track), seconds-domain
//! seek/loop, and wait-free position/state readouts that `PlaybackViewModel`'s
//! FrameClock loop polls per tick — the POLL/PULL model of [AE-03]/[NAT-30],
//! with NO Rust->Kotlin callback anywhere in the audio path.
//!
//! Decisions this implementation binds to (KNOWLEDGE-BASE.md):
//!   [AE-02]   kira 0.12 seconds-domain API: `StaticSoundData::from_cursor`,
//!             `start_position(PlaybackPosition::Seconds)`, live
//!             `set_loop_region(a..b)`, `seek_to`/`seek_by` in f64 seconds;
//!             cloning `StaticSoundData` is zero-copy (Arc-backed frames), so
//!             the SlotMap keeps one decoded copy and every play clones it.
//!   [AE-03]   position()/state() are wait-free atomic loads on the live
//!             `StaticSoundHandle` — safe to call at 60 Hz from the UI thread.
//!   [AE-08]   decode goes through kira's default symphonia backend (mp3/wav/
//!             flac/ogg out of the box); duration comes from
//!             `StaticSoundData::duration()` as f64 seconds.
//!   [NAT-17]  seek is kira's genuine live in-place `seek_to` — a lock-free
//!             ring-buffer command — NOT the web's pause+play workaround.
//!   [ADVR3-06] the `AudioManager` (and therefore cpal's output stream) is
//!             constructed LAZILY on the first load, never in `new()`, so the
//!             iOS adapter can activate AVAudioSession — and the Android
//!             adapter can run `nativeInitAndroid` — strictly BEFORE the
//!             stream opens.
//!   [NATPROD-R3-01]/[NATPROD-R4-01] device-loss recovery is internal to
//!             kira/cpal; there is no rebuild verb here, and the decoded
//!             `StaticSoundData` stays resident across OS-session hiccups.
//!
//! Threading contract: the engine is owned by exactly one Kotlin adapter which
//! serializes access (the FFI shims reconstruct `&mut AudioEngine` from the raw
//! handle). kira internally ships commands to its own cpal audio thread over
//! lock-free queues, so no method here blocks on audio rendering.

use std::io::Cursor;
use std::time::Duration;

use kira::sound::static_sound::{StaticSoundData, StaticSoundHandle};
use kira::sound::{PlaybackState, Region};
use kira::{AudioManager, AudioManagerSettings, DefaultBackend, Tween};
use slotmap::{DefaultKey, SlotMap};

/// An instant, click-free transition (kira's default `Tween` is a ~10 ms fade;
/// `Duration::ZERO` gives exact-frame pause/stop parity with the web lane's
/// `node.stop()` — [AE-02]).
fn instant_tween() -> Tween {
    Tween {
        duration: Duration::ZERO,
        ..Default::default()
    }
}

/// The stateful engine. Heap-allocated; its raw pointer is the opaque FFI
/// handle (`jlong` over JNI, `int64_t`/`uint64_t` over the C-ABI).
///
/// The struct stays private-fielded so it is opaque across the FFI boundary —
/// callers only ever see the pointer handle, never the layout.
pub struct AudioEngine {
    /// The kira manager owning the OS output stream (cpal: CoreAudio / WASAPI /
    /// ALSA / AAudio). `None` until the first load — see [ADVR3-06]: session
    /// setup on iOS and `nativeInitAndroid` on Android MUST precede the stream
    /// open, so `new()` never opens it.
    manager: Option<AudioManager<DefaultBackend>>,
    /// The decoded sounds. The engine holds ONE master track at a time (the
    /// Ports.kt `load(bytes): Double` contract): each successful load clears
    /// the map and inserts the fresh `StaticSoundData`, whose key becomes
    /// `active_sound_key`.
    sounds: SlotMap<DefaultKey, StaticSoundData>,
    /// Key of the currently loaded master track inside `sounds`.
    active_sound_key: Option<DefaultKey>,
    /// The live playback instance, present from the first `play()` until the
    /// next load/teardown. Poll targets (`position`/`state`) read this handle.
    handle: Option<StaticSoundHandle>,
    /// The armed A-B loop region in SECONDS, mirrored onto the live handle via
    /// `set_loop_region` and applied at construction of every new instance.
    loop_region_sec: Option<(f64, f64)>,
    /// Playhead fallback for when no live handle exists yet (fresh load, or
    /// seek-before-first-play): keeps `position()` honest so the UI playhead
    /// lands where the user pointed even before audio ever started.
    cold_position_sec: f64,
}

impl AudioEngine {
    /// Construct an empty engine WITHOUT opening an audio stream. REAL and
    /// non-panicking so `nativeNew`/`ae_new` always hand back a valid handle;
    /// the stream opens lazily on the first load (see `ensure_manager`).
    pub fn new() -> Self {
        AudioEngine {
            manager: None,
            sounds: SlotMap::new(),
            active_sound_key: None,
            handle: None,
            loop_region_sec: None,
            cold_position_sec: 0.0,
        }
    }

    /// Open the kira/cpal output stream if it is not open yet ([ADVR3-06]:
    /// this is the ONE place the stream opens, and it runs strictly after the
    /// platform adapters' session/ndk-context preparation because both happen
    /// before their first `load` call). Device-loss and sample-rate changes
    /// after this point are recovered internally by kira/cpal
    /// ([NATPROD-R4-01]) — no rebuild path exists or is needed.
    fn ensure_manager(&mut self) -> Result<(), String> {
        if self.manager.is_none() {
            let manager = AudioManager::<DefaultBackend>::new(AudioManagerSettings::default())
                .map_err(|error| format!("failed to open the audio output stream: {error:?}"))?;
            self.manager = Some(manager);
        }
        Ok(())
    }

    /// Install a freshly decoded master track: stop any live instance
    /// instantly, replace the SlotMap contents (single-master-track contract),
    /// reset the cold playhead, and hand back the duration in SECONDS — the
    /// exact value the Ports.kt `load(bytes): Double` contract returns.
    fn install_sound(&mut self, sound: StaticSoundData) -> f64 {
        if let Some(mut old_handle) = self.handle.take() {
            old_handle.stop(instant_tween());
        }
        let duration_seconds = sound.duration().as_secs_f64();
        self.sounds.clear();
        self.active_sound_key = Some(self.sounds.insert(sound));
        self.cold_position_sec = 0.0;
        duration_seconds
    }

    /// Decode in-memory file bytes (mp3/wav/flac/ogg via kira's symphonia
    /// backend, [AE-08]) into the master track. Returns duration SECONDS.
    pub fn load_bytes(&mut self, bytes: &[u8]) -> Result<f64, String> {
        self.ensure_manager()?;
        let sound = StaticSoundData::from_cursor(Cursor::new(bytes.to_vec()))
            .map_err(|error| format!("audio decode from bytes failed: {error:?}"))?;
        Ok(self.install_sound(sound))
    }

    /// Decode an audio file at `path` into the master track (the path-flavored
    /// twin of `load_bytes` for adapters that hold a real filesystem path).
    /// Returns duration SECONDS.
    #[cfg(not(target_arch = "wasm32"))]
    pub fn load_path(&mut self, path: &str) -> Result<f64, String> {
        self.ensure_manager()?;
        let sound = StaticSoundData::from_file(path)
                   .map_err(|error| format!("audio decode from path {path:?} failed: {error:?}"))?;
        Ok(self.install_sound(sound))
    }

    /// Play the loaded master track from `from_sec` seconds. Always starts a
    /// fresh instance (stopping the previous one instantly), with the armed
    /// loop region applied at construction — this matches the port mapping
    /// (`play(fromSec, loop)` → set/clear loop, then play) and the web lane's
    /// new-BufferSource-per-play semantics. The `StaticSoundData` clone here
    /// is zero-copy ([AE-02], Arc-backed frames).
    pub fn play(&mut self, from_sec: f64) -> Result<(), String> {
        self.ensure_manager()?;
        if let Some(mut old_handle) = self.handle.take() {
            old_handle.stop(instant_tween());
        }
        let active_key = self
            .active_sound_key
            .ok_or_else(|| "play() called before a successful load(): no sound is loaded".to_string())?;
        let sound = self
            .sounds
            .get(active_key)
            .ok_or_else(|| "loaded sound slot is missing from the SlotMap".to_string())?;
        let mut instance = sound.start_position(from_sec);
        if let Some((loop_in_sec, loop_out_sec)) = self.loop_region_sec {
            instance = instance.loop_region(loop_in_sec..loop_out_sec);
        }
        let manager = self
            .manager
            .as_mut()
            .ok_or_else(|| "audio manager vanished after ensure_manager".to_string())?;
        let new_handle = manager
            .play(instance)
            .map_err(|error| format!("kira play failed: {error:?}"))?;
        self.handle = Some(new_handle);
        self.cold_position_sec = from_sec;
        Ok(())
    }

    /// Instant, click-free pause (kira Tween ZERO — [AE-02]). No-op when
    /// nothing is live.
    pub fn pause(&mut self) {
        if let Some(handle) = self.handle.as_mut() {
            handle.pause(instant_tween());
        }
    }

    /// Resume after a pause, instantly (the web lane resumes without a fade,
    /// so native mirrors it with Tween ZERO). No-op when nothing is live.
    pub fn resume(&mut self) {
        if let Some(handle) = self.handle.as_mut() {
            handle.resume(instant_tween());
        }
    }

    /// Absolute live seek to `sec` seconds — kira's in-place, lock-free
    /// transport reposition ([NAT-17]); the sound keeps playing (or stays
    /// paused) with no stop/re-trigger. Before the first play, records the
    /// target so `position()` and a later `play()` agree with the UI.
    pub fn seek_to(&mut self, sec: f64) {
        match self.handle.as_mut() {
            Some(handle) => handle.seek_to(sec),
            None => self.cold_position_sec = sec.max(0.0),
        }
    }

    /// Relative live seek by `sec` seconds (negative allowed) — same
    /// [NAT-17] in-place command as `seek_to`.
    pub fn seek_by(&mut self, sec: f64) {
        match self.handle.as_mut() {
            Some(handle) => handle.seek_by(sec),
            None => self.cold_position_sec = (self.cold_position_sec + sec).max(0.0),
        }
    }

    /// Arm a live loop region `[a_sec, b_sec]` (seconds) without restarting:
    /// applied to the live handle immediately (kira re-derives frame bounds
    /// and keeps playing — [AE-02] per-clip ±5s looping), and remembered so
    /// every future instance is constructed with it.
    pub fn set_loop(&mut self, a_sec: f64, b_sec: f64) {
        self.loop_region_sec = Some((a_sec, b_sec));
        if let Some(handle) = self.handle.as_mut() {
            handle.set_loop_region(a_sec..b_sec);
        }
    }

    /// Clear the loop region (kira `IntoOptionalRegion` None) live and for
    /// future instances.
    pub fn clear_loop(&mut self) {
        self.loop_region_sec = None;
        if let Some(handle) = self.handle.as_mut() {
            handle.set_loop_region(None::<Region>);
        }
    }

    /// Current playhead in SECONDS — a wait-free atomic load on the live
    /// handle ([AE-03]; kira maintains the value from its own audio thread).
    /// Falls back to the cold playhead before the first play.
    pub fn position(&self) -> f64 {
        match self.handle.as_ref() {
            Some(handle) => handle.position(),
            None => self.cold_position_sec,
        }
    }

    /// Playback state as the canonical u8 tag (M4-FFI-SYMBOLS §2.2):
    /// `0 = Playing, 1 = Pausing, 2 = Paused, 3 = WaitingToResume,
    ///  4 = Resuming, 5 = Stopping, 6 = Stopped`.
    /// The Kotlin poll reconciles `isPlaying = (tag == 0 || tag == 1 ||
    /// tag == 5)`; no-handle-yet reads as Stopped.
    pub fn state(&self) -> u8 {
        match self.handle.as_ref().map(StaticSoundHandle::state) {
            Some(PlaybackState::Playing) => 0,
            Some(PlaybackState::Pausing) => 1,
            Some(PlaybackState::Paused) => 2,
            Some(PlaybackState::WaitingToResume) => 3,
            Some(PlaybackState::Resuming) => 4,
            Some(PlaybackState::Stopping) => 5,
            Some(PlaybackState::Stopped) | None => 6,
        }
    }
}

impl Default for AudioEngine {
    fn default() -> Self {
        Self::new()
    }
}

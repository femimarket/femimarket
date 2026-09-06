package market.femi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * SATELLITE — the audio-engine seam. Extracts the WebAudio playback engine trapped in
 * Studio() (play()/pause() 442-495, playhead advance/wrap 570-588) and AudioMathState
 * (JsCode.kt:436-445). Owns its own state, no editor overlap.
 *
 * State is StateFlow (not Compose state) because a non-Compose consumer — the
 * SharedArrayBuffer clock writer (Studio.kt:405-408,533) — must observe position.
 *
 * Testable: new PlaybackViewModel(FakeAudioEngine(), FakeFrameClock()); play(); the
 * fake's advance(sec) drives positionSec through the pure loop-wrap math.
 */
//class PlaybackViewModel(
//    private val audio: AudioEngine,
//    private val frame: FrameClock,
//) : ViewModel() {
//
//    private val _isPlaying = MutableStateFlow(false)           // ← globalIsPlaying   Studio.kt:232
//    private val _positionMs = MutableStateFlow(0.0)            // ← currentPlaybackPos Studio.kt:235
//    private val _durationMs = MutableStateFlow(0.0)            // ← globalDuration     Studio.kt:250
//    private val _loopRegion = MutableStateFlow<LoopRegion?>(null) // ← activeLoopRegion Studio.kt:267
//    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
//    val positionMs: StateFlow<Double> = _positionMs.asStateFlow()
//    val durationMs: StateFlow<Double> = _durationMs.asStateFlow()
//    val loopRegion: StateFlow<LoopRegion?> = _loopRegion.asStateFlow()
//
//    /** Handed the active clip's bytes by WorkspaceViewModel. */
//    fun setClip(bytes: ByteArray) {
//        viewModelScope.launch { _durationMs.value = audio.load(bytes) * 1000 }
//    }
//
//    fun play(fromMs: Double, loop: LoopRegion?) {
//        TODO("migrate from Studio.kt:442-495 — pick effective loop bounds (pure), audio.play(), start frame loop")
//    }
//
//    fun pause() {
//        TODO("migrate from Studio.kt:pause() — audio.pause(), _isPlaying.value = false")
//    }
//
//    fun seek(ms: Double) = audio.seek(ms / 1000.0)
//
//    fun setLoop(inMs: Double?, outMs: Double?) {
//        // pure validation: start cannot be after end, end cannot be before start (Timeline.kt:274-283)
//        TODO("migrate loop-region set-point rules from Timeline.kt:274-283")
//    }
//
//    // pure fns to move in verbatim (test them directly, they're already isolated):
//    //   getLoopedTime(t, loopStart, loopEnd)   ← JsCode.kt:442
//    //   playhead advance / loop wrap            ← Studio.kt:570-588
//}

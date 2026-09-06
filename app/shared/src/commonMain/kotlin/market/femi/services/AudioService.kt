package market.femi.services

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.kdroidfilter.composemediaplayer.audio.AudioPlayer
import io.github.kdroidfilter.composemediaplayer.audio.AudioPlayerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.onEach
import market.femi.LoopRegion
import kotlin.time.Duration.Companion.milliseconds

interface AudioService {                                       // wraps WebAudio: AudioContext/BufferSource
    fun play(url:String, fromSec: Double?=null, loop: LoopRegion?=null)
    fun pause()
    fun seek(sec: Double)
    fun setLoop(loop: LoopRegion?)
    fun sync()                                           // UI calls each frame: refresh positionSec/isPlaying
    var positionSec: Double
    var isPlaying: Boolean
    var duration: Double
}

fun createRealAudioService(): AudioService = RealComposeMediaAudioService()

// Backed by ComposeMediaPlayer's AudioPlayer, which has no reactive state and no A-B sub-region
// loop. A single periodic ticker — an endless flow paced by delay, no imperative loop — drives
// positionSec/isPlaying AND enforces the LoopRegion by seeking back to inMs when position crosses
// outMs. Best-effort: overshoot ≈ tick interval + seek latency, not sample-accurate. Player calls
// stay on Dispatchers.Main. Call release() to stop the ticker + free the player.
class RealComposeMediaAudioService(
    val player: AudioPlayer = AudioPlayer(),
) : AudioService {
    private var currentUrl: String? = null
    private var loop: LoopRegion? = null

    // One tick every ~30ms with no explicit loop: an endless flow throttled by delay.
    private val ticks: Flow<Unit> =
        generateSequence(Unit) { it }.asFlow().onEach { delay(30.milliseconds) }

    override var positionSec by mutableStateOf(0.0)
    override var isPlaying by mutableStateOf(false)
    override var duration by mutableStateOf(0.0)

//    init {
//        ticks.onEach {
//            val posMs = player.currentPosition() ?: 0L
//            positionSec.value = posMs / 1000.0
//            isPlaying.value = player.currentPlayerState() in
//                    listOf(AudioPlayerState.PLAYING, AudioPlayerState.BUFFERING)
//
//            loop?.outMs?.let { outMs ->
//                if (posMs >= outMs.toLong()) player.seekTo((loop?.inMs ?: 0.0).toLong())
//            }
//        }.launchIn(scope)
//    }


    override fun play(url:String, fromSec: Double?, loop: LoopRegion?) {
        loop?.let { this.loop = it }
        if (url != currentUrl) {
            player.play(url)
        } else {
            player.play()
        }
        fromSec?.let { player.seekTo((it * 1000).toLong()) }
        currentUrl = url
    }

    override fun pause() = player.pause()

    override fun seek(sec: Double) = player.seekTo((sec * 1000).toLong())

    override fun setLoop(loop: LoopRegion?) { this.loop = loop }

    override fun sync() {
        player.currentDuration()?.let {
            duration = it / 1000.0
        }
        val posMs = player.currentPosition() ?: 0L
        positionSec = posMs / 1000.0
        isPlaying = player.currentPlayerState() == AudioPlayerState.PLAYING
        loop?.outMs?.let { outMs ->
            if (posMs >= outMs.toLong()) player.seekTo((loop?.inMs ?: 0.0).toLong())
        }
    }

    fun release() {
        player.release()
    }
}

class FakeAudioService : AudioService {
    override fun play(url:String, fromSec: Double?, loop: LoopRegion?) {}
    override fun pause() {}
    override fun seek(sec: Double) {}
    override fun setLoop(loop: LoopRegion?) {}
    override fun sync() {}
    override var positionSec by mutableStateOf(0.0)
    override var isPlaying by mutableStateOf(false)
    override var duration by mutableStateOf(0.0)
}
package market.femi

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import market.femi.models.Audio
import market.femi.models.Image
import market.femi.models.Matrix
import market.femi.models.Video
import market.femi.models.WordAlignment
import market.femi.models.XmpItem

/**
 * Seam ports — the interfaces the ViewModels depend on so they never touch the host.
 * Production impls are thin adapters in webMain (IndexedDB, OPFS, WebAudio, wasm, ktor);
 * tests inject in-memory fakes. RenderSegment is the existing top-level class (AsyncJob.kt:26).
 *
 * Migration note: the FIRST impl of each port should just delegate to the existing
 * getDbMaybe()/JsCode/RustBindings functions — no logic moves on the first pass.
 */




interface GenerationGateway {                                 // wraps fal/vertex/veo, ElevenLabs, Qwen, LM Studio, flux
    suspend fun forceAlign(lyrics: String, audioName: String): List<WordAlignment>  // Qwen: lyrics + audio → timings
    suspend fun generateImages(prompt: String, refImage: String): List<String> // filenames
    suspend fun submitVeo(prompt: String, refImages: List<String>): List<String> // filenames
    suspend fun generateText(prompt: String): String
}

interface MediaCodec {                                        // wraps RustBindings mux/decode + mediabunny
    suspend fun getDurationMs(name: String): Double
    suspend fun muxSequence(segments: List<RenderSegment>, outName: String): String
    suspend fun decodeToBitmaps(name: String): Int
}


interface SettingsStore {                                     // wraps russhwolf Settings + Rust local-storage
    fun getString(key: String): String?
    fun putString(key: String, value: String?)
    fun getLong(key: String): Long?
    fun putLong(key: String, value: Long?)
}

interface IdGenerator {                                       // wraps Uuid.generateV7/V4 + filename builders
    fun uuidV7(): String
    fun uuidV4(): String
    fun newFileName(ext: String): String
}

// NOTE: UI effects are NOT a port. A port is an input the VM pulls from the outside world
// (db/files/network) and therefore fakes. Effects are OUTPUT — the VM produces them, like its
// state — so they live on the VM (WorkspaceViewModel.effects) and are observed, never faked.



interface FrameClock {                                        // wraps withFrameNanos frame loop
    fun frames(): Flow<Long>
}

// Two more ports from the proposal — add when a journey needs them:
//   SharedSignalPort  (SharedArrayBuffer clock/videos + worker pool)
//   AuthTokenProvider (Google GSI + Vertex/fal tokens, JWT expiry)

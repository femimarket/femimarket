package market.femi.services

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import market.femi.toRgbaFileName


interface CodecService {                                        // wraps RustBindings mux/decode + mediabunny
//    // [~] seqmux-01 / MC-06 (Parity Ledger ROW 1) — SIGNATURE GROWTH. The master-audio path + start
//    // offset + progress callback now thread in (previously dropped at JobsViewModel.kt:107). lyricText
//    // is NOT a param (seqmux-05: it is never drawn to pixels — handleMux stamps it as Video.audioLineText
//    // instead). onProgress defaults to {} so no later signature churn. The fake keys its output off
//    // outName and ignores the new args (the journey plans zero segments → behavior unchanged).
//    suspend fun muxSequence(
//        segments: List<RenderSegment>,
//        masterAudioName: String,
//        audioStartMs: Double,
//        outName: String,
//        onProgress: (Float) -> Unit = {},
//    ): String
//
//    suspend fun decodeToBitmaps(name: String): Int
    suspend fun extractRgba(name: String)
//
//    // [+] cliped-03 (Parity Ledger ROW 2) — single-clip trim+speed+master-audio bake (the clip-editor
//    // "Sync/Trim Video" button), DISTINCT from muxSequence (which stitches MANY clips and cannot carry
//    // master audio). Mirrors RustBindings.mux 1:1. speed is a Long percentage (10L..200L). Driven by
//    // ClipBakeAsyncJob → handleClipBake. The fake returns a canned out name and records its args.
//    suspend fun muxClip(
//        videoName: String,
//        audioName: String,
//        videoStartMs: Double,
//        videoEndMs: Double,
//        audioStartMs: Double,
//        speed: Long,
//        outName: String,
//    ): String

    // [+] contracts-decode-pcm-fourth-widener / NET-02 / ADV5 (Parity Ledger ROW 3) — FOURTH MediaCodec
    // widener. Offline decode → 16 kHz mono f32 PCM for the Qwen force-aligner; returns a SCRATCH
    // path/name (app-private cache, NOT a FileStore-namespaced name). The web actual keeps its WebAudio
    // path; native actuals delegate to metadata-core symphonia+rubato FFI (U51). Called from inside the
    // native forceAlign actual. The fake returns "$audioName.16k.pcm".
    suspend fun decodeTo16kMonoF32(audioName: String): String
}

fun createRealCodecService(kv: KvService): CodecService = RealWebhookCodecService(kv)


class RealWebhookCodecService(
    private val kv: KvService,
    private val client: HttpClient = HttpClient()
) : CodecService {
    override suspend fun decodeTo16kMonoF32(audioName: String): String {
//        TODO("Not yet implemented")
        return ""
    }

    override suspend fun extractRgba(name: String) {
        client.post("http://${kv.codecUrl}/hooks/extract-rgba") {
            contentType(ContentType.Application.Json)
            setBody(
                mapOf(
                    "input_file" to name,
                    "output_file" to name.toRgbaFileName()
                )
            )
        }
    }

}



class FakeCodecService(private val fileService: FileService) : CodecService {
    override suspend fun extractRgba(name: String) {
        val newFilename = name.toRgbaFileName()
        fileService.writeBytes(newFilename,"fake data".encodeToByteArray())
    }
//    // a canned duration keyed to nothing (OPAQUE stub, §9) — duration isn't derivable from a name.
//    private val durationMs: Double = 4_000.0
//    override suspend fun getDurationMs(name: String): Double = durationMs
//    // ROW 2 — records the args of the last muxClip so a test could assert the clip window/speed threaded
//    // through; the journey never bakes a clip so this stays an inert opaque stub (§9).
//    var lastMuxClip: MuxClipCall? = null
//        private set
//    override suspend fun muxSequence(
//        segments: List<RenderSegment>,
//        // ROW 1 — grown args are IGNORED by the fake (it keys output off outName): the master-audio pass
//        // is a real-codec concern, and the journey plans zero segments, so ignoring them keeps it green.
//        masterAudioName: String,
//        audioStartMs: Double,
//        outName: String,
//        onProgress: (Float) -> Unit,
//    ): String {
//        // async wait — a real suspend point so jobs.isRunning is observable "during" the mux (F13)
//        delay(1)
//        // A real muxer stitches the ordered visibility windows planRenderSegments handed it; given NO
//        // windows there is nothing to render, so a real codec produces no output file. Model that here by
//        // materializing the muxed file ONLY when the plan is non-empty — which makes planRenderSegments
//        // LOAD-BEARING instead of ignored: an empty plan (exactly the degenerate zero-width-window
//        // regression JobsViewModel.handleVeo's endMs stamping guards against) now leaves finalCut.name
//        // absent, so step 13's files.exists(finalCut.name) FAILS instead of passing on a phantom file. In
//        // the journey the single clip carries a real [startMs, endMs) window (endMs stamped from the
//        // probed duration), so the plan is non-empty and the file is written exactly as before.
//        if (segments.isNotEmpty()) {
//            // write the muxed output into the shared store so step 13's files.exists(finalCut.name) really holds
//            files.writeBytes(outName, outName.encodeToByteArray())
//        }
//        return outName
//    }
//    override suspend fun decodeToBitmaps(name: String): Int = 0
//
//    // ROW 2 — single-clip trim+speed+master-audio bake. Canned: returns outName, records the call. No
//    // journey step exercises the clip bake (ClipBakeAsyncJob is never dispatched by the journey), so it
//    // stays inert. Materializes the output into the shared store so a future test asserting on the baked
//    // file's existence would hold, mirroring muxSequence's store write.
//    override suspend fun muxClip(
//        videoName: String,
//        audioName: String,
//        videoStartMs: Double,
//        videoEndMs: Double,
//        audioStartMs: Double,
//        speed: Long,
//        outName: String,
//    ): String {
//        delay(1)
//        lastMuxClip = MuxClipCall(videoName, audioName, videoStartMs, videoEndMs, audioStartMs, speed, outName)
//        files.writeBytes(outName, outName.encodeToByteArray())
//        return outName
//    }

    // ROW 3 — offline decode to 16 kHz mono f32 PCM. Derived from the input name (like the gateway fakes)
    // so a test could prove which audio flowed in; no real decode, journey green (the aligner runs behind
    // the native forceAlign actual, not this fake, in the journey's fake-gateway lane).
    override suspend fun decodeTo16kMonoF32(audioName: String): String = "$audioName.pcm"
}
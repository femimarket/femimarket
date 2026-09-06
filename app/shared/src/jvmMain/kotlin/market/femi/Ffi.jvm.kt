package market.femi

import java.io.File

//actual object Ffi {
//    init {
//        val osName = System.getProperty("os.name").lowercase()
//        val libName = when {
//            osName.contains("mac") || osName.contains("darwin") -> "libffi.dylib"
//            osName.contains("win") -> "ffi.dll"
//            else -> "libffi.so"
//        }
//        val resource = Ffi::class.java.getResourceAsStream("/natives/$libName")
//            ?: error("/natives/$libName not on classpath — run build.sh first")
//        val extracted = File.createTempFile("ffi", libName.substringAfterLast('.'))
//        extracted.deleteOnExit()
//        resource.use { input -> extracted.outputStream().use { input.copyTo(it) } }
//        System.load(extracted.absolutePath)
//    }
//
//    // ── id3 reads (metadata-core/src/id3_ops.rs — null == frame absent). ──
//    actual external fun id3ReadLyrics(bytes: ByteArray): String?
//    actual external fun id3ReadEditedLyrics(bytes: ByteArray): String?
//    actual external fun id3ReadAlbum(bytes: ByteArray): String?
//    actual external fun id3ReadGenre(bytes: ByteArray): String?
//    actual external fun id3ReadSunoClipId(bytes: ByteArray): String?
//    actual external fun id3ReadCover(bytes: ByteArray): ByteArray?
//    // Returns JSON string: `[[ms, token], ...]` — null when no edited SYLT.
//    actual external fun id3ReadEditedSyncedLyrics(bytes: ByteArray): String?
//
//    // ── id3 writes (metadata-core/src/id3_ops.rs — return the new file bytes). ──
//    actual external fun id3WriteEditedLyrics(bytes: ByteArray, text: String): ByteArray
//    actual external fun id3WriteEditedSyncedLyrics(bytes: ByteArray, content: String): ByteArray
//    actual external fun id3WriteProtagonist(bytes: ByteArray, picture: ByteArray): ByteArray
//
//    // ── infer (metadata-core/src/infer_ops.rs). ──
//    actual external fun inferExtension(bytes: ByteArray): String?
//
//    // ── codec (metadata-core/src/codec_ops.rs). ──
//    actual external fun codecExtractRgba(inputPath: String, outputPath: String)
//
//    // ── xmp reads (metadata-core/src/xmp_ops.rs via jni.rs — xmpkit, U51 "+ XMP"). ──
//    // Five independent projections over the same file bytes (same shape as the id3 reads
//    // above), one per field the webMain `XmpItem.fromXmp` factory reads. null == the file
//    // carries no XMP packet, or the packet lacks that property.
//    actual external fun xmpReadOriginalDocumentId(bytes: ByteArray): String?
//    actual external fun xmpReadGenre(bytes: ByteArray): String?
//    actual external fun xmpReadLyrics(bytes: ByteArray): String?
//    actual external fun xmpReadRating(bytes: ByteArray): String?
//    actual external fun xmpReadProjectName(bytes: ByteArray): String?
//
//    // ── audio engine transport (stateful — kira over cpal). ──
//    // Construct an engine; returns the opaque handle.
//    actual external fun audNew(): Long
//    // Destroy the engine behind the handle (adapter dispose / "stop").
//    actual external fun audFree(handle: Long)
//    // Load in-memory bytes as the ONE master track (replacing any prior sound); returns duration SECONDS.
//    actual external fun audLoad(handle: Long, bytes: ByteArray): Double
//    // Load a file path as the master track; returns duration SECONDS (iOS-parity variant).
//    actual external fun audLoadPath(handle: Long, path: String): Double
//    // Start playback from an absolute position (seconds).
//    actual external fun audPlay(handle: Long, fromSec: Double)
//    // Instant, click-free pause (Tween ZERO on the Rust side).
//    actual external fun audPause(handle: Long)
//    // Resume from the paused position.
//    actual external fun audResume(handle: Long)
//    // Absolute in-place seek — kira live seek, NO pause+play workaround on native (NAT-17).
//    actual external fun audSeekTo(handle: Long, sec: Double)
//    // Relative in-place seek (seconds delta).
//    actual external fun audSeekBy(handle: Long, sec: Double)
//    // Arm the A-B loop region live (both bounds in seconds); kira re-derives frame bounds in place.
//    actual external fun audSetLoop(handle: Long, aSec: Double, bSec: Double)
//    // Disarm the loop region.
//    actual external fun audClearLoop(handle: Long)
//    // Wait-free position poll (seconds) — read once per FrameClock tick (AE-03 pull model).
//    actual external fun audPosition(handle: Long): Double
//    // Wait-free playback-state poll. kira PlaybackState codes (M4-FFI-SYMBOLS §2.2):
//    // 0=Playing 1=Pausing 2=Paused 3=WaitingToResume 4=Resuming 5=Stopping 6=Stopped.
//    actual external fun audState(handle: Long): Int
//
//    // ── offline decode (metadata-core/src/decode_ops.rs via jni.rs — symphonia+rubato, U51). ──
//    // Decodes inputPath to headerless little-endian interleaved f32 PCM, 16000 Hz mono, written to
//    // outPcmPath. Returns the PCM byte count; NEGATIVE means error (the Kotlin wrapper throws).
//    actual external fun audDecodeTo16kMonoF32(inputPath: String, outPcmPath: String): Long
//
//}

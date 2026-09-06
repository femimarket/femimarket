package market.femi

import market.femi.models.Image
import market.femi.models.Video
import market.femi.models.WordAlignment



/** A generation/mux job produced these; Workspace collects and persists them. */
data class JobResult(
    val videos: List<Video> = emptyList(),
    val images: List<Image> = emptyList(),
)

/** Loop in/out points on the timeline (ms). Replaces the raw Pair<Double?,Double?>. */
data class LoopRegion(val inMs: Double?, val outMs: Double?)

/** A file the user picked, carried without exposing browser File handles. */
data class PickedFile(val name: String, val bytes: ByteArray) {
    override fun equals(other: Any?) = this === other
    override fun hashCode() = name.hashCode()
}

/** Opaque handle for a chosen directory (FileSystemDirectoryHandle in prod, a stub in tests). */
interface DirRef

/** A visibility window on the timeline for one clip — pure, consumed by MediaCodec.muxSequence. */
data class RenderSegment(
    val video: Video,
    val visibleStartMs: Double,
    val visibleEndMs: Double,
)

// ── Background jobs (owned by JobsViewModel) ──
sealed interface BackgroundAsyncJob
data class VeoAsyncJob(
    val finalPrompt: String,
    val refImages: List<String>,
    val audioLineText: String?,
    val audioLineStartMs: Double,
    val project: String,
) : BackgroundAsyncJob
data class MuxAsyncJob(
    val clips: List<Video>,
    val masterAudioPath: String,
    val audioStartMs: Double,
    val lyricText: String,
    val project: String,
) : BackgroundAsyncJob
data class ImageGenAsyncJob(
    val prompt: String,
    val refImage: String,
    val audioLineText: String?,
    val theme: String?,
    val project: String,
) : BackgroundAsyncJob

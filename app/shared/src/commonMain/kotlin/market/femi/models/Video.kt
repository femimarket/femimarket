package market.femi.models

import kotlinx.serialization.Serializable
import kotlin.math.floor
import kotlin.time.Clock
import kotlin.time.Instant

@Serializable
data class Video(
    val id: String,
    val model: VideoModel = VideoModel.Unknown,
    val name: String = "",
    val raw: String = "",
    val durationMs: Double = 0.0,
    val endMs: Double = 0.0,
    val inputImages: List<ImageOld> = emptyList(),
    val inputVideos: List<VideoOld> = emptyList(),
    val project: String = "",
    val prompt: String? = null,
    val sort: Int? = null,
    val speed: Long = 100,
    val startMs: Double = 0.0,
    val audioLineText: String? = null,
    val audioLineStartMs: Double = 0.0,
    val audioLineId: Int? = null,
    val selected: Boolean = false,
    val width: Int = 0,
    val export: Boolean = false,
    val height: Int = 0,
    val timelineDisabled: Boolean = false,
    val timelineDurationMs: Double = 0.0,
    val lane: Int = 0,
    val createdAt: Instant = Clock.System.now()
) {
    fun rgbaName(): String = "$name.raw"

    companion object {
        fun buildLanes(videos: List<Video>): List<Video> {
            val sortedIndices = videos.indices.sortedBy { videos[it].createdAt }
            val projectLanes = mutableListOf<MutableList<Pair<Double, Double>>>()
            val results = videos.toMutableList()
            for (origIdx in sortedIndices) {
                val video = videos[origIdx]
                val speedFactor = video.speed / 100.0
                val durationOnTimelineMs =
                    floor((video.endMs - video.startMs) / speedFactor)
                val startMs = video.audioLineStartMs
                val endMs = startMs + durationOnTimelineMs
                var assignedLane = projectLanes.indexOfFirst { lane ->
                    lane.none { (s, e) -> startMs < e && endMs > s }
                }
                if (assignedLane != -1) {
                    projectLanes[assignedLane].add(startMs to endMs)
                } else {
                    projectLanes.add(mutableListOf(startMs to endMs))
                    assignedLane = projectLanes.size - 1
                }
                results[origIdx] = video.copy(
                    timelineDurationMs = durationOnTimelineMs,
                    lane = assignedLane,
                )
            }
            return results
        }
    }
}



@Serializable
data class VideoOld(
    val id: String? = null,
    val model: VideoModel = VideoModel.Unknown,
    val name: String = "",
    val raw: String = "",
    val durationMs: Double = 0.0,
    val endMs: Double = 0.0,
    val inputImages: List<ImageOld> = emptyList(),
    val inputVideos: List<Video> = emptyList(),
    val project: String = "",
    val prompt: String? = null,
    val sort: Int? = null,
    val speed: Long = 100,
    val startMs: Double = 0.0,
    val audioLineText: String? = null,
    val audioLineStartMs: Double = 0.0,
    val audioLineId: Int? = null,
    val selected: Boolean = false,
    val width: Int = 0,
    val export: Boolean = false,
    val height: Int = 0,
    val timelineDisabled: Boolean = false,
    val timelineDurationMs: Double = 0.0,
    val lane: Int = 0,
) {
    fun rgbaName(): String = "$name.raw"

    companion object {
        fun buildLanes(videos: List<Video>): List<Video> {
            val sortedIndices = videos.indices.sortedBy { videos[it].createdAt }
            val projectLanes = mutableListOf<MutableList<Pair<Double, Double>>>()
            val results = videos.toMutableList()
            for (origIdx in sortedIndices) {
                val video = videos[origIdx]
                val speedFactor = video.speed / 100.0
                val durationOnTimelineMs =
                    floor((video.endMs - video.startMs) / speedFactor)
                val startMs = video.audioLineStartMs
                val endMs = startMs + durationOnTimelineMs
                var assignedLane = projectLanes.indexOfFirst { lane ->
                    lane.none { (s, e) -> startMs < e && endMs > s }
                }
                if (assignedLane != -1) {
                    projectLanes[assignedLane].add(startMs to endMs)
                } else {
                    projectLanes.add(mutableListOf(startMs to endMs))
                    assignedLane = projectLanes.size - 1
                }
                results[origIdx] = video.copy(
                    timelineDurationMs = durationOnTimelineMs,
                    lane = assignedLane,
                )
            }
            return results
        }
    }
}

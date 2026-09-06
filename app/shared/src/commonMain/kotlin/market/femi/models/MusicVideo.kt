package market.femi.models

import kotlinx.serialization.Serializable

@Serializable
data class MusicVideo(
    val id: Int,
    val audios: List<Audio>,
    val videos: List<Video>,
    val images: List<Image>,
)

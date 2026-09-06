package market.femi.models

import kotlinx.serialization.Serializable

@Serializable
data class AudioTimestampedLine(
    val text: String,
    val start: Double = 0.0,
    val end: Double = 0.0,
)

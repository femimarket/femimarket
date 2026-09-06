package market.femi.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AudioLine(
    val id: Int,
    val text: String,
    val startMs: Double,
    val context: String? = null,
    val goal: String? = null,
    val themes: List<AudioTheme>,
    val expands: kotlin.collections.List<kotlin.String>? = null,
    val scenes: kotlin.collections.List<kotlin.String>? = null
)

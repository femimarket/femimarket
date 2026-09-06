package market.femi.models

import kotlinx.serialization.Serializable

@Serializable
data class WordAlignment(
    val text: String,
    val start: Double,
    val end: Double,
)

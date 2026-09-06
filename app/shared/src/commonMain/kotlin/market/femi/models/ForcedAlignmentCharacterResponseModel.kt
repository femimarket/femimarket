package market.femi.models

import kotlinx.serialization.Serializable

@Serializable
data class ForcedAlignmentCharacterResponseModel(
    val text: String,
    val start: Float,
    val end: Float,
)

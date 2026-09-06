package market.femi.models

import kotlinx.serialization.Serializable

@Serializable
data class ForcedAlignmentWordResponseModel(
    val text: String,
    val start: Float,
    val end: Float,
    val loss: Float,
)

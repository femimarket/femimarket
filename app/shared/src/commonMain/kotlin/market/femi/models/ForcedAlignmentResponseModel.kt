package market.femi.models

import kotlinx.serialization.Serializable

@Serializable
data class ForcedAlignmentResponseModel(
    val characters: List<ForcedAlignmentCharacterResponseModel>,
    val words: List<ForcedAlignmentWordResponseModel>,
    val loss: Float,
)

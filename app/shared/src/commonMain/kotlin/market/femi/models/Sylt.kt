package market.femi.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Sylt(
    val lang: String = "eng",
    val description: String = "",
    // Natively maps to [[10, "Word1"], [15, ""]] out of the box!
    val content: List<Pair<Int, String>> = emptyList()
)
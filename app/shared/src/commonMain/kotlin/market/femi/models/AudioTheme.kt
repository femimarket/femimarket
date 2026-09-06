package market.femi.models

import kotlinx.serialization.Serializable

@Serializable
data class AudioTheme(
    val id: Int?=null,
    val theme: String,
    val expand: String? = null,
    val scene: String? = null,
)

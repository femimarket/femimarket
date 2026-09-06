package market.femi.models

import kotlinx.serialization.Serializable

@Serializable
data class Image(
    val id: String,
    val model: ImageModel = ImageModel.Unknown,
    val name: String = "",
    val prompt: String? = null,
    val rating: Int? = null,
    val project: String = "",
    val audioLineId: Int? = null,
    val audioLineText: String? = null,
    val audioLineGoal: String? = null,
    val audioLineContext: String? = null,
    val startMs: Double? = null,
    val theme: String? = null,
    val expand: String? = null,
    val scene: String? = null,
)


@Serializable
data class ImageOld(
    val id: String? = null,
    val model: ImageModel = ImageModel.Unknown,
    val name: String = "",
    val prompt: String? = null,
    val rating: Int? = null,
    val project: String = "",
    val audioLineId: Int? = null,
    val audioLineText: String? = null,
    val audioLineGoal: String? = null,
    val audioLineContext: String? = null,
    val startMs: Double? = null,
    val theme: String? = null,
    val expand: String? = null,
    val scene: String? = null,
)

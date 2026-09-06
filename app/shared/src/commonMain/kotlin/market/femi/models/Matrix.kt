package market.femi.models

import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable

data class Matrix (

    @SerialName(value = "camera_motion") @Required val cameraMotion: kotlin.collections.List<kotlin.String>,

    @SerialName(value = "colors") @Required val colors: kotlin.collections.List<kotlin.String>,

    @SerialName(value = "environment") @Required val environment: kotlin.collections.List<kotlin.String>,

    @SerialName(value = "mood") @Required val mood: kotlin.collections.List<kotlin.String>,

    @SerialName(value = "motion") @Required val motion: kotlin.collections.List<kotlin.String>,

    @SerialName(value = "name") @Required val name: kotlin.String,

    @SerialName(value = "scene") @Required val scene: kotlin.collections.List<kotlin.String>,

    @SerialName(value = "style") @Required val style: kotlin.collections.List<kotlin.String>,

    @SerialName(value = "vibe") @Required val vibe: kotlin.collections.List<kotlin.String>,

    @SerialName(value = "where") @Required val `where`: kotlin.collections.List<kotlin.String>,

    @SerialName(value = "id") val id: kotlin.String

)
package market.femi.models

import kotlinx.serialization.Serializable

@Serializable
data class XmpItem(
    val id: Int? = null,
    val mmOriginalDocumentID: String,
    val name: String,
    val dmGenre: String? = null,
//    val image: String? = null,
//    val like: Boolean? = null,
    val rating: Int = 0,
    val dmLyrics: String? = null,
//    val editedLyrics: String? = null,
//    val elevenLabsForcedAlignment: ForcedAlignmentResponseModel? = null,
//    val protagonist: String? = null,
    val projectName: String = "",
//    val uid: String? = null,
//    val audioLines: List<AudioLine> = emptyList(),
//    val about: String? = null,
//    val video: String? = null,
//    val lyricTokens: List<String> = emptyList(),
) {
    // Empty companion so the wasm factory can attach as `XmpItem.Companion.fromXmp`
    // in webMain (see XmpItemXmp.kt) — call sites `XmpItem.fromXmp(...)` stay unchanged.
    companion object
}

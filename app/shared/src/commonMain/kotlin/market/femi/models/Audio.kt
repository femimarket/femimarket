package market.femi.models

import kotlinx.serialization.Serializable



@Serializable
data class Audio(
    val id: String,
    val backedUp: Boolean = false,
    val name: String = "",
    val error: String? = null,
    val genre: String? = null,
    val image: String,
    val like: Boolean? = null,
    val lyrics: String? = null,
    val editedLyrics: String? = null,
    val elevenLabsForcedAlignment: com.example.elevenlabs.models.ForcedAlignmentResponseModel? = null,
    val protagonist: String? = null,
    val project: String = "Default",
    val uid: String? = null,
    val audioLines: List<AudioLine> = emptyList(),
    val wordAlignments: List<WordAlignment> = emptyList(),
    val faqs: List<AudioQA> = List(10) { AudioQA(question = "", answer = "") },
    val socialMediaBlueprint: String?=null,
    val video: String? = null,
    val lyricTokens: List<String> = emptyList(),
) {

    fun lyrics(): String? = editedLyrics ?: lyrics

    companion object {
        fun emptyFaqs() = List(10) { AudioQA(question = "", answer = "") }

        fun buildLyricTokens(audios: List<Audio>): List<Audio> {
            return audios.map { audio ->
                val lyrics = audio.editedLyrics ?: audio.lyrics ?: return@map audio
                val tokens = mutableSetOf<String>()
                val lines = lyrics
                    .split("\n")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                for (line in lines) {
                    val words = line.split(Regex("\\s+"))
                    for (i in words.indices) {
                        tokens.add(words.subList(i, words.size).joinToString(" "))
                    }
                }
                audio.copy(lyricTokens = tokens.toList())
            }
        }
    }
}
fun Audio.getAlignedLines(): List<AudioTimestampedLine> {
    val words = this.wordAlignments
    if (words.isEmpty()) return emptyList()

    val lines = mutableListOf<AudioTimestampedLine>()
    var currentText = ""
    var currentStart: Double? = null
    var currentEnd: Double? = null

    words.forEach { word ->
        val text = word.text
        if (text == "\n") {
            if (currentText.isNotBlank()) {
                lines.add(
                    AudioTimestampedLine(
                        text = currentText.trim(),
                        start = (currentStart ?: 0.0),
                        end = (currentEnd ?: 0.0)
                    )
                )
            }
            currentText = ""
            currentStart = null
            currentEnd = null
        } else {
            currentText += text
            currentText += " "
            if (currentStart == null) currentStart = word.start
            currentEnd = word.end
        }
    }

    if (currentText.isNotBlank()) {
        lines.add(
            AudioTimestampedLine(
                text = currentText.trim(),
                start = (currentStart ?: 0.0),
                end = (currentEnd ?: 0.0)
            )
        )
    }
    return lines
}

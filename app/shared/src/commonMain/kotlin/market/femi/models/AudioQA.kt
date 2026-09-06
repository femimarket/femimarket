package market.femi.models

import kotlinx.serialization.Serializable

@Serializable
data class AudioQA(
    val question: String,
    val answer: String? = null,
)



val List<AudioQA>.has10Questions get() = count { it.question.isNotBlank() } == 10
val List<AudioQA>.answeredCount get() = count { !it.answer.isNullOrBlank() }
val List<AudioQA>.allAnswered get() = has10Questions && all { !it.answer.isNullOrBlank() }

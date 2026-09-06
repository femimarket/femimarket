package market.femi.models

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import kotlinx.serialization.Serializable

@Serializable
data class Faqs(
    val qas: List<AudioQA> = List(10) { AudioQA(question = "", answer = "") },
) {
    val has10Questions by derivedStateOf { qas.count { it.question.isNotBlank() } == 10 }
    val answeredCount by derivedStateOf { qas.count { !it.answer.isNullOrBlank() } }
    val allAnswered by derivedStateOf { has10Questions && qas.all { !it.answer.isNullOrBlank() } }
//    val hasAboutText by derivedStateOf { !about.isNullOrBlank() }
//    val needsToGenerate by derivedStateOf { allAnswered && !hasAboutText }

}

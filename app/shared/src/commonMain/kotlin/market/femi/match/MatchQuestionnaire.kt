package market.femi.match

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import kotlinx.coroutines.Job
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import market.femi.ActiveShowState
import market.femi.State
import market.femi.match.models.SessionAnswerCreate
import market.femi.match.models.SessionAnswerGet
import market.femi.services.LogService
import market.femi.services.createRealLogService

class MatchQuestionnaireState(private val state: State) : ActiveShowState() {
    private val log: LogService = createRealLogService("MatchQuestionnaireState")
    var lang by mutableStateOf("pt")
    var firstName by mutableStateOf("")
    var lastName by mutableStateOf("")
    var linkedin by mutableStateOf("")
    var sessionAnswers by mutableStateOf(listOf<SessionAnswerGet>())
    val answers = mutableStateListOf<String>()

    val complete get() = firstName.isNotBlank() && lastName.isNotBlank() &&
        linkedin.isNotBlank() && answers.isNotEmpty() && answers.all { it.isNotBlank() }

    suspend fun getQuestionnaire(): JsonElement {
        val questionnaire = state.match.getQuestionnaire(state.matchApp.questionnaire!!.id)
        if (answers.size != questionnaire.jsonObject["questions"]!!.jsonArray.size) {
            answers.clear()
            questionnaire.jsonObject["questions"]!!.jsonArray.forEach { answers.add("") }
        }
        return questionnaire
    }

    suspend fun getSessionAnswers() {
        sessionAnswers = state.match.getSessionAnswers(state.matchApp.sessionId)
    }

    fun submit(questionnaire: JsonElement): Job = working(state.scope, log, ::submit.name, requireShown = false) {
        state.match.createSessionAnswers(
            state.matchApp.sessionId,
            questionnaire.jsonObject["questions"]!!.jsonArray.mapIndexed { index, question ->
                SessionAnswerCreate(
                    questionId = question.jsonObject["id"]!!.jsonPrimitive.int,
                    answer = answers[index],
                )
            },
        )
        state.match.createApplicant(
            state.matchApp.sessionId,
            firstName,
            lastName,
            linkedin,
        )
        getSessionAnswers()
    }
}

@Composable
fun MatchQuestionnaire(state: State, questionnaire: JsonElement) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND.dp)
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Kotlin Frontend Lead",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = if (state.matchApp.questions.lang == "pt") {
                "Kotlin Multiplatform · Compose Material 3 · iOS, Android, JS e WASM a partir de um único código. " +
                    "Onze perguntas. Responda pelas suas próprias palavras. A entrevista é uma conversa sobre as suas respostas."
            } else {
                "Kotlin Multiplatform · Compose Material 3 · iOS, Android, JS and WASM from one codebase. " +
                    "Eleven questions. Answer in your own words. The interview is a conversation about your answers."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = state.matchApp.questions.firstName,
            onValueChange = { state.matchApp.questions.firstName = it },
            label = { Text(if (state.matchApp.questions.lang == "pt") "Nome" else "First name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.matchApp.questions.lastName,
            onValueChange = { state.matchApp.questions.lastName = it },
            label = { Text(if (state.matchApp.questions.lang == "pt") "Apelido" else "Last name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.matchApp.questions.linkedin,
            onValueChange = { state.matchApp.questions.linkedin = it },
            label = { Text(if (state.matchApp.questions.lang == "pt") "URL do perfil LinkedIn" else "LinkedIn profile URL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        questionnaire.jsonObject["questions"]!!.jsonArray.forEachIndexed { index, question ->
            (question.jsonObject["pretext_label"] as? JsonObject)?.let { pretext ->
                Text(
                    text = pretext["translations"]!!.jsonArray.first {
                        it.jsonObject["lang_id"]!!.jsonPrimitive.content == state.matchApp.questions.lang
                    }.jsonObject["text"]!!.jsonPrimitive.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            Text(
                text = "${index + 1}. " + question.jsonObject["question_label"]!!.jsonObject["translations"]!!.jsonArray.first {
                    it.jsonObject["lang_id"]!!.jsonPrimitive.content == state.matchApp.questions.lang
                }.jsonObject["text"]!!.jsonPrimitive.content,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp),
            )
            question.jsonObject["code_snippet"]!!.jsonPrimitive.contentOrNull?.let { code ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                ) {
                    Text(
                        text = code,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(14.dp),
                    )
                }
            }
            OutlinedTextField(
                value = state.matchApp.questions.answers[index],
                onValueChange = { state.matchApp.questions.answers[index] = it },
                label = { Text(if (state.matchApp.questions.lang == "pt") "A sua resposta" else "Your answer") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (state.matchApp.questions.err.isNotEmpty()) {
            Text(
                text = if (state.matchApp.questions.lang == "pt") {
                    "Não foi possível enviar. Tente novamente."
                } else {
                    "Could not submit. Try again."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Button(
            onClick = { state.matchApp.questions.submit(questionnaire) },
            enabled = state.matchApp.questions.complete && !state.matchApp.questions.isWorking,
        ) {
            if (state.matchApp.questions.isWorking) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(if (state.matchApp.questions.lang == "pt") "Enviar candidatura" else "Submit application")
            }
        }
    }
}

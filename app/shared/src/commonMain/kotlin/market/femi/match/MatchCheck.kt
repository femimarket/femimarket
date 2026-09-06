package market.femi.match

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import kotlinx.coroutines.Job
import market.femi.ActiveShowState
import market.femi.State
import market.femi.services.LogService
import market.femi.services.createRealLogService

class MatchCheckState(private val state: State) : ActiveShowState() {
    private val log: LogService = createRealLogService("MatchCheckState")

    fun check(): Job = working(state.scope, log, ::check.name, requireShown = false) {
        if (state.matchApp.sessionId.isEmpty()) {
            state.matchApp.createSession()
        }
        if (state.matchApp.questionnaire == null) {
            handleList()
        } else {
            handleQuestionnaire()
        }
    }

    private suspend fun handleList() {
        state.matchApp.getQuestionnaires()
        state.nav.openMatchQuestionnaireList()
    }

    private suspend fun handleQuestionnaire() {
        val questionnaire = state.matchApp.questions.getQuestionnaire()
        state.matchApp.questions.getSessionAnswers()
        state.nav.openMatchQuestionnaire(questionnaire)
    }
}

@Composable
fun MatchCheck(state: State) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND.dp)
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state.matchApp.check.err.isNotEmpty()) {
                Text(
                    text = if (state.matchApp.questions.lang == "pt") {
                        "Não foi possível carregar as perguntas."
                    } else {
                        "Could not load the questions."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = { state.matchApp.check.check() }) {
                    Text(if (state.matchApp.questions.lang == "pt") "Tentar novamente" else "Try again")
                }
            } else {
                CircularProgressIndicator()
            }
        }
    }
}

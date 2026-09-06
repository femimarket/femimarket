package market.femi.carer.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import market.femi.State

private val questions = listOf(
    "A client refuses their morning medication. Walk us through what you do.",
    "You arrive and the hoist looks like it has been left damaged. What happens next?",
    "A family member asks you something about another client. How do you answer?",
)

@Composable
fun CareVideo(state: State) {
    val answered = remember { mutableStateOf(0) }
    CareStep(
        say = "We do not interview.",
        sub = "Record these three answers whenever suits you, as many times as you like. " +
            "The first time you meet us is your first shift.",
        stepNumber = 0,
        stepsTotal = 0,
        action = "Done",
        onNext = { state.nav.goBack() },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            questions.forEachIndexed { index, question ->
                Text(
                    text = "${index + 1}. $question",
                    style = MaterialTheme.typography.bodyLarge,
                )
                RegisterUpload("video answer ${index + 1}", answered.value > index) {
                    answered.value = if (answered.value > index) index else index + 1
                }
            }
        }
    }
}

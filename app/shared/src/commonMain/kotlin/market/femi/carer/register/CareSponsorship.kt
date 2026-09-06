package market.femi.carer.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import market.femi.State

@Composable
fun CareSponsorship(state: State) {
        var chosen by remember { mutableStateOf("") }
    CareStep(
        say = "Are you in the UK on a Health and Care Worker visa?",
        sub = "Switching sponsors means you already have a share code.",
        stepNumber = 4,
        stepsTotal = 5,
        action = "Continue",
        onNext = {
            when (chosen) {
                "Yes, switching sponsors" -> state.nav.openCareShareCode()
                "No, I am overseas" -> state.nav.openCareOverseas()
            }
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CareChoices(
                options = listOf("Yes, switching sponsors", "No, I am overseas"),
                chosen = chosen,
                onChoose = { chosen = it },
            )
        }
    }
}

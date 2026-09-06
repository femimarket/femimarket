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
fun CareRightToWork(state: State) {
        var chosen by remember { mutableStateOf("") }
    CareStep(
        say = "Do you have the right to work in the UK?",
        sub = "We have to check this before anything else.",
        stepNumber = 3,
        stepsTotal = 5,
        action = "Continue",
        onNext = {
            when (chosen) {
                "British or Irish citizen" -> state.nav.openCarePassword()
                "Settled or pre-settled status" -> state.nav.openCareShareCode()
                "I need sponsorship" -> state.nav.openCareSponsorship()
            }
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CareChoices(
                options = listOf("British or Irish citizen", "Settled or pre-settled status", "I need sponsorship"),
                chosen = chosen,
                onChoose = { chosen = it },
            )
        }
    }
}

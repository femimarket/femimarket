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

private val slots = listOf(
    "Tue 25 Aug, 08:00 · Arthur Beckwith, Ashford",
    "Wed 26 Aug, 12:30 · Norah Pickering, Ashford",
    "Thu 27 Aug, 08:00 · Baldev Sandhu, Staines",
)

@Composable
fun CareAssessment(state: State) {
    var chosen by remember { mutableStateOf("") }
    CareStep(
        say = "Pick your first shift.",
        sub = "A real visit with a coordinator alongside you, paid like any other. " +
            "Your slot is held for an hour while you finish your checklist.",
        stepNumber = 5,
        stepsTotal = 5,
        action = "That one",
        onNext = { state.nav.openCareChecklist() },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CareChoices(options = slots, chosen = chosen, onChoose = { chosen = it })
        }
    }
}

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
fun CareHealth(state: State) {
        var chosen by remember { mutableStateOf("") }
        var health by remember { mutableStateOf("") }
        var emergencyName by remember { mutableStateOf("") }
        var emergencyMobile by remember { mutableStateOf("") }
    CareStep(
        say = "Anything we should know about your health?",
        sub = "We ask so we can make adjustments, and because we have to record fitness for the role.",
        stepNumber = 0,
        stepsTotal = 0,
        action = "Done",
        onNext = { state.nav.goBack() },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CareChoices(
                options = listOf("Nothing to declare", "Something to tell you"),
                chosen = chosen,
                onChoose = { chosen = it },
            )
            CareField("Conditions or adjustments, if any", health) { health = it }
            CareField("Emergency contact name", emergencyName) { emergencyName = it }
            CareField("Emergency contact mobile", emergencyMobile) { emergencyMobile = it }
        }
    }
}

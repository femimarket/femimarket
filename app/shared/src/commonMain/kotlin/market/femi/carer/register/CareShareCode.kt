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
fun CareShareCode(state: State) {
        var share by remember { mutableStateOf("") }
        var dateOfBirth by remember { mutableStateOf("") }
    CareStep(
        say = "What is your share code?",
        sub = "The code from GOV.UK, with your date of birth. We check it while you book.",
        stepNumber = 4,
        stepsTotal = 5,
        action = "Continue",
        onNext = { state.nav.openCarePassword() },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CareField("Share code", share) { share = it }
            CareField("Date of birth", dateOfBirth) { dateOfBirth = it }
        }
    }
}

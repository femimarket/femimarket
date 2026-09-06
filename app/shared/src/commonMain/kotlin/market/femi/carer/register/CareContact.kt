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
fun CareContact(state: State) {
        var mobile by remember { mutableStateOf("") }
    CareStep(
        say = "And a mobile number.",
        sub = "So the office can reach you before your account is set up.",
        stepNumber = 2,
        stepsTotal = 5,
        action = "Continue",
        onNext = { state.nav.openCareRightToWork() },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CareField("Mobile number", mobile) { mobile = it }
        }
    }
}

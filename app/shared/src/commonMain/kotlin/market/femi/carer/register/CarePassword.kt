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
import kotlin.random.Random

@Composable
fun CarePassword(state: State) {
    var password by remember { mutableStateOf("") }
    CareStep(
        say = "Pick a password.",
        sub = "This is how you get your rota, your messages and your assessment details. " +
            "Your username will be @aramide:femi.market.",
        stepNumber = 5,
        stepsTotal = 5,
        action = "Create my account",
        onNext = {
            if (Random.nextBoolean()) state.nav.openCareAssessment()
            else state.nav.openCareNoSlots()
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CareField("Password", password) { password = it }
            Text(
                text = "Everything you have filled in so far is on this device. " +
                    "Creating the account is what sends it to us.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

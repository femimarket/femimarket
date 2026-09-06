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
fun CareTrading(state: State) {
        var chosen by remember { mutableStateOf("") }
        var utr by remember { mutableStateOf("") }
        var ni by remember { mutableStateOf("") }
    CareStep(
        say = "We engage self-employed sole traders.",
        sub = "If you are not registered yet it is a ten-minute job on the HMRC website, and it is free.",
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
                options = listOf("Registered", "Not yet"),
                chosen = chosen,
                onChoose = { chosen = it },
            )
            CareField("UTR (ten digits)", utr) { utr = it }
            CareField("National Insurance number", ni) { ni = it }
        }
    }
}

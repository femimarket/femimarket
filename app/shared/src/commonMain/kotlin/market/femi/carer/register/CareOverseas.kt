package market.femi.carer.register

import androidx.compose.runtime.Composable
import market.femi.State

@Composable
fun CareOverseas(state: State) {
    CareStep(
        say = "We cannot sponsor from overseas right now",
        sub = "Only people already allowed to work in the UK can join today.",
        stepNumber = 4,
        stepsTotal = 5,
        action = "Back",
        onNext = { state.nav.goBack() },
    ) {
    }
}

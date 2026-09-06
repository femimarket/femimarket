package market.femi.carer.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import market.femi.State

@Composable
fun CareDone(state: State) {
    CareStep(
        say = "Your first shift is booked."        ,
        sub = "We check your documents and references before the day. Everything after this comes to you here.",
        stepNumber = 16,
        stepsTotal = 16,
        action = "Back to shifts",
        onNext = { state.nav.goBack() },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Before the day", style = MaterialTheme.typography.titleMedium, color = ink)
            Text("0 of 7 checks done - finish them here and the slot is yours.", style = MaterialTheme.typography.bodyMedium, color = dim)
            Text("Finish early and you may be moved to a sooner shift.", style = MaterialTheme.typography.bodyMedium, color = dim)
            Text("Everything after this comes to you in the app.", style = MaterialTheme.typography.bodyMedium, color = dim)
        }
    }
}

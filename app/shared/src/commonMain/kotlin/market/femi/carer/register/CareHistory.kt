package market.femi.carer.register

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import market.femi.State

@Composable
fun CareHistory(state: State) {
    var entries by remember { mutableIntStateOf(1) }
    CareStep(
        say = "Your work history.",
        sub = "Every role, most recent first, and a line about any gap before it. We have to hold this on file.",
        stepNumber = 0,
        stepsTotal = 0,
        action = "Done",
        onNext = { state.nav.goBack() },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            repeat(entries) { index ->
                HistoryRole(index + 1)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(panel)
                    .pointerInput(Unit) { detectTapGestures { entries += 1 } }
                    .padding(20.dp),
            ) {
                Text(
                    text = "Add another role",
                    style = MaterialTheme.typography.titleMedium,
                    color = accent,
                )
            }
        }
    }
}

@Composable
private fun HistoryRole(number: Int) {
    var employer by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var fromDate by remember { mutableStateOf("") }
    var toDate by remember { mutableStateOf("") }
    var gapBefore by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Role $number", style = MaterialTheme.typography.titleMedium)
        CareField("Employer", employer) { employer = it }
        CareField("Role", role) { role = it }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                CareField("From", fromDate) { fromDate = it }
            }
            Column(modifier = Modifier.weight(1f)) {
                CareField("To", toDate) { toDate = it }
            }
        }
        CareField("Gap before this role, if any", gapBefore) { gapBefore = it }
    }
}

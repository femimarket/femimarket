package market.femi.carer.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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

@Composable
fun CareReferences(state: State) {
    CareStep(
        say = "Two referees.",
        sub = "If you have worked in care, one must be your most recent care employer. We contact them, not you.",
        stepNumber = 0,
        stepsTotal = 0,
        action = "Done",
        onNext = { state.nav.goBack() },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Referee("First referee")
            Referee("Second referee")
        }
    }
}

@Composable
private fun Referee(which: String) {
    var name by remember { mutableStateOf("") }
    var organisation by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var fromDate by remember { mutableStateOf("") }
    var toDate by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(which, style = MaterialTheme.typography.titleMedium)
        CareField("Name", name) { name = it }
        CareField("Where they worked with you", organisation) { organisation = it }
        CareField("Their role there", role) { role = it }
        CareField("What they were to you", relationship) { relationship = it }
        CareField("Email", email) { email = it }
        CareField("Mobile", mobile) { mobile = it }
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
    }
}

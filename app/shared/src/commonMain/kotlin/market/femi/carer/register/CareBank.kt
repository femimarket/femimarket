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
fun CareBank(state: State) {
        var bank by remember { mutableStateOf("") }
        var sort by remember { mutableStateOf("") }
        var account by remember { mutableStateOf("") }
    CareStep(
        say = "Where should we pay you?",
        sub = "Sort code is six digits, account number is eight.",
        stepNumber = 0,
        stepsTotal = 0,
        action = "Done",
        onNext = { state.nav.goBack() },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CareField("Bank name", bank) { bank = it }
            CareField("Sort code", sort) { sort = it }
            CareField("Account number", account) { account = it }
        }
    }
}

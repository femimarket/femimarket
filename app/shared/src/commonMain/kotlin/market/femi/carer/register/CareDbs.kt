package market.femi.carer.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import market.femi.State

@Composable
fun CareDbs(state: State) {
        var chosen by remember { mutableStateOf("") }
        var number by remember { mutableStateOf("") }
        var issued by remember { mutableStateOf("") }
        var dateOfBirth by remember { mutableStateOf("") }
        var paid by remember { mutableStateOf(false) }
    CareStep(
        say = "Where are you with DBS?",
        sub = "Enhanced with the adult barred list is the one care needs.",
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
                options = listOf("On the update service", "I have a certificate", "I don't have one"),
                chosen = chosen,
                onChoose = { chosen = it },
            )
            when (chosen) {
                "On the update service" -> {
                    CareField("DBS certificate number", number) { number = it }
                    CareField("Date of birth", dateOfBirth) { dateOfBirth = it }
                }
                "I have a certificate" -> {
                    CareField("DBS certificate number", number) { number = it }
                    CareField("Date issued", issued) { issued = it }
                    CareField("Date of birth", dateOfBirth) { dateOfBirth = it }
                }
                "I don't have one" -> {
                    if (paid) {
                        Text(
                            text = "With the office. We apply for you and tell you the moment it clears - " +
                                "today's slot will lapse, and you book again then with everything else already done.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    } else {
                        Text(
                            text = "You need an enhanced DBS, and only we can apply for it - individuals cannot. " +
                                "It is £55, paid by you, and clearance takes about two weeks, " +
                                "so today's slot will lapse. You book again the moment it clears.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        CareField("Date of birth", dateOfBirth) { dateOfBirth = it }
                        Text(
                            text = "Pay with your reference: 4251ec2c",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        OutlinedButton(
                            onClick = {},
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Pay £55 by Wise")
                        }
                        TextButton(onClick = { paid = true }) {
                            Text("I have paid")
                        }
                    }
                }
            }
        }
    }
}

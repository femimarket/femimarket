package market.femi.carer.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import market.femi.State


class CareNamesState(private val state: State) {

    fun create(){

    }
}

@Composable
fun CareNames(state: State) {
        var first by remember { mutableStateOf("") }
        var last by remember { mutableStateOf("") }
    CareStep(
        say = "What should we call you?",
        sub = "",
        stepNumber = 1,
        stepsTotal = 5,
        action = "Continue",
        onNext = { state.nav.openCareContact() },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = buildAnnotatedString {
                    append("Your name exactly as it appears on your passport - ")
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                        append("it cannot be changed later.")
                    }
                },
                style = MaterialTheme.typography.bodyLarge,
                color = dim,
            )
            OutlinedTextField(
                value = first,
                onValueChange = { first = it },
                label = { Text("First name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = last,
                onValueChange = { last = it },
                label = { Text("Surname") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

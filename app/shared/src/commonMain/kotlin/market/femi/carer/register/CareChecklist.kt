package market.femi.carer.register

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import market.femi.State

@Composable
fun CareChecklist(state: State) {
    var remaining by remember { mutableStateOf(60 * 60) }
    LaunchedEffect(Unit) {
        while (remaining > 0) {
            delay(1000)
            remaining -= 1
        }
    }
    CareStep(
        say = "Your slot is held.",
        sub = "Finish this list within the hour to keep it, in any order.",
        stepNumber = 0,
        stepsTotal = 0,
        action = "Close",
        onNext = { state.nav.goBack() },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ListItem(
                headlineContent = {
                    Text(if (remaining > 0) "Slot held for" else "Hold ended")
                },
                trailingContent = {
                    Text(
                        text = if (remaining > 0) {
                            "${remaining / 60}:${(remaining % 60).toString().padStart(2, '0')}"
                        } else {
                            "slot released"
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.clip(RoundedCornerShape(10.dp)),
            )
            ChecklistRow("ID documents") { state.nav.openCareDocuments() }
            ChecklistRow("DBS") { state.nav.openCareDbs() }
            ChecklistRow("Employment history") { state.nav.openCareHistory() }
            ChecklistRow("References") { state.nav.openCareReferences() }
            ChecklistRow("Bank details") { state.nav.openCareBank() }
            ChecklistRow("Self-employed status") { state.nav.openCareTrading() }
            ChecklistRow("Health") { state.nav.openCareHealth() }
            ChecklistRow("Equality") { state.nav.openCareEquality() }
            ChecklistRow("Intro video") { state.nav.openCareVideo() }
        }
    }
}

@Composable
private fun ChecklistRow(name: String, onOpen: () -> Unit) {
    ListItem(
        headlineContent = { Text(name) },
        trailingContent = {
            Text(
                text = "To do",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onOpen() },
    )
}

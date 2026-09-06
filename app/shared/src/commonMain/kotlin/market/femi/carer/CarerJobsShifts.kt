package market.femi.carer

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import market.femi.State

data class JobShift(
    val id: String,
    val kind: String,
    val start: String,
    val end: String,
    val minutes: Int,
    val carers: Int,
    val postcode: String,
    val town: String,
    val travelMins: Int,
    val needs: List<String>,
)

private const val JOB_CARD_WIDTH = 264


private val offered = listOf(
    JobShift("s01", "Morning call", "07:00", "07:45", 45, 2, "TW15 1RB", "Ashford", 12, listOf()),
    JobShift("s02", "Morning call", "08:00", "08:45", 45, 1, "TW17 8TB", "Shepperton", 18, listOf()),
    JobShift("s03", "Morning double", "08:00", "10:00", 120, 2, "TW15 3DB", "Ashford", 12, listOf("Hoist")),
    JobShift("s04", "Long shift", "08:00", "13:00", 300, 1, "KT13 8BX", "Weybridge", 34, listOf("Dementia")),
    JobShift("s05", "Lunch call", "12:30", "13:00", 30, 2, "TW15 1RB", "Ashford", 12, listOf()),
    JobShift("s06", "Lunch call", "13:00", "14:00", 60, 1, "TW18 4QB", "Staines", 21, listOf("Medication")),
    JobShift("s07", "Tea call", "15:30", "16:00", 30, 2, "TW17 8EB", "Shepperton", 19, listOf("Hoist")),
    JobShift("s08", "Evening call", "17:30", "18:30", 60, 2, "TW15 3DB", "Ashford", 12, listOf()),
    JobShift("s09", "Evening call", "19:30", "20:00", 30, 1, "TW15 1RB", "Ashford", 12, listOf("PEG feeding")),
)

private val trained = listOf("Medication")

private fun payFor(minutes: Int): String {
    val pence = minutes * CARE_RATE * 100 / 60
    return "£${pence / 100}.${(pence % 100).toString().padStart(2, '0')}"
}

private fun lengthOf(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60}h"
    else -> "${minutes / 60}h ${minutes % 60}m"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CarerJobsShifts(state: State) {
    val chosen = remember { mutableStateListOf<String>() }
    val picked = offered.filter { chosen.contains(it.id) }
    val minutes = picked.sumOf { it.minutes }
    val claims = picked.flatMap { it.needs }.toSet().filterNot { trained.contains(it) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND.dp)
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Your shifts",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "${offered.size} inside the hours you set, sorted by how far they are from you. " +
                "£$CARE_RATE an hour on every one. Your first is with a coordinator.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
            offered.forEach { shift ->
                JobCard(
                    shift = shift,
                    taken = chosen.contains(shift.id),
                    onToggle = {
                        if (chosen.contains(shift.id)) chosen.remove(shift.id)
                        else chosen.add(shift.id)
                    },
                )
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = if (picked.isEmpty()) "Nothing chosen yet"
                else "${picked.size} shifts you want · ${lengthOf(minutes)} · ${payFor(minutes)} a week",
                style = MaterialTheme.typography.titleMedium,
                color = if (picked.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            if (claims.isNotEmpty()) {
                Text(
                    text = "We will check you are signed off for ${claims.joinToString(", ")} " +
                        "at your assessment.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Button(
                onClick = { state.nav.openCareJobsExperience() },
                enabled = picked.isNotEmpty(),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Text("Continue with these")
            }
        }
        }
        Text(
            text = "Ten minutes of paperwork here and you are booked in. " +
                "The first time you meet us is your assessment.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 20.dp),
        )
    }
}

@Composable
private fun JobCard(
    shift: JobShift,
    taken: Boolean,
    onToggle: () -> Unit,
) {
    val unmet = shift.needs.filterNot { trained.contains(it) }
    Surface(
        modifier = Modifier
            .padding(4.dp)
            .width(JOB_CARD_WIDTH.dp)
            .pointerInput(shift.id) { detectTapGestures { onToggle() } },
        shape = RoundedCornerShape(12.dp),
        color = if (taken) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
    Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = shift.kind,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = payFor(shift.minutes),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            text = "${shift.start} – ${shift.end} · ${lengthOf(shift.minutes)}",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "${shift.postcode} · ${shift.town} · ${shift.travelMins} min away",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (shift.needs.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                shift.needs.forEach { need ->
                    Text(
                        text = need,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (trained.contains(need)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (trained.contains(need)) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (shift.carers > 1) "Two carers" else "On your own",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (taken) "Chosen" else "I want this",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (unmet.isNotEmpty() && taken) {
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
            Text(
                text = "Checked at assessment",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    }
}

package market.femi.carer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import market.femi.State

data class AvailabilityEntry(
    val span: String,
    val window: String,
    val note: String,
    val approved: Boolean,
)

val availabilityEntries = mutableStateListOf(
    AvailabilityEntry("25 Aug - 30 Aug", "06:00 - 20:00", "weekly pattern, Mondays off", true),
    AvailabilityEntry("1 Sep - 27 Dec", "06:00 - 20:00", "weekly pattern, Mondays off", true),
    AvailabilityEntry("7 Sep - 11 Sep", "06:00 - 20:00", "family visit", false),
)

@Composable
fun CarerAvailability(state: State) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND.dp)
                .fillMaxWidth(),
        ) {
            Text(
                text = "Your availability",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(24.dp),
            )
            ListItem(
                headlineContent = { Text("New availability") },
                leadingContent = {
                    Icon(Icons.Filled.Add, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { state.nav.openCareAvailabilityNew() },
            )
            HorizontalDivider()
            availabilityEntries.forEachIndexed { index, entry ->
                ListItem(
                    headlineContent = { Text(entry.span) },
                    supportingContent = { Text(entry.note) },
                    trailingContent = {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = entry.window,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = if (entry.approved) "Approved" else "Waiting",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (entry.approved) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.tertiary,
                            )
                        }
                    },
                )
                if (index != availabilityEntries.lastIndex) {
                    HorizontalDivider()
                }
            }
        }
    }
}

fun clock(hour: Int, minute: Int): String =
    "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

fun dayLabel(millis: Long): String {
    val days = millis / 86_400_000L
    val z = days + 719_468L
    val era = z / 146_097L
    val doe = z - era * 146_097L
    val yoe = (doe - doe / 1_460L + doe / 36_524L - doe / 146_096L) / 365L
    val doy = doe - (365L * yoe + yoe / 4L - yoe / 100L)
    val mp = (5L * doy + 2L) / 153L
    val d = doy - (153L * mp + 2L) / 5L + 1L
    val m = if (mp < 10L) mp + 3L else mp - 9L
    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    return "$d ${months[(m - 1L).toInt()]}"
}

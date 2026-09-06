package market.femi.carer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import market.femi.State

const val PUBLISHED_THROUGH = 1_787_616_000_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarerAvailabilityNew(state: State) {
    val picker = rememberDatePickerState(
        initialDisplayedMonthMillis = PUBLISHED_THROUGH,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                utcTimeMillis >= PUBLISHED_THROUGH
        },
    )
    var startMillis by remember { mutableStateOf<Long?>(null) }
    var endMillis by remember { mutableStateOf<Long?>(null) }
    var lastHandled by remember { mutableStateOf<Long?>(null) }
    val startTime = rememberTimePickerState(initialHour = 6, initialMinute = 0, is24Hour = true)
    val endTime = rememberTimePickerState(initialHour = 20, initialMinute = 0, is24Hour = true)
    var editingStart by remember { mutableStateOf(false) }
    var editingEnd by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    LaunchedEffect(picker.selectedDateMillis) {
        val sel = picker.selectedDateMillis
        if (sel != null && sel != lastHandled) {
            lastHandled = sel
            val start = startMillis
            when {
                start == null -> startMillis = sel
                endMillis == null -> {
                    startMillis = minOf(start, sel)
                    endMillis = maxOf(start, sel)
                }
                else -> {
                    startMillis = sel
                    endMillis = null
                }
            }
        }
    }
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
                text = "New availability",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(24.dp),
            )
            DatePicker(
                state = picker,
                title = null,
                headline = null,
                showModeToggle = false,
                modifier = Modifier.fillMaxWidth(),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val start = startMillis
                val end = endMillis
                Text(
                    text = when {
                        start == null -> "Pick your first day"
                        end == null -> "${dayLabel(start)} - pick your last day"
                        else -> "${dayLabel(start)} - ${dayLabel(end)}"
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
                OutlinedButton(
                    onClick = { editingStart = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("From ${clock(startTime.hour, startTime.minute)}")
                }
                OutlinedButton(
                    onClick = { editingEnd = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Until ${clock(endTime.hour, endTime.minute)}")
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = {
                        availabilityEntries.add(
                            AvailabilityEntry(
                                span = "${dayLabel(start ?: 0L)} - ${dayLabel(end ?: start ?: 0L)}",
                                window = "${clock(startTime.hour, startTime.minute)} - ${clock(endTime.hour, endTime.minute)}",
                                note = note,
                                approved = false,
                            ),
                        )
                        state.nav.goBack()
                    },
                    enabled = start != null && end != null,
                ) {
                    Text("Send for approval")
                }
            }
        }
    }
    if (editingStart) {
        TimePickerDialog(
            onDismissRequest = { editingStart = false },
            confirmButton = {
                TextButton(onClick = { editingStart = false }) { Text("Done") }
            },
            title = { Text("From") },
        ) {
            TimePicker(state = startTime)
        }
    }
    if (editingEnd) {
        TimePickerDialog(
            onDismissRequest = { editingEnd = false },
            confirmButton = {
                TextButton(onClick = { editingEnd = false }) { Text("Done") }
            },
            title = { Text("Until") },
        ) {
            TimePicker(state = endTime)
        }
    }
}

package market.femi.carer

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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

private val hourDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private val hourBands = listOf("Morning", "Lunch", "Tea", "Evening", "Night")


@Composable
fun CarerJobsHours(state: State) {
    val chosen = remember { mutableStateListOf<String>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND.dp)
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "When can you work?",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "Tap everything that suits you. We will only ever show you shifts inside these hours.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.width(74.dp))
                hourDays.forEach { day ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(day, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            hourBands.forEach { band ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = band,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.width(74.dp),
                    )
                    hourDays.forEach { day ->
                        val key = "$day $band"
                        val on = chosen.contains(key)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(2.dp)
                                .height(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest)
                                .pointerInput(key) {
                                    detectTapGestures {
                                        if (on) chosen.remove(key) else chosen.add(key)
                                    }
                                },
                        )
                    }
                }
            }
        }
            }
        Text(
            text = when {
                chosen.isEmpty() -> "Nothing picked yet."
                else -> "${chosen.size} slots a week — about ${chosen.size * 2} hours of work available to you."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (chosen.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Button(
            onClick = { state.nav.openCareNames() },
            enabled = chosen.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Text("Continue")
        }
    }
}

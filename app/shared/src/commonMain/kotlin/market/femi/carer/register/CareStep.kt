package market.femi.carer.register

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass

internal val bg = Color(0xFFFBFBFA)
internal val panel = Color(0xFFFFFFFF)
internal val line = Color(0xFFE6E4E0)
internal val ink = Color(0xFF1A1A19)
internal val dim = Color(0xFF8A8781)
internal val accent = Color(0xFF2F6F4E)
internal val accentSoft = Color(0xFFEAF3EE)

@Composable
internal fun CareStep(
    say: String,
    sub: String,
    stepNumber: Int,
    stepsTotal: Int,
    action: String,
    onNext: () -> Unit,
    body: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .verticalScroll(rememberScrollState())
            .wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND.dp)
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (stepNumber > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(line),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(stepNumber.toFloat() / stepsTotal)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(accent),
                )
            }
        }
        Text(say, style = MaterialTheme.typography.headlineSmall, color = ink)
        if (sub.isNotEmpty()) {
            Text(sub, style = MaterialTheme.typography.bodyLarge, color = dim)
        }
        body()
        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = accent),
        ) {
            Text(action)
        }
    }
}

@Composable
internal fun CareField(
    label: String,
    value: String,
    onValue: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CareChoices(
    options: List<String>,
    chosen: String,
    onChoose: (String) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = chosen == option,
                onClick = { onChoose(option) },
                label = { Text(option) },
            )
        }
    }
}

@Composable
internal fun RegisterUpload(
    what: String,
    picked: Boolean,
    onPick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (picked) accentSoft else panel)
            .pointerInput(what) { detectTapGestures { onPick() } }
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = if (picked) "$what added" else "Add $what",
            style = MaterialTheme.typography.titleMedium,
            color = if (picked) accent else ink,
        )
        Text(
            text = "A clear photo, all four corners, no glare.",
            style = MaterialTheme.typography.bodySmall,
            color = dim,
        )
    }
}

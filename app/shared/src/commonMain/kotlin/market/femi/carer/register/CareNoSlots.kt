package market.femi.carer.register

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import market.femi.State

@Composable
fun CareNoSlots(state: State) {
    var notify by remember { mutableStateOf(false) }
    CareStep(
        say = "All slots are taken right now.",
        sub = "New slots open every Monday, and they go fast.",
        stepNumber = 5,
        stepsTotal = 5,
        action = "Done",
        onNext = { state.nav.goBack() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (notify) accentSoft else panel)
                .pointerInput(Unit) { detectTapGestures { notify = !notify } }
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = if (notify) "We will tell you when a slot opens" else "Tell me when a slot opens",
                style = MaterialTheme.typography.titleMedium,
                color = if (notify) accent else ink,
            )
            Text(
                text = "One message, the moment a slot appears.",
                style = MaterialTheme.typography.bodySmall,
                color = dim,
            )
        }
    }
}

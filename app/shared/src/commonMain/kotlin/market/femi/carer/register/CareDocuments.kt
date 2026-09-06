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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import market.femi.State

@Composable
fun CareDocuments(state: State) {
    var passport by remember { mutableStateOf(false) }
    val certificates = remember { mutableStateListOf(false) }
    CareStep(
        say = "Your passport and any certificates.",
        sub = "The passport proves who you are. Certificates save you repeating training you have already done.",
        stepNumber = 0,
        stepsTotal = 0,
        action = "Done",
        onNext = { state.nav.goBack() },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RegisterUpload("passport", passport) { passport = !passport }
            certificates.forEachIndexed { index, picked ->
                RegisterUpload("certificate", picked) { certificates[index] = !picked }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(panel)
                    .pointerInput(Unit) { detectTapGestures { certificates.add(false) } }
                    .padding(20.dp),
            ) {
                Text(
                    text = "Add another certificate",
                    style = MaterialTheme.typography.titleMedium,
                    color = accent,
                )
            }
        }
    }
}

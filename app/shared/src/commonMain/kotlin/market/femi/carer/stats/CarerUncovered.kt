package market.femi.carer.stats

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import market.femi.State

data class FunnelStep(
    val remaining: Int,
    val rule: String,
    val removed: String,
)

data class UncoveredCall(
    val client: String,
    val time: String,
    val needs: Int,
    val funnel: List<FunnelStep>,
    val fix: String,
)

private val uncoveredCalls = listOf(
    UncoveredCall(
        client = "Sylvia Trenholm",
        time = "22:00 – 07:00",
        needs = 1,
        funnel = listOf(
            FunnelStep(18, "staffs", "everyone on the books"),
            FunnelStep(15, "transport_modes", "3 have no travel mode set"),
            FunnelStep(7, "availabilities", "8 not available today"),
            FunnelStep(0, "availabilities", "7 finish at 20:00, none work past it"),
        ),
        fix = "One carer's availability must extend past 22:00. No shift time or rule change can cover this call.",
    ),
)

private const val BAR_WIDTH = 260

private val paper = Color(0xFFFAF9F5)
private val card = Color(0xFFFFFFFF)
private val ink = Color(0xFF24312D)
private val muted = Color(0xFF5F6E67)
private val line = Color(0xFFE3E1D8)
private val accent = Color(0xFF0F6E56)
private val alarmFill = Color(0xFFFBEBEB)
private val alarmInk = Color(0xFF991F1F)

@Composable
fun CarerUncovered(state: State) {
    val page = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize().background(paper).verticalScroll(page).padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column {
                Text(
                    text = "back",
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                    modifier = Modifier.pointerInput(Unit) { detectTapGestures { state.nav.goBack() } },
                )
                Text("Uncovered", style = MaterialTheme.typography.headlineMedium, color = ink)
            }
            Text(
                text = "Wednesday 19 August",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = muted,
            )
        }
        uncoveredCalls.forEach { call ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(card)
                    .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(call.client, style = MaterialTheme.typography.titleLarge, color = ink)
                        Text(call.time, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace), color = muted)
                    }
                    Text(
                        text = "needs ${call.needs}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = alarmInk,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(alarmFill)
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                    )
                }
                call.funnel.forEach { step ->
                    val width = (BAR_WIDTH * step.remaining / call.funnel.first().remaining).coerceAtLeast(2)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = step.remaining.toString(),
                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold),
                            color = if (step.remaining == 0) alarmInk else ink,
                            modifier = Modifier.width(34.dp),
                        )
                        Box(
                            modifier = Modifier
                                .width(width.dp)
                                .height(16.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (step.remaining == 0) alarmFill else accent),
                        )
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(
                                text = step.removed,
                                style = MaterialTheme.typography.bodyMedium,
                                color = ink,
                                maxLines = 1,
                            )
                            Text(
                                text = step.rule,
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = muted,
                            )
                        }
                    }
                }
                Text(
                    text = call.fix,
                    style = MaterialTheme.typography.bodyMedium,
                    color = alarmInk,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(alarmFill)
                        .padding(12.dp),
                )
            }
        }
        Text(
            text = "Every other call today has a carer.",
            style = MaterialTheme.typography.bodyMedium,
            color = muted,
        )
    }
}

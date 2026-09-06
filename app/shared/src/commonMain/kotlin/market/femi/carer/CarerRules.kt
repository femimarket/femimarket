package market.femi.carer

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.window.core.layout.WindowSizeClass
import market.femi.State

data class RuleFact(
    val subject: String,
    val note: String,
)

data class RuleTile(
    val rule: String,
    val value: String,
    val severity: Int,
    val facts: List<RuleFact>,
)

private val ruleTiles = listOf(
    RuleTile("Uncovered", "1", 2, listOf(
        RuleFact("Sylvia Trenholm 22:00", "waking night, nobody past 20:00"))),
    RuleTile("Banned partners", "4", 0, listOf(
        RuleFact("Tunde + Marlon", "until 23 Aug"),
        RuleFact("Tunde + Anjali", "until 23 Aug"),
        RuleFact("Marlon + Priya", "until 23 Aug"),
        RuleFact("Tunde + Priya", "until 23 Aug"))),
    RuleTile("Whitelists", "3", 0, listOf(
        RuleFact("list 1", "5 carers, Beatrice's team"),
        RuleFact("list 2", "carers cleared for Baldev"),
        RuleFact("list 3", "carers cleared for Kamala"))),
    RuleTile("Blacklists", "8", 0, listOf(
        RuleFact("list 1", "untrained for solo visits"),
        RuleFact("list 2", "Pauline's solos, Marlon trained"),
        RuleFact("list 3", "Alice Pemberton's solos, Adaeze trained"),
        RuleFact("list 4", "Constance's solos, Marlon and Priya trained"),
        RuleFact("list 5", "Muriel's solos, Marlon and Priya trained"))),
    RuleTile("Max calls per carer", "0", 1, listOf(
        RuleFact("max_daily_hours_rules", "empty, no ceiling on anyone's day"))),
    RuleTile("Longest run", "6h30", 2, listOf(
        RuleFact("Grace", "6h30 unbroken"),
        RuleFact("Nasrin", "6h00 unbroken"),
        RuleFact("Folasade", "3h30 unbroken"),
        RuleFact("Marlon", "2h45 unbroken"),
        RuleFact("Priya", "1h50 unbroken"),
        RuleFact("Anjali", "1h45 unbroken"),
        RuleFact("Tunde", "1h00 unbroken"))),
    RuleTile("Highest hours", "11h15", 1, listOf(
        RuleFact("Grace", "11h15"),
        RuleFact("Nasrin", "9h30"),
        RuleFact("Folasade", "7h45"),
        RuleFact("Anjali", "6h30"),
        RuleFact("Priya", "6h30"),
        RuleFact("Marlon", "6h15"),
        RuleFact("Tunde", "3h30"))),
    RuleTile("Lowest hours", "3h30", 0, listOf(
        RuleFact("Tunde", "3h30"),
        RuleFact("Marlon", "6h15"),
        RuleFact("Priya", "6h30"),
        RuleFact("Anjali", "6h30"),
        RuleFact("Folasade", "7h45"),
        RuleFact("Nasrin", "9h30"),
        RuleFact("Grace", "11h15"))),
    RuleTile("Highest lateness", "+14m", 2, listOf(
        RuleFact("Reginald Askew 10:30", "Grace +14m"),
        RuleFact("Kamala Deol 12:30", "Folasade +9m"),
        RuleFact("Marjorie Ellerby 09:45", "Grace +8m"),
        RuleFact("Sylvia Trenholm 13:00", "Nasrin +7m"),
        RuleFact("Alice Pemberton 13:30", "Anjali +7m"),
        RuleFact("Reginald Askew 13:30", "Nasrin +7m"),
        RuleFact("Arthur Beckwith 19:30", "Nasrin +4m"),
        RuleFact("Norah Pickering 10:07", "Folasade +3m"))),
    RuleTile("Next expiry", "19 Aug", 2, listOf(
        RuleFact("Priya", "supervision ends 19 Aug"),
        RuleFact("Sunita", "availability ends 19 Aug"),
        RuleFact("Tunde + Marlon", "partner ban ends 23 Aug"),
        RuleFact("Tunde + Anjali", "partner ban ends 23 Aug"),
        RuleFact("Marlon + Priya", "partner ban ends 23 Aug"),
        RuleFact("Tunde", "doubles-only ends 23 Aug"),
        RuleFact("Adaeze", "doubles-only ends 23 Aug"))),
    RuleTile("Clients to call", "0", 0, listOf()),
    RuleTile("Taxis", "2", 2, listOf(
        RuleFact("Anjali", "Alice Pemberton to Harold Wetherby, 09:45"),
        RuleFact("Grace", "Marjorie Ellerby to Reginald Askew, 10:30"))),
)

private const val TILE_WIDTH = 170

private val paper = Color(0xFFFAF9F5)
private val card = Color(0xFFFFFFFF)
private val ink = Color(0xFF24312D)
private val muted = Color(0xFF5F6E67)
private val line = Color(0xFFE3E1D8)
private val accent = Color(0xFF0F6E56)
private val watchFill = Color(0xFFFDF0D8)
private val watchInk = Color(0xFF8A5A00)
private val alarmFill = Color(0xFFFBEBEB)
private val alarmInk = Color(0xFF991F1F)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CarerRules(state: State) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(paper)
            .verticalScroll(rememberScrollState())
            .wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND.dp)
            .fillMaxWidth()
            .padding(14.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
            Text("Rules · Wednesday 19 August", style = MaterialTheme.typography.titleLarge, color = ink)
            Text("44 calls · 7 carers · optimal", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = muted)
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            ruleTiles.forEach { tile ->
                    val fill = when (tile.severity) {
                        2 -> alarmFill
                        1 -> watchFill
                        else -> card
                    }
                    val tone = when (tile.severity) {
                        2 -> alarmInk
                        1 -> watchInk
                        else -> ink
                    }
                    Column(
                        modifier = Modifier
                            .width(TILE_WIDTH.dp)
                            .padding(3.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(fill)
                            .pointerInput(tile.rule) {
                                detectTapGestures {
                                    if (tile.rule == "Uncovered") state.nav.openCareUncovered()
                                }
                            }
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = tile.rule,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (tile.severity == 0) muted else tone,
                            maxLines = 1,
                        )
                        Text(
                            text = tile.value,
                            style = MaterialTheme.typography.headlineMedium,
                            color = tone,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                    }
                }
        }
    }
}

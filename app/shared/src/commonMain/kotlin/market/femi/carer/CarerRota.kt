package market.femi.carer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import market.femi.State

data class Assignment(
    val client: String,
    val startTime: String,
    val endTime: String,
    val isDouble: Boolean,
    val lateMins: Int,
    val taxiFrom: String?,
)

data class CarerDay(
    val carer: String,
    val assignments: List<Assignment>,
)

private val wednesday = listOf(
    CarerDay(
        "Folasade Adeyemi",
        listOf(
            Assignment("Baldev Sandhu", "08:00:00", "10:00:00", true, 0, null),
            Assignment("Norah Pickering", "10:07:00", "10:52:00", false, 3, null),
            Assignment("Edith Naylor", "11:00:00", "11:30:00", false, 0, null),
            Assignment("Arthur Beckwith", "12:00:00", "12:30:00", true, 1, null),
            Assignment("Kamala Deol", "12:30:00", "13:30:00", false, 9, "Arthur Beckwith"),
            Assignment("Norah Pickering", "13:30:00", "14:00:00", false, 1, null),
            Assignment("Norah Pickering", "16:30:00", "17:00:00", false, 0, null),
            Assignment("Baldev Sandhu", "17:30:00", "18:30:00", true, 0, null),
            Assignment("Kamala Deol", "19:00:00", "19:30:00", false, 0, null),
            Assignment("Norah Pickering", "19:30:00", "20:00:00", false, 1, null),
        ),
    ),
    CarerDay(
        "Grace Ogunleye",
        listOf(
            Assignment("Sylvia Trenholm", "07:00:00", "07:45:00", true, 0, null),
            Assignment("Rosemary Dunnett", "08:00:00", "08:45:00", true, 0, null),
            Assignment("Alice Pemberton", "08:45:00", "09:45:00", false, 0, null),
            Assignment("Marjorie Ellerby", "09:45:00", "10:30:00", true, 8, null),
            Assignment("Reginald Askew", "10:30:00", "11:30:00", true, 14, null),
            Assignment("Winifred 'Muriel' Fotheringay", "11:45:00", "12:15:00", false, 0, null),
            Assignment("Beatrice Ramnarine", "13:30:00", "20:00:00", false, 0, null),
        ),
    ),
    CarerDay(
        "Nasrin Begum",
        listOf(
            Assignment("Beatrice Ramnarine", "08:00:00", "13:00:00", false, 0, null),
            Assignment("Sylvia Trenholm", "13:00:00", "13:30:00", true, 7, null),
            Assignment("Reginald Askew", "13:30:00", "14:00:00", true, 7, null),
            Assignment("Marjorie Ellerby", "15:12:00", "15:42:00", true, 0, null),
            Assignment("Sylvia Trenholm", "15:50:00", "16:20:00", true, 0, null),
            Assignment("Edith Naylor", "16:39:00", "17:09:00", false, 0, null),
            Assignment("Reginald Askew", "17:30:00", "18:00:00", true, 0, null),
            Assignment("Marjorie Ellerby", "18:12:00", "18:42:00", true, 0, null),
            Assignment("Sylvia Trenholm", "18:50:00", "19:20:00", true, 0, null),
            Assignment("Arthur Beckwith", "19:30:00", "20:00:00", true, 4, null),
        ),
    ),
    CarerDay(
        "Tunde Ajayi",
        listOf(
            Assignment("Rosemary Dunnett", "08:00:00", "08:45:00", true, 0, null),
            Assignment("Marjorie Ellerby", "09:45:00", "10:30:00", true, 0, null),
            Assignment("Marjorie Ellerby", "15:12:00", "15:42:00", true, 0, null),
            Assignment("Baldev Sandhu", "17:30:00", "18:30:00", true, 0, null),
            Assignment("Arthur Beckwith", "19:30:00", "20:00:00", true, 0, null),
        ),
    ),
    CarerDay(
        "Marlon Cheng",
        listOf(
            Assignment("Sylvia Trenholm", "07:00:00", "07:45:00", true, 0, null),
            Assignment("Pauline Draycott", "07:58:00", "08:43:00", false, 0, null),
            Assignment("Arthur Beckwith", "09:00:00", "09:45:00", true, 0, null),
            Assignment("Muriel Frances Hartley", "11:00:00", "11:30:00", false, 0, null),
            Assignment("Arthur Beckwith", "12:00:00", "12:30:00", true, 0, null),
            Assignment("Sylvia Trenholm", "13:00:00", "13:30:00", true, 0, null),
            Assignment("Constance Fairweather", "13:48:00", "14:18:00", false, 0, null),
            Assignment("Arthur Beckwith", "15:30:00", "16:00:00", true, 0, null),
            Assignment("Pauline Draycott", "16:30:00", "17:00:00", false, 0, null),
            Assignment("Constance Fairweather", "18:30:00", "19:00:00", false, 0, null),
            Assignment("Pauline Draycott", "19:30:00", "20:00:00", false, 0, null),
        ),
    ),
    CarerDay(
        "Priya Sharma",
        listOf(
            Assignment("Baldev Sandhu", "08:00:00", "10:00:00", true, 0, null),
            Assignment("Reginald Askew", "10:30:00", "11:30:00", true, 0, null),
            Assignment("Baldev Sandhu", "12:00:00", "13:00:00", true, 0, null),
            Assignment("Reginald Askew", "13:30:00", "14:00:00", true, 0, null),
            Assignment("Sylvia Trenholm", "15:50:00", "16:20:00", true, 0, null),
            Assignment("Reginald Askew", "17:30:00", "18:00:00", true, 0, null),
            Assignment("Marjorie Ellerby", "18:12:00", "18:42:00", true, 0, null),
            Assignment("Sylvia Trenholm", "18:50:00", "19:20:00", true, 0, null),
        ),
    ),
    CarerDay(
        "Anjali Thapa",
        listOf(
            Assignment("Constance Fairweather", "07:45:00", "08:30:00", false, 0, null),
            Assignment("Arthur Beckwith", "09:00:00", "09:45:00", true, 0, "Constance Fairweather"),
            Assignment("Harold Wetherby", "09:45:00", "10:45:00", false, 0, null),
            Assignment("Baldev Sandhu", "12:00:00", "13:00:00", true, 0, null),
            Assignment("Alice Pemberton", "13:30:00", "14:00:00", false, 7, null),
            Assignment("Rosemary Dunnett", "14:08:00", "14:38:00", false, 0, null),
            Assignment("Arthur Beckwith", "15:30:00", "16:00:00", true, 0, null),
            Assignment("Alice Pemberton", "16:30:00", "17:00:00", false, 0, null),
            Assignment("Rosemary Dunnett", "18:52:00", "19:22:00", false, 0, null),
            Assignment("Alice Pemberton", "19:30:00", "20:00:00", false, 0, null),
        ),
    ),
)

private const val FIRST_HOUR = 7
private const val LAST_HOUR = 21
private const val PIXELS_PER_HOUR = 108
private const val COLUMN_WIDTH = 132
private const val CLOCK_WIDTH = 46
private const val HEADER_HEIGHT = 52

private val paper = Color(0xFFFAF9F5)
private val card = Color(0xFFFFFFFF)
private val ink = Color(0xFF24312D)
private val muted = Color(0xFF5F6E67)
private val line = Color(0xFFE3E1D8)
private val accent = Color(0xFF0F6E56)
private val singleFill = Color(0xFFDFF0E9)
private val singleInk = Color(0xFF085041)
private val singleEdge = Color(0xFF1D9E75)
private val doubleFill = Color(0xFFE7E5F8)
private val doubleInk = Color(0xFF3C3489)
private val doubleEdge = Color(0xFF7F77DD)
private val lateEdge = Color(0xFFE24B4A)
private val taxiEdge = Color(0xFF8A5A00)

private val mono = TextStyle(fontFamily = FontFamily.Monospace)

fun minuteOf(time: String): Int =
    time.substring(0, 2).toInt() * 60 + time.substring(3, 5).toInt()

fun clockOf(minute: Int): String =
    "${(minute / 60).toString().padStart(2, '0')}:${(minute % 60).toString().padStart(2, '0')}"

@Composable
fun CarerRota(state: State) {
    val days = wednesday
    var chosen by remember { mutableStateOf<Assignment?>(null) }
    val across = rememberScrollState()
    val down = rememberScrollState()
    val text = rememberTextMeasurer()
    val dayStart = FIRST_HOUR * 60
    val boardHeight = ((LAST_HOUR - FIRST_HOUR) * PIXELS_PER_HOUR).dp
    val boardWidth = (days.size * COLUMN_WIDTH).dp

    Column(modifier = Modifier.fillMaxSize().background(paper)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column {
                Text("Femi care", style = MaterialTheme.typography.labelMedium, color = accent)
                Text(
                    text = "Wednesday 19 August",
                    style = MaterialTheme.typography.headlineSmall,
                    color = ink,
                )
            }
            Text(
                text = "${days.sumOf { it.assignments.size }} visits · ${days.size} carers",
                style = MaterialTheme.typography.bodySmall,
                color = muted,
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.width(CLOCK_WIDTH.dp))
            Box(modifier = Modifier.horizontalScroll(across)) {
                Row(modifier = Modifier.width(boardWidth).height(HEADER_HEIGHT.dp)) {
                    days.forEach { day ->
                        val worked = day.assignments.sumOf { minuteOf(it.endTime) - minuteOf(it.startTime) }
                        Column(
                            modifier = Modifier
                                .width(COLUMN_WIDTH.dp)
                                .fillMaxHeight()
                                .background(paper)
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = day.carer.substringBefore(' '),
                                style = MaterialTheme.typography.titleSmall,
                                color = ink,
                                maxLines = 1,
                            )
                            Text(
                                text = "${worked / 60}h ${worked % 60}m",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = muted,
                            )
                        }
                    }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(down)) {
            Canvas(modifier = Modifier.size(CLOCK_WIDTH.dp, boardHeight)) {
                (FIRST_HOUR until LAST_HOUR).forEach { hour ->
                    val y = (hour * 60 - dayStart) / 60f * PIXELS_PER_HOUR.dp.toPx()
                    drawText(
                        textMeasurer = text,
                        text = clockOf(hour * 60),
                        topLeft = Offset(6.dp.toPx(), y + 2.dp.toPx()),
                        style = mono.copy(color = muted, fontSize = 11.sp),
                    )
                }
            }
            Box(modifier = Modifier.horizontalScroll(across)) {
                Canvas(
                    modifier = Modifier
                        .size(boardWidth, boardHeight)
                        .background(card)
                        .pointerInput(days) {
                            detectTapGestures { at ->
                                val columnWidth = COLUMN_WIDTH.dp.toPx()
                                val minute = dayStart + (at.y / (PIXELS_PER_HOUR.dp.toPx() / 60f)).toInt()
                                chosen = days.getOrNull((at.x / columnWidth).toInt())?.assignments
                                    ?.firstOrNull {
                                        minute >= minuteOf(it.startTime) && minute <= minuteOf(it.endTime)
                                    }
                            }
                        },
                ) {
                    val perMinute = PIXELS_PER_HOUR.dp.toPx() / 60f
                    val columnWidth = COLUMN_WIDTH.dp.toPx()
                    val pad = 8.dp.toPx()
                    (FIRST_HOUR until LAST_HOUR).forEach { hour ->
                        val y = (hour * 60 - dayStart) * perMinute
                        drawLine(line, Offset(0f, y), Offset(size.width, y))
                    }
                    days.forEachIndexed { column, day ->
                        val left = column * columnWidth
                        drawLine(line, Offset(left, 0f), Offset(left, size.height))
                        day.assignments.forEach { visit ->
                            val top = (minuteOf(visit.startTime) - dayStart) * perMinute
                            val height = (minuteOf(visit.endTime) - minuteOf(visit.startTime)) * perMinute
                            drawRoundRect(
                                color = if (visit.isDouble) doubleFill else singleFill,
                                topLeft = Offset(left + 3.dp.toPx(), top + 1.dp.toPx()),
                                size = Size(columnWidth - 6.dp.toPx(), height - 2.dp.toPx()),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                            )
                            drawRect(
                                color = if (visit.isDouble) doubleEdge else singleEdge,
                                topLeft = Offset(left + 3.dp.toPx(), top + 1.dp.toPx()),
                                size = Size(3.dp.toPx(), height - 2.dp.toPx()),
                            )
                            val blockInk = if (visit.isDouble) doubleInk else singleInk
                            drawText(
                                textMeasurer = text,
                                text = visit.client,
                                topLeft = Offset(left + pad + 3.dp.toPx(), top + 5.dp.toPx()),
                                size = Size(columnWidth - pad * 2, (height - 8.dp.toPx()).coerceAtLeast(1f)),
                                style = TextStyle(color = blockInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                                maxLines = 2,
                            )
                            if (height > 44.dp.toPx()) {
                                drawText(
                                    textMeasurer = text,
                                    text = "${visit.startTime.substring(0, 5)}–${visit.endTime.substring(0, 5)}",
                                    topLeft = Offset(left + pad + 3.dp.toPx(), top + height - 20.dp.toPx()),
                                    size = Size(columnWidth - pad * 2, 16.dp.toPx()),
                                    style = mono.copy(color = blockInk, fontSize = 10.sp),
                                    maxLines = 1,
                                )
                            }
                            if (visit.lateMins > 0) {
                                drawRoundRect(
                                    color = lateEdge,
                                    topLeft = Offset(left + columnWidth - 37.dp.toPx(), top + 4.dp.toPx()),
                                    size = Size(31.dp.toPx(), 15.dp.toPx()),
                                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                                )
                                drawText(
                                    textMeasurer = text,
                                    text = "+${visit.lateMins}m",
                                    topLeft = Offset(left + columnWidth - 34.dp.toPx(), top + 4.5f.dp.toPx()),
                                    style = mono.copy(color = Color.White, fontSize = 9.sp),
                                    maxLines = 1,
                                )
                            }
                            if (visit.taxiFrom != null) {
                                drawRoundRect(
                                    color = taxiEdge,
                                    topLeft = Offset(left + columnWidth - 37.dp.toPx(), top + height - 21.dp.toPx()),
                                    size = Size(31.dp.toPx(), 15.dp.toPx()),
                                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                                )
                                drawText(
                                    textMeasurer = text,
                                    text = "taxi",
                                    topLeft = Offset(left + columnWidth - 32.dp.toPx(), top + height - 20.5f.dp.toPx()),
                                    style = mono.copy(color = Color.White, fontSize = 9.sp),
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
        chosen?.let { visit ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(card)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(visit.client, style = MaterialTheme.typography.titleMedium, color = ink)
                    Text(
                        text = "${visit.startTime.substring(0, 5)}–${visit.endTime.substring(0, 5)}" +
                            if (visit.isDouble) " · double-up" else "",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        color = muted,
                    )
                    if (visit.lateMins > 0) {
                        Text(
                            text = "arrives ${clockOf(minuteOf(visit.startTime) + visit.lateMins)} (+${visit.lateMins}m)",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = lateEdge,
                        )
                    }
                    visit.taxiFrom?.let { from ->
                        Text(
                            text = "taxi from $from",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = taxiEdge,
                        )
                    }
                }
                Text(
                    text = "close",
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                    modifier = Modifier.pointerInput(Unit) { detectTapGestures { chosen = null } },
                )
            }
        }
    }
}

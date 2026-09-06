package market.femi.carer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import femi.app.shared.generated.resources.Res
import femi.app.shared.generated.resources.care_nav_tasks
import market.femi.Footer
import market.femi.State
import org.jetbrains.compose.resources.stringResource

enum class VisitStep { EN_ROUTE, WAIT_PARTNER, CLOCK_IN, TASKS, LOG, HANDOVER, AFTER, NO_ANSWER }

enum class TaskKind { DOING, CHECK, MEDS }

data class VisitTask(
    val name: String,
    val kind: TaskKind,
    val answers: List<String>,
    val evidenceAnswers: List<String>,
    val meds: List<String>,
)

data class AfterStep(
    val title: String,
    val body: String,
    val action: String,
)

data class DoorStage(
    val instruction: String,
    val action: String,
)

data class Visit(
    val client: String,
    val time: String,
    val leaveLine: String,
    val clockedIn: String,
    val postcode: String,
    val partner: String,
    val totp: Boolean,
    val noAnswer: Boolean,
    val after: AfterStep?,
    val carePlan: String,
    val lastHandover: String,
    val logChips: List<String>,
    val handoverChips: List<String>,
    val tasks: List<VisitTask>,
)

val notDoneReasons = listOf("Declined", "Not needed", "No supplies")

val incidentKinds = listOf("Fall", "Injury found", "Medication error", "Safeguarding concern")

val doorStages = listOf(
    DoorStage("No answer. Knock again and try the bell.", "Still no answer"),
    DoorStage("Ring Sylvia - 07700 900123.", "No answer to the call"),
    DoorStage(
        "Stay at the door. The office is ringing her daughter and the district nurse. " +
            "Every attempt so far is stamped on the record.",
        "The office came back",
    ),
    DoorStage(
        "Sylvia is at the hospital with her daughter. The visit is stood down and you are released. " +
            "The record shows the minute of every knock, call and answer.",
        "Understood",
    ),
)

fun doing(name: String) = VisitTask(name, TaskKind.DOING, emptyList(), emptyList(), emptyList())

fun check(name: String, answers: List<String>, evidence: List<String>) =
    VisitTask(name, TaskKind.CHECK, answers, evidence, emptyList())

fun meds(meds: List<String>) = VisitTask("Medication", TaskKind.MEDS, emptyList(), emptyList(), meds)

val simulatedDay = listOf(
    Visit(
        client = "Norah Pickering",
        time = "09:00",
        leaveLine = "Leave now. 12 minutes away, be there for 09:00.",
        clockedIn = "09:02",
        postcode = "TW15 1EF",
        partner = "",
        totp = false,
        noAnswer = false,
        after = AfterStep(
            title = "Running 12 minutes behind",
            body = "You tapped out at 10:12. The office and Baldev's home already know. Just drive.",
            action = "On my way",
        ),
        carePlan = "Norah washes at the basin and manages her top half herself - support, never take over. " +
            "Porridge and tea, one sugar. Both heels checked every visit, she is at risk of pressure damage. " +
            "Pendant alarm stays on. She consents to photos of skin concerns only.",
        lastHandover = "Bins go out tonight. She was asking about her daughter's Saturday visit.",
        logChips = listOf("Settled and chatty", "Ate well", "Ate a little", "Low mood", "Wanted the radio on", "Asked about her daughter"),
        handoverChips = listOf("Pads running low", "Watch her heels", "Bins done"),
        tasks = listOf(
            doing("toileting"),
            doing("meal preparation"),
            doing("feeding assistance"),
            doing("skin creams"),
            check("Skin check, both heels", listOf("Intact", "Redness", "Broken"), listOf("Redness", "Broken")),
            check("Fluids", listOf("Drank well", "Half a glass", "Sips", "Refused"), listOf("Refused")),
        ),
    ),
    Visit(
        client = "Baldev Sandhu",
        time = "12:00",
        leaveLine = "Head for Baldev Sandhu, 12:00. Meet Marlon outside.",
        clockedIn = "11:58",
        postcode = "TW18 2QA",
        partner = "Marlon Cheng",
        totp = true,
        noAnswer = false,
        after = AfterStep(
            title = "Take your break now",
            body = "60 minutes, yours. You have worked six hours; the law and your break rule both say stop. " +
                "Back on for Sylvia Trenholm at 15:00.",
            action = "Break done",
        ),
        carePlan = "Two carers for every transfer, full hoist, blue sling, loops on the third setting. " +
            "Medication from the blister pack only, midday row. He is turned every visit. " +
            "Missing a visit here is a fatality risk - never leave without the office knowing.",
        lastHandover = "New blister pack started this morning, top row is Monday.",
        logChips = listOf("Comfortable", "Transfer went well", "Ate well", "Tired today", "Mentioned pain"),
        handoverChips = listOf("Sling needs a wash", "GP was called", "Pain mentioned again"),
        tasks = listOf(
            doing("hoist transfer"),
            doing("meal preparation"),
            meds(listOf("Metformin 500mg", "Amlodipine 5mg")),
            check("Repositioned", listOf("Turned left", "Turned right", "On his back"), emptyList()),
        ),
    ),
    Visit(
        client = "Sylvia Trenholm",
        time = "15:00",
        leaveLine = "Leave for Sylvia Trenholm, 15:00. 9 minutes away.",
        clockedIn = "",
        postcode = "TW19 5NW",
        partner = "",
        totp = false,
        noAnswer = true,
        after = null,
        carePlan = "Tea visit is a light meal and company. She manages her own drinks. " +
            "Cream for her arms is in the bathroom cabinet, apply if she asks. " +
            "The waking night carer arrives at 22:00.",
        lastHandover = "District nurse due tomorrow morning.",
        logChips = emptyList(),
        handoverChips = emptyList(),
        tasks = emptyList(),
    ),
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CarerTasks(state: State) {
    var visitIndex by remember { mutableStateOf(0) }
    var step by remember { mutableStateOf(VisitStep.EN_ROUTE) }
    var doorStage by remember { mutableStateOf(0) }
    var readingPlan by remember { mutableStateOf(false) }
    val dayDone = visitIndex >= simulatedDay.size
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.care_nav_tasks)) },
                navigationIcon = {
                    if (state.nav.backStack.size > 1) {
                        IconButton(onClick = { state.nav.goBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
        bottomBar = { Footer(state) },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND.dp)
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            LinearProgressIndicator(
                progress = { visitIndex.toFloat() / simulatedDay.size },
                modifier = Modifier.fillMaxWidth(),
            )
            if (dayDone) {
                Text("Day complete", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "Two visits delivered, one stood down at the door. Every task, dose, " +
                        "attempt and minute is on the record, written while it was true.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val visit = simulatedDay[visitIndex]
                Text(
                    text = "Visit ${visitIndex + 1} of ${simulatedDay.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                when (step) {
                    VisitStep.EN_ROUTE -> {
                        Text("${visit.client}, ${visit.time}", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            text = visit.leaveLine,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        ListItem(
                            headlineContent = { Text(visit.postcode) },
                            supportingContent = {
                                Text(
                                    text = if (visit.partner.isEmpty()) "Solo visit"
                                    else "Double up with ${visit.partner}",
                                )
                            },
                        )
                        HorizontalDivider()
                        ListItem(
                            headlineContent = { Text("Last handover") },
                            supportingContent = { Text(visit.lastHandover) },
                        )
                        OutlinedButton(onClick = { readingPlan = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Care plan")
                        }
                        Button(
                            onClick = {
                                step = when {
                                    visit.noAnswer -> VisitStep.NO_ANSWER
                                    visit.partner.isNotEmpty() -> VisitStep.WAIT_PARTNER
                                    visit.totp -> VisitStep.CLOCK_IN
                                    else -> VisitStep.TASKS
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (visit.totp || visit.noAnswer) "Arrived" else "Arrived - tap the tag")
                        }
                    }
                    VisitStep.WAIT_PARTNER -> {
                        Text(visit.client, style = MaterialTheme.typography.headlineMedium)
                        Text(
                            text = "Wait for ${visit.partner.substringBefore(" ")} before going in. " +
                                "This visit never starts with one carer.",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "${visit.partner.substringBefore(" ")} is 4 minutes away. The office can see you both.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = { step = if (visit.totp) VisitStep.CLOCK_IN else VisitStep.TASKS },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("${visit.partner.substringBefore(" ")} is here")
                        }
                    }
                    VisitStep.CLOCK_IN -> {
                        Text("Clock in at ${visit.client}", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            text = "The six digits from the code box. You both clock in.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        var code by remember(visitIndex) { mutableStateOf("") }
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it },
                            label = { Text("Code") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(
                            onClick = { step = VisitStep.TASKS },
                            enabled = code.length == 6,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Clock in")
                        }
                    }
                    VisitStep.TASKS -> {
                        val resolved = remember(visitIndex) { mutableStateMapOf<String, String>() }
                        val medsOutcome = remember(visitIndex) { mutableStateMapOf<String, String>() }
                        var reasonFor by remember(visitIndex) { mutableStateOf("") }
                        var incidentOpen by remember(visitIndex) { mutableStateOf(false) }
                        var incident by remember(visitIndex) { mutableStateOf("") }
                        Text(visit.client, style = MaterialTheme.typography.headlineMedium)
                        Text(
                            text = "Clocked in ${visit.clockedIn}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        visit.tasks.forEach { task ->
                            when (task.kind) {
                                TaskKind.DOING -> {
                                    ListItem(
                                        headlineContent = { Text(task.name) },
                                        supportingContent = {
                                            val answer = resolved[task.name]
                                            if (answer != null && answer != "Done") Text(answer)
                                        },
                                        trailingContent = {
                                            when (resolved[task.name]) {
                                                null -> TextButton(onClick = { resolved[task.name] = "Done" }) { Text("Done") }
                                                "Done" -> Text("Done", color = MaterialTheme.colorScheme.primary)
                                                else -> {}
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    if (resolved[task.name] == null) {
                                        if (reasonFor == task.name) {
                                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                notDoneReasons.forEach { reason ->
                                                    FilterChip(
                                                        selected = false,
                                                        onClick = {
                                                            resolved[task.name] = reason
                                                            reasonFor = ""
                                                        },
                                                        label = { Text(reason) },
                                                    )
                                                }
                                            }
                                        } else {
                                            TextButton(onClick = { reasonFor = task.name }) { Text("Can't do this one") }
                                        }
                                    }
                                }
                                TaskKind.CHECK -> {
                                    ListItem(
                                        headlineContent = { Text(task.name) },
                                        supportingContent = {
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    task.answers.forEach { answer ->
                                                        FilterChip(
                                                            selected = resolved[task.name] == answer,
                                                            onClick = { resolved[task.name] = answer },
                                                            label = { Text(answer) },
                                                        )
                                                    }
                                                }
                                                if (resolved[task.name] in task.evidenceAnswers) {
                                                    OutlinedButton(onClick = {}) {
                                                        Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                                                        Text("  Photo and body map")
                                                    }
                                                }
                                            }
                                        },
                                    )
                                }
                                TaskKind.MEDS -> {
                                    ListItem(
                                        headlineContent = { Text(task.name) },
                                        supportingContent = { Text("From the blister pack, midday row") },
                                    )
                                    task.meds.forEach { med ->
                                        ListItem(
                                            headlineContent = { Text(med) },
                                            supportingContent = {
                                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    listOf("Given", "Refused", "Not available").forEach { outcome ->
                                                        FilterChip(
                                                            selected = medsOutcome[med] == outcome,
                                                            onClick = {
                                                                medsOutcome[med] = outcome
                                                                if (task.meds.all { medsOutcome[it] != null }) {
                                                                    resolved[task.name] = "Recorded"
                                                                }
                                                            },
                                                            label = { Text(outcome) },
                                                        )
                                                    }
                                                }
                                            },
                                        )
                                    }
                                    if (task.meds.any { medsOutcome[it] == "Refused" || medsOutcome[it] == "Not available" }) {
                                        Text(
                                            text = "The office is told the moment you tap out.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.tertiary,
                                        )
                                    }
                                }
                            }
                            HorizontalDivider()
                        }
                        if (incidentOpen) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                incidentKinds.forEach { kind ->
                                    FilterChip(
                                        selected = incident == kind,
                                        onClick = { incident = kind },
                                        label = { Text(kind) },
                                    )
                                }
                            }
                            if (incident.isNotEmpty()) {
                                Text(
                                    text = "$incident recorded. The office is told the moment you tap out.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.tertiary,
                                )
                            }
                        } else {
                            TextButton(onClick = { incidentOpen = true }) { Text("Something happened") }
                        }
                        val unanswered = visit.tasks.count { resolved[it.name] == null }
                        Button(
                            onClick = { step = VisitStep.LOG },
                            enabled = unanswered == 0,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (unanswered == 0) "Write the log" else "$unanswered tasks unanswered")
                        }
                    }
                    VisitStep.LOG -> {
                        val chosen = remember(visitIndex) { mutableStateListOf<String>() }
                        var more by remember(visitIndex) { mutableStateOf(false) }
                        var custom by remember(visitIndex) { mutableStateOf("") }
                        Text(
                            text = "How was ${visit.client.substringBefore(" ")}?",
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            visit.logChips.forEach { chip ->
                                FilterChip(
                                    selected = chip in chosen,
                                    onClick = { if (chip in chosen) chosen.remove(chip) else chosen.add(chip) },
                                    label = { Text(chip) },
                                )
                            }
                            FilterChip(
                                selected = more,
                                onClick = { more = !more },
                                label = { Text("More") },
                            )
                        }
                        if (more) {
                            OutlinedTextField(
                                value = custom,
                                onValueChange = { custom = it },
                                label = { Text("Anything unusual") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Button(
                            onClick = { step = VisitStep.HANDOVER },
                            enabled = chosen.isNotEmpty() || custom.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Log it")
                        }
                    }
                    VisitStep.HANDOVER -> {
                        val chosen = remember(visitIndex) { mutableStateListOf<String>() }
                        Text("Anything for the next carer?", style = MaterialTheme.typography.headlineMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            visit.handoverChips.forEach { chip ->
                                FilterChip(
                                    selected = chip in chosen,
                                    onClick = { if (chip in chosen) chosen.remove(chip) else chosen.add(chip) },
                                    label = { Text(chip) },
                                )
                            }
                        }
                        Button(
                            onClick = {
                                if (visit.after != null) {
                                    step = VisitStep.AFTER
                                } else {
                                    visitIndex += 1
                                    step = VisitStep.EN_ROUTE
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Tap out")
                        }
                    }
                    VisitStep.AFTER -> {
                        val after = visit.after
                        if (after != null) {
                            Text(after.title, style = MaterialTheme.typography.headlineMedium)
                            Text(
                                text = after.body,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Button(
                                onClick = {
                                    visitIndex += 1
                                    step = VisitStep.EN_ROUTE
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(after.action)
                            }
                        }
                    }
                    VisitStep.NO_ANSWER -> {
                        val stage = doorStages[doorStage]
                        Text(visit.client, style = MaterialTheme.typography.headlineMedium)
                        Text(
                            text = stage.instruction,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "Do not leave the door until the office says so.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                        Button(
                            onClick = {
                                if (doorStage < doorStages.lastIndex) {
                                    doorStage += 1
                                } else {
                                    visitIndex += 1
                                    step = VisitStep.EN_ROUTE
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stage.action)
                        }
                    }
                }
            }
        }
    }
    if (readingPlan && !dayDone) {
        AlertDialog(
            onDismissRequest = { readingPlan = false },
            title = { Text(simulatedDay[visitIndex].client) },
            text = { Text(simulatedDay[visitIndex].carePlan) },
            confirmButton = {
                TextButton(onClick = { readingPlan = false }) { Text("Close") }
            },
        )
    }
}

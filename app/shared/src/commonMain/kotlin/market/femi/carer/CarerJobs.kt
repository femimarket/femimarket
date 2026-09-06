package market.femi.carer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import market.femi.ActiveShowState
import market.femi.State
import market.femi.services.LogService
import market.femi.services.createRealLogService

const val CARE_RATE = 13
const val NEARBY_MIN_MINUTES = 35
const val NEARBY_MAX_MINUTES = 120

class CareJobsState(private val state: State) : ActiveShowState(load = {state.isWorking = it}) {
    private val log: LogService = createRealLogService("CareJobsState")
    var postcode by mutableStateOf("")
    var nearby by mutableStateOf("")
    var searching by mutableStateOf(false)

    private var searchJob: Job? = null
    fun searchNearbyShifts(search:String) {
        postcode = search
        searchJob?.cancel()
        if (search.filter { !it.isWhitespace() }.length < 3) {
            searching = false
            return
        }
        searching = true
        searchJob = state.scope.launch {
            delay(1000.milliseconds)
            if (postcode.filter { !it.isWhitespace() }.length < 5) {
                searching = false
                return@launch
            }
            searching = false
            working(state.scope, log, ::searchNearbyShifts.name, requireShown = false) {
                nearby = state.care.getShiftsNearby(postcode, NEARBY_MIN_MINUTES, NEARBY_MAX_MINUTES)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CareJobs(state: State) {
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
            text = "You pick the shifts.",
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            text = "Care work in Surrey and West London at £$CARE_RATE an hour. " +
                "You set your hours first, then choose the visits that fit them.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Where are you travelling from?",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 12.dp),
        )
        OutlinedTextField(
            value = state.careApp.jobs.postcode,
            onValueChange = { state.careApp.jobs.searchNearbyShifts(it) },
            label = { Text("Your postcode") },
            singleLine = true,
            trailingIcon = {
                if (state.careApp.jobs.searching || state.careApp.jobs.isWorking) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else if (state.careApp.jobs.err.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Filled.Error,
                        contentDescription = null,
                    )
                } else if (state.careApp.jobs.nearby.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Filled.Done,
                        contentDescription = null,
                    )
                }
            },
            isError = state.careApp.jobs.err.isNotEmpty(),
            supportingText = if (state.careApp.jobs.err.isNotEmpty()) {
                {
                    Text("Could not check your postcode.")
                }
            } else {
                null
            },
            modifier = Modifier.width(240.dp),
        )
        if (state.careApp.jobs.err.isNotEmpty()) {
            Button(onClick = { state.careApp.jobs.searchNearbyShifts(state.careApp.jobs.postcode) }) {
                Text("Try again")
            }
        } else if (state.careApp.jobs.nearby.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = state.careApp.jobs.nearby,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Button(
                onClick = { state.nav.openCareJobsExperience() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text("Show me the work")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("£$CARE_RATE", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                Text("an hour, flat", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("You", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                Text("set the hours", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("No", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                Text("interview to book", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
        Text(
            text = "Everything is done here — hours, shifts, paperwork. " +
                "The first time you meet us is your assessment.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

package market.femi.carer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import market.femi.Footer
import market.femi.State
import market.femi.TEST_API_URL
import market.femi.care.models.UserType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CareServers(state: State) {
    LaunchedEffect(Unit) {
        state.care.getMe()
    }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
//        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Your server, your data") },
                subtitle = { Text("Your data stays with whoever runs your service.") },
//                scrollBehavior = scrollBehavior,
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
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Server setup",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = "The people responsible for the care are the same people who hold the data. No third party.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Hosting",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Run it on your own servers for free. Trial our demo before committing.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    OutlinedTextField(
                        value = state.kv.apiUrl,
                        onValueChange = { state.kv.apiUrl = it },
                        label = { Text("Care server URL") },
                        supportingText = { Text("Your server, or the demo.") },
                        placeholder = { Text(TEST_API_URL) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Your role",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Everyone in the company uses the same system. Your role decides what you see and what you can change.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Column(Modifier.selectableGroup()) {
                        state.care.users.forEach { user ->
                            ListItem(
                                headlineContent = {
                                    Text(
                                        when (user.userType) {
                                            UserType.ServiceUser -> "Service user"
                                            UserType.Staff -> "Staff"
                                            UserType.Candidate -> "Candidate"
                                            UserType.Management -> "Management"
                                        },
                                    )
                                },
                                leadingContent = { RadioButton(selected = state.care.user == user, onClick = null) },
                                modifier = Modifier.selectable(
                                    selected = state.care.user == user,
                                    role = Role.RadioButton,
                                    onClick = { state.care.user = user },
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

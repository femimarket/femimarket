package market.femi.carer.CareServiceUser

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import market.femi.State
import market.femi.care.models.User

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CareServiceUserList(state: State) {
    var serviceUsers by remember { mutableStateOf<List<User>?>(null) }
    LaunchedEffect(Unit) {
        serviceUsers = state.care.listServiceUsers()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Service users") },
                subtitle = { Text("Everyone you care for") },
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
        floatingActionButton = {
            FloatingActionButton(onClick = { state.nav.openCareServiceUserCreateUserName() }) {
                Icon(Icons.Filled.Add, contentDescription = "New service user")
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .wrapContentWidth(Alignment.CenterHorizontally)
                    .widthIn(max = WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND.dp)
                    .fillMaxWidth(),
            ) {
                serviceUsers?.forEachIndexed { index, user ->
                    ListItem(
                        headlineContent = { Text(listOfNotNull(user.firstName, user.lastName).joinToString(" ")) },
                    )
                    if (index != serviceUsers?.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
            if (serviceUsers?.isEmpty() == true) {
                Text("No service users yet", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

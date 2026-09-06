package market.femi.carer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import femi.app.shared.generated.resources.Res
import femi.app.shared.generated.resources.care_account_service_users
import femi.app.shared.generated.resources.care_account_staffs
import femi.app.shared.generated.resources.care_availability
import femi.app.shared.generated.resources.care_clients
import femi.app.shared.generated.resources.care_nav_account
import femi.app.shared.generated.resources.care_nav_availability
import femi.app.shared.generated.resources.care_staff
import market.femi.CarouselMenu
import market.femi.CarouselMenuItem
import market.femi.Footer
import market.femi.State
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareAccount(state: State) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.care_nav_account)) },
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
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            CarouselMenu(
                items = listOf(
                    CarouselMenuItem(
                        title = stringResource(Res.string.care_account_service_users),
                        imagePainter = painterResource(Res.drawable.care_clients),
                        onClick = { state.nav.openCareServiceUserList() },
                    ),
                    CarouselMenuItem(
                        title = stringResource(Res.string.care_nav_availability),
                        imagePainter = painterResource(Res.drawable.care_availability),
                        onClick = { state.nav.openCareAvailability() },
                    ),
                    CarouselMenuItem(
                        title = stringResource(Res.string.care_account_staffs),
                        imagePainter = painterResource(Res.drawable.care_staff),
                        onClick = {},
                    ),
                ),
            )
        }
    }
}

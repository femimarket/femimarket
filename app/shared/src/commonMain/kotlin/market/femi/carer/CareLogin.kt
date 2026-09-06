package market.femi.carer

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import femi.app.shared.generated.resources.Res
import femi.app.shared.generated.resources.care_belief
import femi.app.shared.generated.resources.care_goal
import femi.app.shared.generated.resources.care_login_belief
import femi.app.shared.generated.resources.care_login_belief_title
import femi.app.shared.generated.resources.care_login_enter
import femi.app.shared.generated.resources.care_login_goal
import femi.app.shared.generated.resources.care_login_goal_title
import femi.app.shared.generated.resources.care_login_how
import femi.app.shared.generated.resources.care_login_how_title
import femi.app.shared.generated.resources.care_login_service
import femi.app.shared.generated.resources.care_login_service_title
import femi.app.shared.generated.resources.care_login_what
import femi.app.shared.generated.resources.care_login_what_title
import femi.app.shared.generated.resources.care_login_why
import femi.app.shared.generated.resources.care_login_why_title
import femi.app.shared.generated.resources.care_why
import femi.app.shared.generated.resources.matrix_logo
import femi.app.shared.generated.resources.splash_care
import io.github.kdroidfilter.composemediaplayer.VideoPlayerSurface
import io.github.kdroidfilter.composemediaplayer.rememberVideoPlayerState
import kotlinx.coroutines.launch
import market.femi.State
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareLogin(state: State) {
    LaunchedEffect(Unit){
        state.matrix.checkLogin()
        state.care.getMe()
        state.care.user = state.care.user ?: state.care.users.firstOrNull()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.splash_care)) },
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
    ) { padding ->
    BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(padding)) {
        val viewportHeight = maxHeight
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND.dp)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(Res.string.care_login_what_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(Res.string.care_login_what),
                style = MaterialTheme.typography.bodyLarge,
            )
            val player = rememberVideoPlayerState()
            LaunchedEffect(Unit) {
                player.loop = true
                player.volume = 0f
                player.openUri(state.kv.withFsUrl("care-what-this-is.mp4"))
            }
            VideoPlayerSurface(
                playerState = player,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(MaterialTheme.shapes.extraLarge),
                contentScale = ContentScale.Crop,
            )

            Text(
                text = stringResource(Res.string.care_login_why_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(Res.string.care_login_why),
                style = MaterialTheme.typography.bodyLarge,
            )
            val whyY = remember { mutableStateOf(0f) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .onGloballyPositioned { whyY.value = it.positionInParent().y },
            ) {
                Image(
                    painter = painterResource(Res.drawable.care_why),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .offset {
                            val progress = ((scrollState.value + viewportHeight.toPx() - whyY.value) /
                                (viewportHeight.toPx() + 200.dp.toPx())).coerceIn(0f, 1f)
                            IntOffset(0, ((progress - 1f) * 80.dp.toPx()).roundToInt())
                        },
                )
            }

            Text(
                text = stringResource(Res.string.care_login_belief_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(Res.string.care_login_belief),
                style = MaterialTheme.typography.bodyLarge,
            )
            val beliefY = remember { mutableStateOf(0f) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .onGloballyPositioned { beliefY.value = it.positionInParent().y },
            ) {
                Image(
                    painter = painterResource(Res.drawable.care_belief),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .offset {
                            val progress = ((scrollState.value + viewportHeight.toPx() - beliefY.value) /
                                (viewportHeight.toPx() + 200.dp.toPx())).coerceIn(0f, 1f)
                            IntOffset(0, ((progress - 1f) * 80.dp.toPx()).roundToInt())
                        },
                )
            }

            Text(
                text = stringResource(Res.string.care_login_goal_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(Res.string.care_login_goal),
                style = MaterialTheme.typography.bodyLarge,
            )
            val goalY = remember { mutableStateOf(0f) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .onGloballyPositioned { goalY.value = it.positionInParent().y },
            ) {
                Image(
                    painter = painterResource(Res.drawable.care_goal),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .offset {
                            val progress = ((scrollState.value + viewportHeight.toPx() - goalY.value) /
                                (viewportHeight.toPx() + 200.dp.toPx())).coerceIn(0f, 1f)
                            IntOffset(0, ((progress - 1f) * 80.dp.toPx()).roundToInt())
                        },
                )
            }

            Text(
                text = stringResource(Res.string.care_login_service_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(Res.string.care_login_service),
                style = MaterialTheme.typography.bodyLarge,
            )

            Text(
                text = stringResource(Res.string.care_login_how_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(Res.string.care_login_how),
                style = MaterialTheme.typography.bodyLarge,
            )
            Image(
                painter = painterResource(Res.drawable.matrix_logo),
                contentDescription = null,
                modifier = Modifier.height(48.dp),
            )
            Text(
                text = "Matrix is a decentralised encrypted chat service. It provides authentication services and communications protocol.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "To login, you'll be provided with a code and taken to matrix servers use existent or create new account with code. Once complete, you'll be logged in.",
                style = MaterialTheme.typography.bodyLarge,
            )
            if (state.matrix.userId.isNotEmpty()) {
                Button(onClick = { state.nav.openCareServers() }) {
                    Text("Enter")
                }
                OutlinedButton(onClick = { state.scope.launch { state.matrix.signOut() } }) {
                    Text("Log out")
                }
            } else {
                Button(
                    onClick = { state.scope.launch {
                        state.matrix.login()
                        state.care.getMe()
                        state.care.user = state.care.user ?: state.care.users.firstOrNull()
                    } },
                    enabled = state.matrix.userCode == null,
                ) {
                    Text(stringResource(Res.string.care_login_enter))
                }
                state.matrix.userCode?.let { code ->
                    Text(
                        text = "Enter this code on the sign in page",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Card {
                        Text(
                            text = code,
                            style = MaterialTheme.typography.displayMedium,
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                        )
                    }
                    val clipboard = LocalClipboardManager.current
                    OutlinedButton(onClick = { clipboard.setText(AnnotatedString(code)) }) {
                        Text("Copy code")
                    }
                    val uriHandler = LocalUriHandler.current
                    OutlinedButton(onClick = { state.matrix.verificationUri?.let(uriHandler::openUri) }) {
                        Text("Open the sign in page")
                    }
                    CircularProgressIndicator()
                    Text(
                        text = "Waiting for you to finish on the page.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
    }
}

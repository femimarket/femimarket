package market.femi

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import femi.app.shared.generated.resources.Res
import femi.app.shared.generated.resources.matrix_logo
import org.jetbrains.compose.resources.painterResource
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.authProviders
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.encodeURLParameter
import io.ktor.http.isSuccess
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import market.femi.services.LogService
import market.femi.services.createRealLogService

@Composable
fun Login(state: State) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND.dp)
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        LaunchedEffect(Unit) {
            state.login.check()
        }
        Text(
            text = state.login.status,
            style = MaterialTheme.typography.headlineMedium,
        )
        Image(
            painter = painterResource(Res.drawable.matrix_logo),
            contentDescription = null,
            modifier = Modifier.height(48.dp),
        )
//        Text(
//            text = "Why Matrix?",
//            style = MaterialTheme.typography.headlineSmall,
//        )
        Text(
            text = "Matrix is a decentralised encrypted chat service. It provides authentication services and communications protocol.",
            style = MaterialTheme.typography.bodyLarge,
        )

        Text(
            text = "To login, you'll be provided with a code and taken to matrix servers use existent or create new account with code. Once complete, you'll be logged in.",
            style = MaterialTheme.typography.bodyLarge,
        )

        Button(
            onClick = { state.login.start() },
            enabled = !state.login.isWorking && !state.login.loggedIn,
        ) {
            Text("Get a code")
        }
        state.login.userCode?.let { code ->
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
            OutlinedButton(onClick = { uriHandler.openUri(state.login.verificationUri) }) {
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

class LoginState(val state: State) : ActiveShowState(onShow = { state.showMenu.hide() }) {
    // the code the user types on the homeserver's page, and where to type it
    var userCode by mutableStateOf<String?>(null)
    var verificationUri by mutableStateOf("")
    var deviceExpiry by mutableStateOf<kotlin.time.Instant?>(null)
    var loggedIn by mutableStateOf(false)
    val status by derivedStateOf {
        when {
            isWorking -> "Checking if you are logged in"
            err.isNotEmpty() -> "Could not check if you are logged in"
            loggedIn -> "You are logged in"
            else -> "You are not logged in"
        }
    }

    private val log: LogService = createRealLogService("LoginState")

    fun check() = working(
        scope = state.scope,
        log = state.log,
        name = "check",
        requireShown = false,
    ) {
//        loggedIn = state.matrix.isLoggedIn
        log.d { "[check] loggedIn $loggedIn" }
    }

    private val client = HttpClient()

    // asks the homeserver for a code, then polls it until the user approves
    fun start() = working(
        scope = state.scope,
        log = state.log,
        name = "login",
        requireShown = false,
    ) {
        val auth = "${state.kv.matrixUrl}/auth/oauth2"
        // one registration per install: the homeserver keeps every client we register,
        // so a stored id is reused instead of minting a new client each login
        val clientId = state.kv.matrixClientId.ifEmpty {
            val response = client.post("$auth/registration") {
                contentType(ContentType.Application.Json)
                setBody(
                    """{"client_name":"femi","client_uri":"https://femi.market","application_type":"native","token_endpoint_auth_method":"none","grant_types":["urn:ietf:params:oauth:grant-type:device_code","refresh_token"],"response_types":[]}""",
                )
            }
            if (!response.status.isSuccess()) {
                error("[registration] ${response.status}: ${response.bodyAsText()}")
            }
            Json.parseToJsonElement(response.bodyAsText())
                .jsonObject["client_id"]!!.jsonPrimitive.content
                .also {
                    state.kv.matrixClientId = it
                    log.d { "[start] registered client $it" }
                }
        }
        log.d { "[start] client $clientId" }
        val deviceId = (1..10)
            .map { "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".random() }
            .joinToString("")
        val scope = "urn:matrix:org.matrix.msc2967.client:api:* urn:matrix:org.matrix.msc2967.client:device:$deviceId"
        val device = client.post("$auth/device") {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody("client_id=$clientId&scope=${scope.encodeURLParameter()}")
        }
        if (!device.status.isSuccess()) {
            error("[device] ${device.status}: ${device.bodyAsText()}")
        }
        val grant = Json.parseToJsonElement(device.bodyAsText()).jsonObject
        userCode = grant["user_code"]!!.jsonPrimitive.content
        verificationUri = (grant["verification_uri_complete"] ?: grant["verification_uri"])!!.jsonPrimitive.content
        deviceExpiry = kotlin.time.Clock.System.now() + grant["expires_in"]!!.jsonPrimitive.int.seconds
        log.d { "[start] code $userCode uri $verificationUri expires $deviceExpiry" }
        try {
            var interval = grant["interval"]?.jsonPrimitive?.intOrNull ?: 5
            while (true) {
                if (kotlin.time.Clock.System.now() >= deviceExpiry!!) {
                    log.w { "[start] code expired at $deviceExpiry" }
                    error("[login] the code expired, get a new one")
                }
                delay((interval * 1000L).milliseconds)
                val token = client.post("$auth/token") {
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody(
                        "grant_type=${"urn:ietf:params:oauth:grant-type:device_code".encodeURLParameter()}" +
                            "&device_code=${grant["device_code"]!!.jsonPrimitive.content}" +
                            "&client_id=$clientId",
                    )
                }
                val body = Json.parseToJsonElement(token.bodyAsText()).jsonObject
                if (token.status.isSuccess()) {
                    state.kv.matrixAccessToken = body["access_token"]!!.jsonPrimitive.content
                    state.kv.matrixRefreshToken = body["refresh_token"]!!.jsonPrimitive.content
                    // the shared client memorizes the first token pair it loads; drop
                    // that memory so its next request re-reads kv and gets this pair
                    state.http.client.authProviders.filterIsInstance<BearerAuthProvider>().forEach { it.clearToken() }
                    log.d { "[start] tokens received access=${state.kv.matrixAccessToken} refresh=${state.kv.matrixRefreshToken} expires_in=${body["expires_in"]?.jsonPrimitive?.intOrNull}" }
                    break
                }
                when (body["error"]?.jsonPrimitive?.content) {
                    "authorization_pending" -> log.d { "[start] pending, next poll in ${interval}s" }
                    "slow_down" -> {
                        interval += 5
                        log.d { "[start] slow_down, next poll in ${interval}s" }
                    }
                    else -> error("[token] ${token.status}: ${token.bodyAsText()}")
                }
            }
            val whoami = client.get("${state.kv.matrixUrl}/_matrix/client/v3/account/whoami") {
                header(HttpHeaders.Authorization, "Bearer ${state.kv.matrixAccessToken}")
            }
            if (!whoami.status.isSuccess()) {
                error("[whoami] ${whoami.status}: ${whoami.bodyAsText()}")
            }
            val userId = Json.parseToJsonElement(whoami.bodyAsText()).jsonObject["user_id"]!!.jsonPrimitive.content
            log.d { "[start] whoami $userId" }
            state.matrix.login()
//            loggedIn = state.matrix.isLoggedIn
            log.d { "[start] login() loggedIn=$loggedIn access=${state.kv.matrixAccessToken}" }
        } finally {
            userCode = null
            deviceExpiry = null
        }
    }
}

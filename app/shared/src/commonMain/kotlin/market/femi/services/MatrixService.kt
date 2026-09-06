package market.femi.services

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.connect2x.trixnity.clientserverapi.client.LogoutInfo
import de.connect2x.trixnity.clientserverapi.client.MatrixClientAuthProviderData
import de.connect2x.trixnity.clientserverapi.client.MatrixClientAuthProviderDataStore
import de.connect2x.trixnity.clientserverapi.client.MatrixClientServerApiClientImpl
import de.connect2x.trixnity.clientserverapi.client.oauth2.ApplicationType
import de.connect2x.trixnity.clientserverapi.client.oauth2.LocalizedField
import de.connect2x.trixnity.clientserverapi.client.oauth2.OAuth2DeviceAuthorizationLoginFlowImpl
import de.connect2x.trixnity.clientserverapi.client.oauth2.OAuth2MatrixClientAuthProvider
import de.connect2x.trixnity.clientserverapi.client.oauth2.OAuth2MatrixClientAuthProviderData
import de.connect2x.trixnity.clientserverapi.client.oauth2.oAuth2
import de.connect2x.trixnity.core.MatrixServerException
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import market.femi.State

interface MatrixService {
//    val isLoggedIn: Boolean
    val userId: String
    val userCode: String?
    val verificationUri: String?
    suspend fun checkLogin()
    suspend fun login()
    suspend fun signOut()
}

fun createRealMatrixService(kv: KvService, http: HttpService): MatrixService = RealOpenApiMatrixService(kv, http)

class RealOpenApiMatrixService(
    private val kv: KvService,
    private val http: HttpService,
    private val log: LogService = createRealLogService("RealOpenApiMatrixService"),
) : MatrixService {

//    override var isLoggedIn by mutableStateOf(kv.matrixAccessToken.isNotEmpty())
//        private set
    override var userId by mutableStateOf("")
        private set

    val store = object : MatrixClientAuthProviderDataStore {
        override suspend fun getAuthData() = kv.matrixAccessToken.takeIf { it.isNotEmpty() }?.let {
            MatrixClientAuthProviderData.oAuth2(Url(kv.matrixUrl), kv.matrixClientId, it, refreshToken = kv.matrixRefreshToken.ifEmpty { null })
        }
        override suspend fun setAuthData(authData: MatrixClientAuthProviderData?) {
            (authData as? OAuth2MatrixClientAuthProviderData)?.let {
                kv.matrixClientId = it.clientId
                kv.matrixAccessToken = it.accessToken
                it.refreshToken?.let { refreshToken -> kv.matrixRefreshToken = refreshToken }
            }
        }
    }
    val onLogout: suspend (LogoutInfo) -> Unit = {
        kv.matrixAccessToken = ""
        kv.matrixRefreshToken = ""
        userId = ""
    }
    val authProvider = OAuth2MatrixClientAuthProvider(
        Url(kv.matrixUrl),
        store,
        onLogout = onLogout,
        httpClientEngine = http.client.engine,
        httpClientConfig = null
    )
    val api = MatrixClientServerApiClientImpl(authProvider = authProvider)

    override suspend fun checkLogin() {
        api.authentication.whoAmI().fold(
            onSuccess = {
                userId = it.userId.full
            },
            onFailure = { error ->
                if (error is MatrixServerException && error.statusCode == HttpStatusCode.Unauthorized) {
                    userId = ""
                } else {
                    log.e { "[checkLogin] $error" }
                    throw error
                }
            },
        )
    }

    override suspend fun signOut() {
        authProvider.logout().onFailure { error ->
            log.e { "[signOut] $error" }
            throw error
        }
        onLogout(LogoutInfo(isSoft = false, isLocked = false))
    }

    override var userCode by mutableStateOf<String?>(null)
        private set
    override var verificationUri by mutableStateOf<String?>(null)
        private set

    override suspend fun login() {
        val flow = OAuth2DeviceAuthorizationLoginFlowImpl(
            baseUrl = Url(kv.matrixUrl),
            applicationType = ApplicationType.Native,
            clientUri = "https://femi.market",
            redirectUri = "https://femi.market",
            clientName = LocalizedField("femi"),
            httpClientEngine = http.client.engine,
        )
        val request = flow.createAuthRequest().getOrThrow()
        userCode = request.userCode
        verificationUri = (request.verificationUriComplete ?: request.verificationUri).toString()
        try {
            store.setAuthData(flow.waitForLogin().getOrThrow())
            checkLogin()
        } finally {
            userCode = null
            verificationUri = null
        }
    }
}


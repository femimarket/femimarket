package market.femi.services

import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import io.ktor.serialization.kotlinx.json.json
import market.femi.AppJson
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

interface HttpService {
    val client: HttpClient
}

fun createRealHttpService(kv: KvService): HttpService = RealHttpService(kv)

// the ONE http client the generated api clients share: json setup (the
// pass-a-client constructor skips the setup the engine constructor does)
// plus bearer auth. ktor attaches the token, catches the 401, refreshes,
// and retries by itself. MAS rotates on refresh: BOTH tokens come back
// new and BOTH are written back.
class RealHttpService(
    private val kv: KvService,
    private val log: LogService = createRealLogService("RealHttpService"),
) : HttpService {
    override val client: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(AppJson)
        }
        install(Auth) {
            bearer {
                loadTokens {
                    log.d { "[loadTokens] reading kv access=${kv.matrixAccessToken} refresh=${kv.matrixRefreshToken}" }
                    BearerTokens(kv.matrixAccessToken, kv.matrixRefreshToken)
                }
                refreshTokens {
                    log.d { "[refreshTokens] 401 from ${response.call.request.url}, refreshing with refresh=${kv.matrixRefreshToken} client_id=${kv.matrixClientId}" }
                    val refreshed = client.submitForm(
                        url = "${kv.matrixUrl}/auth/oauth2/token",
                        formParameters = parameters {
                            append("grant_type", "refresh_token")
                            append("refresh_token", kv.matrixRefreshToken)
                            append("client_id", kv.matrixClientId)
                        },
                    ) { markAsRefreshTokenRequest() }
                    val text = refreshed.bodyAsText()
                    if (!refreshed.status.isSuccess()) {
                        log.e { "[refreshTokens] homeserver refused: ${refreshed.status}: $text" }
                        return@refreshTokens null
                    }
                    val body = AppJson.parseToJsonElement(text).jsonObject
                    kv.matrixAccessToken = body["access_token"]!!.jsonPrimitive.content
                    kv.matrixRefreshToken = body["refresh_token"]!!.jsonPrimitive.content
                    log.d { "[refreshTokens] rotated, new access=${kv.matrixAccessToken} refresh=${kv.matrixRefreshToken}" }
                    BearerTokens(kv.matrixAccessToken, kv.matrixRefreshToken)
                }
            }
        }
    }
}

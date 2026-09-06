package market.femi.services

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import market.femi.ui.apis.ComposablesApi
import market.femi.ui.models.Composable

interface UiService {
    suspend fun getComposable(composableId: Int): JsonElement
    suspend fun getComposables(): List<Composable>
}

fun createRealUiService(kv: KvService): UiService = RealOpenApiUiService(kv)

class RealOpenApiUiService(
    private val kv: KvService,
    private val client: HttpClient = HttpClient(),
    private val log: LogService = createRealLogService("RealOpenApiUiService"),
    private val url: String = "${kv.apiUrl}/api/ui",
) : UiService {

    override suspend fun getComposable(composableId: Int): JsonElement {
        val response = client.get("$url/composables/$composableId")
        if (!response.status.isSuccess()) {
            log.e { "[getComposable] ${response.status}" }
            error("[getComposable] ${response.status}: ${response.bodyAsText()}")
        }
        return Json.parseToJsonElement(response.bodyAsText())
    }

    override suspend fun getComposables(): List<Composable> {
        val response = ComposablesApi(url).list()
        if (!response.success) {
            log.e { "[getComposables] ${response.status}" }
            error("[getComposables] ${response.status}")
        }
        return response.body()
    }
}

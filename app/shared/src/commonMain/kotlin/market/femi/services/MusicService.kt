package market.femi.services

import io.ktor.client.request.forms.FormPart
import io.ktor.client.request.forms.InputProvider
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.core.ByteReadPacket
import market.femi.music.apis.AssetsApi
import market.femi.music.models.Asset

interface MusicService {
    suspend fun uploadAsset(name: String, bytes: ByteArray): Asset
}

fun createRealMusicService(kv: KvService, http: HttpService): MusicService = RealOpenApiMusicService(kv, http)

class RealOpenApiMusicService(
    private val kv: KvService,
    private val http: HttpService,
    private val log: LogService = createRealLogService("RealOpenApiMusicService"),
    private val url: String = kv.apiUrl,
) : MusicService {

    private val api = AssetsApi(url, http.client)

    override suspend fun uploadAsset(name: String, bytes: ByteArray): Asset {
        // the generated client leaves the multipart plumbing to the caller: the
        // Content-Disposition filename is REQUIRED — the server 400s without it.
        val response = api.post(
            FormPart(
                "file",
                InputProvider(bytes.size.toLong()) { ByteReadPacket(bytes) },
                Headers.build {
                    append(HttpHeaders.ContentDisposition, "form-data; name=\"file\"; filename=\"$name\"")
                },
            )
        )
        if (!response.success) {
            log.e { "[uploadAsset] ${response.status}" }
            error("[uploadAsset] ${response.status}")
        }
        return response.body()
    }
}

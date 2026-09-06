@file:OptIn(ExperimentalUuidApi::class)

package market.femi.services

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.contentType
import kotlinx.coroutines.delay
import market.femi.models.WordAlignment
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

interface ApiService {                                 // wraps fal/vertex/veo, ElevenLabs, Qwen, LM Studio, flux
    suspend fun forceAlign(lyrics: String, audioName: String): List<WordAlignment>  // Qwen: lyrics + audio → timings
    suspend fun generateImages(prompt: String, refImage: String): List<String> // filenames
    suspend fun generateDraftImage(prompt: String): String // filename
    suspend fun submitVeo(prompt: String, refImages: List<String>): List<String> // filenames
}

fun createRealApiService(kv: KvService, fs: FileService): ApiService = RealRustApiService(kv, fs)

class RealRustApiService(
    private val kv: KvService,
    private val fs: FileService,
    private val client: HttpClient = HttpClient(),
) : ApiService {

    override suspend fun generateDraftImage(prompt: String): String {
        val response = client.post(kv.apiUrl) {
            setBody(MultiPartFormDataContent(formData {
                append("ZImageTurbo[prompt]", prompt)
            }))
        }
        val file = "${Uuid.random()}.${response.contentType()?.contentSubtype ?: error("[generateDraftImage] no Content-Type")}"
        fs.writeBytes(file, response.bodyAsBytes())
        return file
    }

    override suspend fun forceAlign(lyrics: String, audioName: String): List<WordAlignment> = TODO("Not yet implemented")
    override suspend fun generateImages(prompt: String, refImage: String): List<String> = TODO("Not yet implemented")
    override suspend fun submitVeo(prompt: String, refImages: List<String>): List<String> = TODO("Not yet implemented")
}

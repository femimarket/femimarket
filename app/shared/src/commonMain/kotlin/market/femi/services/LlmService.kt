package market.femi.services

import co.touchlab.kermit.Logger
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.api.response.ResponseInput
import com.aallam.openai.api.response.ResponseRequest
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIHost
import io.ktor.client.HttpClient
import kotlinx.coroutines.delay
import market.femi.models.WordAlignment
import kotlin.time.Duration.Companion.minutes

interface LlmService {                                 // wraps fal/vertex/veo, ElevenLabs, Qwen, LM Studio, flux
    suspend fun generateText(prompt: String): String
}


fun createRealLmStudioLlmService(kv: KvService): LlmService = RealLmStudioLlmService(kv)


class RealLmStudioLlmService(
    private val kv: KvService,
    private val log: LogService = createRealLogService("RealLmStudioLlmService")
//    private val openai: OpenAI = OpenAI(
//        token = "your-api-key",
//        host = OpenAIHost(kv.llmUrl),
//    )
) : LlmService {

    private val openai = OpenAI(
        token = "your-api-key",
        host = OpenAIHost(kv.llmUrl),
        timeout = Timeout(socket = 10.minutes),
    )

    override suspend fun generateText(prompt: String): String {
        try {
            val completion = openai.response(
                request = ResponseRequest(
                    model = ModelId("qwen/qwen3.6-35b-a3b"),
                    input = ResponseInput(prompt)
                )
            )
            val res = completion.output.last().content?.last()?.text
            if (res == null) {
                val err = "$prompt produced empty response"
                error(err)
            }
            return res
        } catch (e: Exception) {
            log.e(e) {
                "$prompt produced empty response"
            }
            error(e)
        }
    }

}

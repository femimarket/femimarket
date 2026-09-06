package market.femi.services

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import market.femi.match.apis.ApplicantsApi
import market.femi.match.apis.QuestionnairesApi
import market.femi.match.apis.QuestionsApi
import market.femi.match.apis.SessionAnswersApi
import market.femi.match.apis.SessionsApi
import market.femi.match.apis.TranslationsApi
import market.femi.match.models.ApplicantCreate
import market.femi.match.models.QuestionGet
import market.femi.match.models.QuestionnaireList
import market.femi.match.models.SessionAnswerCreate
import market.femi.match.models.SessionAnswerGet
import market.femi.match.models.SessionGet
import market.femi.match.models.TranslationGet

interface MatchService {
    suspend fun createSession(): String
    suspend fun getSession(sessionId: String): SessionGet
    suspend fun getQuestionnaire(questionnaireId: Int): JsonElement
    suspend fun getQuestionnaires(): List<QuestionnaireList>
    suspend fun getQuestions(questionnaireId: Int): List<QuestionGet>
    suspend fun getTranslations(): List<TranslationGet>
    suspend fun getSessionAnswers(sessionId: String): List<SessionAnswerGet>
    suspend fun createSessionAnswers(sessionId: String, answers: List<SessionAnswerCreate>)
    suspend fun createApplicant(
        sessionId: String,
        firstName: String,
        lastName: String,
        linkedin: String,
    )
}

fun createRealMatchService(kv: KvService): MatchService = RealOpenApiMatchService(kv)

class RealOpenApiMatchService(
    private val kv: KvService,
    private val client: HttpClient = HttpClient {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    },
    private val log: LogService = createRealLogService("RealOpenApiMatchService"),
    private val url: String = "${kv.apiUrl}/api/match",
) : MatchService {

    override suspend fun createSession(): String {
        val response = SessionsApi(url, client).create()
        if (!response.success) {
            log.e { "[createSession] ${response.status}" }
            error("[createSession] ${response.status}")
        }
        return response.body()
    }

    override suspend fun getSession(sessionId: String): SessionGet {
        val response = SessionsApi(url, client).get(sessionId)
        if (!response.success) {
            log.e { "[getSession] ${response.status}" }
            error("[getSession] ${response.status}")
        }
        return response.body()
    }

    override suspend fun getQuestionnaire(questionnaireId: Int): JsonElement {
        val response = client.get("$url/questionnaires/$questionnaireId")
        if (!response.status.isSuccess()) {
            log.e { "[getQuestionnaire] ${response.status}" }
            error("[getQuestionnaire] ${response.status}: ${response.bodyAsText()}")
        }
        return response.body()
    }

    override suspend fun getQuestionnaires(): List<QuestionnaireList> {
        val response = QuestionnairesApi(url, client).list()
        if (!response.success) {
            log.e { "[getQuestionnaires] ${response.status}" }
            error("[getQuestionnaires] ${response.status}")
        }
        return response.body()
    }

    override suspend fun getQuestions(questionnaireId: Int): List<QuestionGet> {
        val response = QuestionsApi(url, client).get(questionnaireId)
        if (!response.success) {
            log.e { "[getQuestions] ${response.status}" }
            error("[getQuestions] ${response.status}")
        }
        return response.body()
    }

    override suspend fun getTranslations(): List<TranslationGet> {
        val response = TranslationsApi(url, client).get()
        if (!response.success) {
            log.e { "[getTranslations] ${response.status}" }
            error("[getTranslations] ${response.status}")
        }
        return response.body()
    }

    override suspend fun getSessionAnswers(sessionId: String): List<SessionAnswerGet> {
        val response = SessionAnswersApi(url, client).get(sessionId)
        if (!response.success) {
            log.e { "[getSessionAnswers] ${response.status}" }
            error("[getSessionAnswers] ${response.status}")
        }
        return response.body()
    }

    override suspend fun createSessionAnswers(sessionId: String, answers: List<SessionAnswerCreate>) {
        val response = SessionAnswersApi(url, client).create(sessionId, answers)
        if (!response.success) {
            log.e { "[createSessionAnswers] ${response.status}" }
            error("[createSessionAnswers] ${response.status}")
        }
    }

    override suspend fun createApplicant(
        sessionId: String,
        firstName: String,
        lastName: String,
        linkedin: String,
    ) {
        val response = ApplicantsApi(url, client).create(
            sessionId,
            ApplicantCreate(
                firstName = firstName,
                lastName = lastName,
                linkedin = linkedin,
            ),
        )
        if (!response.success) {
            log.e { "[createApplicant] ${response.status}" }
            error("[createApplicant] ${response.status}")
        }
    }
}

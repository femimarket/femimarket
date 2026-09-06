package market.femi.services

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import market.femi.care.apis.ShiftsApi
import market.femi.care.apis.UsersApi
import market.femi.care.models.User

@Serializable
data class CandidateName(
    val id: Int,
    @SerialName("candidate_id") val candidateId: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    @SerialName("created_at") val createdAt: String,
)


interface CareService {
    suspend fun createCandidate(): String
    suspend fun createCandidateName(candidateId: String, firstName: String, lastName: String)
    suspend fun getCandidateName(candidateId: String): CandidateName
    suspend fun getShiftsNearby(postcode: String, min: Int, max: Int): String
    suspend fun createUser(user: User): User
    suspend fun getUser(id: String): User
    suspend fun listServiceUsers(): List<User>
    var user: User?
    val users: List<User>
    suspend fun getMe()
}

fun createRealCareService(kv: KvService, http: HttpService): CareService = RealOpenApiCareService(kv, http)

class RealOpenApiCareService(
    private val kv: KvService,
    private val http: HttpService,
    private val log: LogService=createRealLogService("RealOpenApiCareService"),
    private val url: String = "${kv.apiUrl}/api/care"
) : CareService {

    override var user by mutableStateOf<User?>(null)
    override var users by mutableStateOf<List<User>>(emptyList())

    override suspend fun listServiceUsers(): List<User> {
        val response = UsersApi(url, http.client).serviceUsers()
        if (!response.success) {
            log.e { "[listServiceUsers] ${response.status}" }
            error("[listServiceUsers] ${response.status}")
        }
        return response.body()
    }

    override suspend fun getMe() {
        val response = UsersApi(url, http.client).me()
        if (!response.success) {
            log.e { "[getMe] ${response.status}" }
            error("[getMe] ${response.status}")
        }
        users = response.body()
    }

    override suspend fun createCandidate(): String {
        val response = http.client.post("$url/candidates")
        if (!response.status.isSuccess()) {
            error("[createCandidate] ${response.status}: ${response.bodyAsText()}")
        }
        return response.bodyAsText()
    }

    override suspend fun createCandidateName(candidateId: String, firstName: String, lastName: String) {
        val response = http.client.post("$url/candidates/$candidateId/name") {
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("first_name", firstName)
                    put("last_name", lastName)
                },
            )
        }
        if (!response.status.isSuccess()) {
            error("[createCandidateName] ${response.status}: ${response.bodyAsText()}")
        }
    }

    override suspend fun getCandidateName(candidateId: String): CandidateName {
        val response = http.client.get("$url/candidates/$candidateId/name")
        if (!response.status.isSuccess()) {
            error("[getCandidateName] ${response.status}: ${response.bodyAsText()}")
        }
        return response.body()
    }

    override suspend fun getShiftsNearby(postcode: String, min: Int, max: Int): String {
        val response = ShiftsApi(url, http.client).nearby(postcode, min, max)
        if (!response.success) {
            log.e { "[getShiftsNearby] ${response.status}" }
            error("[getShiftsNearby] ${response.status}")
        }
        return response.body()
    }

    override suspend fun createUser(user: User): User {
        val response = UsersApi(url, http.client).create(user)
        if (!response.success) {
            log.e { "[createUser] ${response.status}" }
            error("[createUser] ${response.status}")
        }
        return response.body()
    }

    override suspend fun getUser(id: String): User {
        val response = UsersApi(url, http.client).id(id)
        if (!response.success) {
            log.e { "[getUser] ${response.status}" }
            error("[getUser] ${response.status}")
        }
        return response.body()
    }
}

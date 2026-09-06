package market.femi.services

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.basicAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import market.femi.AppJson
import market.femi.TABLE_AUDIO
import market.femi.TABLE_IMAGE
import market.femi.TABLE_MATRIX
import market.femi.TABLE_VIDEO
import market.femi.models.Audio
import market.femi.models.Image
import market.femi.models.Matrix
import market.femi.models.Video
import market.femi.models.XmpItem

interface DbService {                                   // wraps IDBPDatabase (Idb.kt) + appendData/deleteData (Viewport.kt)
    suspend fun upsert(
        audios: List<Audio> = emptyList(),
        videos: List<Video> = emptyList(),
        images: List<Image> = emptyList(),
        files: List<XmpItem> = emptyList(),
        matrixs: List<Matrix> = emptyList(),
    )
    suspend fun delete(
        audios: List<Audio> = emptyList(),
        videos: List<Video> = emptyList(),
        images: List<Image> = emptyList(),
        files: List<XmpItem> = emptyList(),
        matrixs: List<Matrix> = emptyList(),
        softDelete: Boolean = false,
    )
    suspend fun audios(project: String? = null): List<Audio>
    suspend fun videos(project: String? = null): List<Video>
    suspend fun images(project: String? = null): List<Image>
    suspend fun files(): List<XmpItem>
    suspend fun matrixs(): List<Matrix>
    suspend fun audio(id: String): Audio?
    suspend fun searchAudios(query: String, likedOnly: Boolean, inProjects: Boolean): List<Audio>
    suspend fun searchAudioLyrics(query: String, project: String): List<Audio>
    suspend fun searchImages(query: String): List<Image>
    suspend fun clearAll(): Unit
}

// THIS is what uses expect. We need the platform to provide the real implementation.
fun createRealDbService(kv: KvService, log: LogService): DbService = RealCouchDbService(kv,log)


@Serializable
data class SurrealResponse<T>(
    val result: List<T>,
    val status: String? = null,
    val time: String? = null
)

class RealSurrealDbService(
    val kv: KvService,
    val log: LogService,
    private val namespace: String = "main",
    private val database: String = "main",
    private val authToken: String = "root:root",
) : DbService {

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(AppJson)
        }
        defaultRequest {
            header("Surreal-NS", namespace)
            header("Surreal-DB", database)
            header("Accept", "application/json")

//            if (authToken.contains(":")) {
//                val (user, pass) = authToken.split(":", limit = 2)
//                basicAuth(user, pass)
//            } else {
////                bearerAuth(authToken)
//            }
        }
    }

    override suspend fun upsert(
        audios: List<Audio>,
        videos: List<Video>,
        images: List<Image>,
        files: List<XmpItem>,
        matrixs: List<Matrix>
    ) {
        coroutineScope {
            val jobs = audios.map { async { putItem(TABLE_AUDIO, it.id, it) } } +
                    videos.map { async { putItem(TABLE_VIDEO, it.id, it) } } +
                    images.map { async { putItem(TABLE_IMAGE, it.id, it) } } +
//                    files.map { async { putItem("file", it.id, it) } } +
                    matrixs.map { async { putItem(TABLE_MATRIX, it.id, it) } }

            jobs.awaitAll()
        }
    }

    override suspend fun delete(
        audios: List<Audio>,
        videos: List<Video>,
        images: List<Image>,
        files: List<XmpItem>,
        matrixs: List<Matrix>,
        softDelete: Boolean
    ) {
        coroutineScope {
            val jobs = audios.map { async { deleteItem(TABLE_AUDIO, it.id) } } +
                    videos.map { async { deleteItem(TABLE_VIDEO, it.id) } } +
                    images.map { async { deleteItem(TABLE_IMAGE, it.id) } }
//                    files.map { async { deleteItem("file", it.id) } } +
//                    matrixs.map { async { deleteItem("matrixs", it.id) } }

            jobs.awaitAll()
        }
    }

    override suspend fun audios(project: String?): List<Audio> =
        fetchAll<Audio>(TABLE_AUDIO).filter { project == null || it.project == project }

    override suspend fun videos(project: String?): List<Video> =
        fetchAll<Video>(TABLE_VIDEO).filter { project == null || it.project == project }

    override suspend fun images(project: String?): List<Image> =
        fetchAll<Image>(TABLE_IMAGE).filter { project == null || it.project == project }

    override suspend fun files(): List<XmpItem> = fetchAll("file")

    override suspend fun matrixs(): List<Matrix> = fetchAll(TABLE_MATRIX)

    override suspend fun audio(id: String): Audio? {
        return try {
            val cleanId = id.substringAfter(":")
            val response = client.get("${kv.dbUrl}/key/audio/$cleanId")
            if (response.status.isSuccess()) {
                response.body<List<Audio>>().firstOrNull()
            } else null
        } catch (e: Exception) {
            println("Error fetching audio $id: ${e.message}")
            null
        }
    }

    override suspend fun searchAudios(query: String, likedOnly: Boolean, inProjects: Boolean): List<Audio> {
        return audios(null).filter { it.name.contains(query, ignoreCase = true) }
    }

    override suspend fun searchAudioLyrics(query: String, project: String): List<Audio> {
        var body = ""
        var status = ""
        val safeQuery = query.replace("'", "\\'")
        val safeProject = project.replace("'", "\\'")
        return try {
            val response = client.post("${kv.dbUrl}/sql") {
                contentType(ContentType.Text.Plain)
                setBody("SELECT * FROM $TABLE_AUDIO WHERE lyrics CONTAINS '$safeQuery' AND project CONTAINS '$safeProject';")
            }
            body = response.bodyAsText()
            status = response.status.toString()
            val envelope = response.body<List<SurrealResponse<Audio>>>()
            envelope.first().result
        } catch (e: Exception) {
            error("[RealSurrealDbService:searchAudioLyrics] Failed:$body $status ${e.message}")
        }
    }

    override suspend fun searchImages(query: String): List<Image> {
        return images(null).filter { it.name.contains(query, ignoreCase = true) }
    }

    override suspend fun clearAll() {
        val baseUrl = kv.dbUrl

        // Pass SurrealQL directly to the SQL endpoint
        val query = """
        DELETE audios;
        DELETE videos;
        DELETE images;
        DELETE files;
        DELETE matrixs;
    """.trimIndent()

        val response = client.post("$baseUrl/sql") {
            contentType(ContentType.Text.Plain)
            setBody(query)
        }

        if (!response.status.isSuccess()) {
            error("RealSurrealDbService - Failed to clear database: HTTP ${response.status}")
        }
    }

    // --- Private Helper Methods ---

    private suspend inline fun <reified T> putItem(table: String, id: String, item: T) {
        try {
            val cleanId = id.substringAfter(":")
            // The HTTP /sql endpoint takes RAW SurrealQL as the body — it does NOT support the
            // {query, variables} binding format (that's websocket RPC only). Sending that JSON object
            // made SurrealDB evaluate it as a plain object literal and echo it back with status OK, so
            // the UPSERT never ran and the table was never created. Inline the data as a JSON literal
            // (valid SurrealQL) and send it as text, exactly like searchAudioLyrics does.
            val data = Json.encodeToJsonElement(item).toString()
            val sql = "UPSERT $table:`$cleanId` CONTENT $data;"

            val r = client.post("${kv.dbUrl}/sql") {
                contentType(ContentType.Text.Plain)
                setBody(sql)
            }
            println(r.bodyAsText())
        } catch (e: Exception) {
            error("RealSurrealDbService - Failed to upsert $table/$id: ${e.message}")
        }
    }

    private suspend fun deleteItem(table: String, id: String) {
        try {
            val cleanId = id.substringAfter(":")
            client.delete("${kv.dbUrl}/key/$table/$cleanId")
        } catch (e: Exception) {
            error("RealSurrealDbService - Failed to delete $table/$id: ${e.message}")
        }
    }

    private suspend inline fun <reified T> fetchAll(table: String): List<T> {
        var err = ""
        var status = ""
        return try {
            val response = client.get("${kv.dbUrl}/key/$table")
            err = response.bodyAsText()
            status = response.status.toString()
            val envelope = response.body<List<SurrealResponse<T>>>()
            envelope.first().result
        } catch (e: Exception) {
            if (err.contains("The table '$table' does not exist")) {
                log.w {
                    "table $table doesn't exist"
                }
            } else {
                log.e(e, tag = "RealSurrealDbService:fetchAll") {
                    "body=$err, status=$status"
                }
            }
            emptyList()
        }
    }
}


// CouchDB over its plain HTTP API: ONE database, every doc carries a "doc_type" field
// (audios/videos/images/matrixs) as the table discriminator, doc _id = the model's id.
// CouchDB demands the current _rev to overwrite, so putItem fetches it first; AppJson's
// ignoreUnknownKeys lets docs decode straight into the models despite the extra
// _id/_rev/doc_type keys. Reads pull _all_docs and filter by doc_type in memory.
class RealCouchDbService(
    val kv: KvService,
    val log: LogService,
) : DbService {
    // get() so this reads the CURRENT kv value per call — `=` would snapshot it at construction,
    // before SetupDialog/tests have set it, freezing "" into every URL.
    private val database: String get() = kv.dbUsername
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(AppJson)
        }
        defaultRequest {
            basicAuth(kv.dbUsername, kv.dbPass)
        }
    }

    override suspend fun upsert(
        audios: List<Audio>,
        videos: List<Video>,
        images: List<Image>,
        files: List<XmpItem>,
        matrixs: List<Matrix>
    ) {
        coroutineScope {
            val jobs = audios.map { async { putItem(TABLE_AUDIO, it.id, it) } } +
                    videos.map { async { putItem(TABLE_VIDEO, it.id, it) } } +
                    images.map { async { putItem(TABLE_IMAGE, it.id, it) } } +
                    matrixs.map { async { putItem(TABLE_MATRIX, it.id, it) } }

            jobs.awaitAll()
        }
    }

    override suspend fun delete(
        audios: List<Audio>,
        videos: List<Video>,
        images: List<Image>,
        files: List<XmpItem>,
        matrixs: List<Matrix>,
        softDelete: Boolean
    ) {
        coroutineScope {
            val jobs = audios.map { async { deleteItem(TABLE_AUDIO, it.id) } } +
                    videos.map { async { deleteItem(TABLE_VIDEO, it.id) } } +
                    images.map { async { deleteItem(TABLE_IMAGE, it.id) } }

            jobs.awaitAll()
        }
    }

    override suspend fun audios(project: String?): List<Audio> =
        fetchAll<Audio>(TABLE_AUDIO).filter { project == null || it.project == project }

    override suspend fun videos(project: String?): List<Video> =
        fetchAll<Video>(TABLE_VIDEO).filter { project == null || it.project == project }

    override suspend fun images(project: String?): List<Image> =
        fetchAll<Image>(TABLE_IMAGE).filter { project == null || it.project == project }

    override suspend fun files(): List<XmpItem> = emptyList()

    override suspend fun matrixs(): List<Matrix> = fetchAll(TABLE_MATRIX)

    override suspend fun audio(id: String): Audio? {
        return try {
            val response = client.get("${kv.dbUrl}/$database/$id")
            if (response.status.isSuccess()) {
                AppJson.decodeFromString(Audio.serializer(), response.bodyAsText())
            } else null
        } catch (e: Exception) {
            log.e(e, tag = "RealCouchDbService:audio") { "id=$id" }
            null
        }
    }

    override suspend fun searchAudios(query: String, likedOnly: Boolean, inProjects: Boolean): List<Audio> =
        audios(null).filter { it.name.contains(query, ignoreCase = true) }

    override suspend fun searchAudioLyrics(query: String, project: String): List<Audio> =
        audios(null).filter {
            (it.lyrics?.contains(query, ignoreCase = true) ?: query.isEmpty()) &&
                    it.project.contains(project, ignoreCase = true)
        }

    override suspend fun searchImages(query: String): List<Image> =
        images(null).filter { it.name.contains(query, ignoreCase = true) }

    override suspend fun clearAll() {
        client.delete("${kv.dbUrl}/$database")
        client.put("${kv.dbUrl}/$database")
    }

    // --- Private Helper Methods ---

    private suspend inline fun <reified T> putItem(table: String, id: String, item: T) {
        try {
            val doc = AppJson.encodeToJsonElement(item).jsonObject.toMutableMap()
            doc["_id"] = JsonPrimitive(id)
            doc["doc_type"] = JsonPrimitive(table)
            val existing = client.get("${kv.dbUrl}/$database/$id")
            when {
                existing.status.isSuccess() -> {
                    doc["_rev"] = AppJson.parseToJsonElement(existing.bodyAsText()).jsonObject.getValue("_rev")
                    requireNotNull(doc["_rev"])
                }
                existing.status != HttpStatusCode.NotFound ->      // ONLY 404 means "not there" — anything else is a real failure
                    error("HTTP ${existing.status}: ${existing.bodyAsText()}")
            }
            val r = client.put("${kv.dbUrl}/$database/$id") {
                contentType(ContentType.Application.Json)
                setBody(JsonObject(doc).toString())
            }
            if (!r.status.isSuccess()) error("HTTP ${r.status}: ${r.bodyAsText()}")
        } catch (e: Exception) {
            error("RealCouchDbService - Failed to upsert $table/$id: ${e.message}")
        }
    }

    private suspend fun deleteItem(table: String, id: String) {
        try {
            val existing = client.get("${kv.dbUrl}/$database/$id")
            if (!existing.status.isSuccess()) return
            val rev = AppJson.parseToJsonElement(existing.bodyAsText()).jsonObject.getValue("_rev").jsonPrimitive.content
            client.delete("${kv.dbUrl}/$database/$id") { parameter("rev", rev) }
        } catch (e: Exception) {
            error("RealCouchDbService - Failed to delete $table/$id: ${e.message}")
        }
    }

    private suspend inline fun <reified T> fetchAll(table: String): List<T> {
        var err = ""
        var status = ""
        return try {
            val response = client.get("${kv.dbUrl}/$database/_all_docs") { parameter("include_docs", "true") }
            err = response.bodyAsText()
            status = response.status.toString()
            AppJson.parseToJsonElement(err).jsonObject.getValue("rows").jsonArray
                .map { it.jsonObject.getValue("doc").jsonObject }
                .filter { it["doc_type"]?.jsonPrimitive?.content == table }
                .map { AppJson.decodeFromJsonElement<T>(it) }
        } catch (e: Exception) {
            if (err.contains("not_found")) {
                log.w {
                    "database $table doesn't exist"
                }
            } else {
                log.e(e, tag = "RealCouchDbService:fetchAll") {
                    "body=$err, status=$status"
                }
            }
            emptyList()
        }
    }
}


class FakeDbService : DbService {
    private val a = linkedMapOf<String, Audio>()
    private val v = linkedMapOf<String, Video>()
    private val i = linkedMapOf<String, Image>()
    override suspend fun upsert(audios: List<Audio>, videos: List<Video>, images: List<Image>, files: List<XmpItem>, matrixs: List<Matrix>) {
        audios.forEach { a[it.id] = it }; videos.forEach { v[it.id] = it }; images.forEach { i[it.id] = it }
    }
    override suspend fun delete(audios: List<Audio>, videos: List<Video>, images: List<Image>, files: List<XmpItem>, matrixs: List<Matrix>, softDelete: Boolean) {
        audios.forEach { a.remove(it.id) }; videos.forEach { v.remove(it.id) }; images.forEach { i.remove(it.id) }
    }
    override suspend fun audios(project: String?): List<Audio> = a.values.filter { project == null || it.project == project }
    override suspend fun videos(project: String?): List<Video> = v.values.filter { project == null || it.project == project }
    // HONOR the project filter, mirroring videos(project) above (F19: FAKE = real in-memory behavior,
    // faithful to the real repo's project scoping) — the real repo scopes images by
    // project, so a selectAudio re-read of projectImages surfaces only THIS project's frames. (Previously
    // returned every image regardless of project, which would hide a project-scoping regression.)
    override suspend fun images(project: String?): List<Image> = i.values.filter { project == null || it.project == project }
    override suspend fun files(): List<XmpItem> = emptyList()
    override suspend fun matrixs(): List<Matrix> = emptyList()
    override suspend fun audio(id: String): Audio? = a[id]
    override suspend fun searchAudios(query: String, likedOnly: Boolean, inProjects: Boolean): List<Audio> =
        a.values.filter { it.name.contains(query, ignoreCase = true) }

    override suspend fun searchAudioLyrics(query: String, project: String): List<Audio> =
        a.values.filter {
            (it.lyrics?.contains(query, ignoreCase = true) ?: query.isEmpty()) &&
                    it.project.contains(project, ignoreCase = true)
        }

    // Faithful in-memory image search (mirrors searchAudios): filter the stored images by name so the
    // protagonist picker's onSearch resolves to real, already file-backed library rows (GAP-23 backing).
    override suspend fun searchImages(query: String): List<Image> =
        i.values.filter { it.name.contains(query, ignoreCase = true) }

    override suspend fun clearAll() {
        TODO("Not yet implemented")
    }
}
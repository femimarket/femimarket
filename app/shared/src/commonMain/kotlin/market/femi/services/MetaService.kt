package market.femi.services

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import market.femi.AppJson
import market.femi.models.AudioLine
import market.femi.models.AudioQA
import market.femi.models.WordAlignment
import market.femi.models.XmpItem

interface MetaService {                                   // wraps wasm id3/xmp/infer
    suspend fun inferExtension(filename: String): String
    // One read per Audio field — GET /audio/<field>/{file} on the meta server, typed:
    // null (or empty list) when the frame is absent. Reads the source of truth (the TXXX
    // custom frame) only — the interoperable frames are outbound copies for other apps.
    suspend fun readAudioId(file: String): String?
    suspend fun readAudioBackedUp(file: String): Boolean?
    suspend fun readAudioName(file: String): String?
    suspend fun readAudioError(file: String): String?
    suspend fun readAudioGenre(file: String): String?
    suspend fun readAudioImage(file: String): String?
    suspend fun readAudioLike(file: String): Boolean?
    suspend fun readAudioLyrics(file: String): String?
    suspend fun readAudioEditedLyrics(file: String): String?
    suspend fun readAudioElevenLabsForcedAlignment(file: String): com.example.elevenlabs.models.ForcedAlignmentResponseModel?
    suspend fun readAudioProtagonist(file: String): String?
    suspend fun readAudioProject(file: String): String?
    suspend fun readAudioUid(file: String): String?
    suspend fun readAudioAudioLines(file: String): List<AudioLine>
    suspend fun readAudioWordAlignments(file: String): List<WordAlignment>
    suspend fun readAudioFaqs(file: String): List<AudioQA>
    suspend fun readAudioSocialMediaBlueprint(file: String): String?
    suspend fun readAudioVideo(file: String): String?
    suspend fun readAudioLyricTokens(file: String): List<String>
    // One write per Audio field — POST /audio/<field> on the meta server. The id3 TXXX
    // custom frame is the source of truth; fields with an interoperable frame (TCON, TALB,
    // USLT, SYLT, APIC) also get that proper place written server-side for other apps.
    // `value` is the string form (JSON string for list/object fields, image filename for protagonist).
    suspend fun writeAudioId(file: String, value: String)
    suspend fun writeAudioBackedUp(file: String, value: String)
    suspend fun writeAudioName(file: String, value: String)
    suspend fun writeAudioError(file: String, value: String)
    suspend fun writeAudioGenre(file: String, value: String)
    suspend fun writeAudioImage(file: String, value: String)
    suspend fun writeAudioLike(file: String, value: String)
    suspend fun writeAudioLyrics(file: String, value: String)
    suspend fun writeAudioEditedLyrics(file: String, value: String)
    suspend fun writeAudioElevenLabsForcedAlignment(file: String, value: String)
    suspend fun writeAudioProtagonist(file: String, value: String)
    suspend fun writeAudioProject(file: String, value: String)
    suspend fun writeAudioUid(file: String, value: String)
    suspend fun writeAudioAudioLines(file: String, value: String)
    suspend fun writeAudioWordAlignments(file: String, value: String)
    suspend fun writeAudioFaqs(file: String, value: String)
    suspend fun writeAudioSocialMediaBlueprint(file: String, value: String)
    suspend fun writeAudioVideo(file: String, value: String)
    suspend fun writeAudioLyricTokens(file: String, value: String)
}

fun createRealMetaService(kv: KvService, fs: FileService): MetaService = RealWebhookMetaService(kv,fs)

class RealWebhookMetaService(
    private val kv: KvService,
    private val fs: FileService,
    private val client: HttpClient = HttpClient {
        install(ContentNegotiation) {
            // adnanh/webhook doesn't do content negotiation — it always responds
            // `text/plain` regardless of Accept headers, even though the body is
            // valid JSON (confirmed via curl against the live meta service).
            json(AppJson)
            json(AppJson, contentType = ContentType.Text.Plain)
        }
    }
): MetaService {

//    override suspend fun readId3(filename: String): Id3Tags  {
//        val response = client.get("${kv.metaUrl}/hooks/read_id3") {
//            parameter("file", filename)
//        }.body<Id3ReadResponse>()
//
//        val coverImage = runCatching {
//            client.get("${kv.metaUrl}/hooks/extract_cover_image") {
//                parameter("file", filename)
//            }.bodyAsText().trim().takeIf { it.endsWith(".png") || it.endsWith(".jpg") }
//        }.getOrNull()
//
//        val protagonistImage = runCatching {
//            client.get("${kv.metaUrl}/hooks/extract_protagonist_image") {
//                parameter("file", filename)
//            }.bodyAsText().trim().takeIf { it.endsWith(".png") || it.endsWith(".jpg") }
//        }.getOrNull()
//
//        return Id3Tags(
//            lyrics = response.lyrics,
//            album = response.album,
//            genre = response.genre,
//            sunoId = response.sunoId,
//            coverImage = coverImage,
//            editedLyrics = response.editedLyrics,
//            editedSyncedLyrics = response.editedSyncedLyrics,
//            protagonistImage = protagonistImage,
//        )
//    }
//
//
//    /**
//     * REAL native XMP read (U51 "+ XMP") — mirrors the webMain `XmpItem.fromXmp` factory
//     * field-for-field: `xmpMM:OriginalDocumentID` is REQUIRED (a file with no XMP packet, or a
//     * packet without that tag, throws — the import fan-out wraps `readXmp` in `runCatching` and
//     * treats the failure as "no XMP for this file", exactly like the web path's thrown
//     * extraction/parsing errors); genre / lyrics / rating / projectName are optional and take
//     * the model's defaults when absent, with the web's `toIntOrNull() ?: 0` rating parse.
//     */
//    override suspend fun readXmp(filename: String): XmpItem {
//        val response = client.get("${kv.metaUrl}/hooks/read_xmp") {
//            parameter("file", filename)
//        }.body<List<XmpExifToolResponse>>().first()
//
//        val originalDocumentId = response.originalDocumentId
//            ?: throw IllegalArgumentException(
//                "XMP parsing failed for '$filename': the file has no XMP packet, or the required " +
//                        "'xmpMM:OriginalDocumentID' tag is missing from it.",
//            )
//
//        return XmpItem(
//            mmOriginalDocumentID = originalDocumentId,
//            name = filename,
//            dmGenre = response.genre,
//            rating = response.rating?.toIntOrNull() ?: 0,
//            dmLyrics = response.lyrics,
//            projectName = response.projectName ?: "",
//        )
//    }

    override suspend fun inferExtension(filename: String): String = withContext(Dispatchers.Default) {
//        Ffi.inferExtension(bytes) ?: error("[inferExtension] unknown")
        TODO("Not yet implemented")
    }

//    override suspend fun writeProtagonist(audioFilename: String, imageFilename: String) {
//        client.get("${kv.metaUrl}/hooks/write_protagonist_image") {
//            parameter("file", audioFilename)
//            parameter("image", imageFilename)
//        }
//    }

    // plain text: the body IS the value; 404 = absent
    override suspend fun readAudioId(file: String): String? {
        val r = client.get("${kv.metaUrl}/audio/id/$file")
        return if (r.status == HttpStatusCode.NotFound) null else r.bodyAsText()
    }

    // plain text "true"/"false"; 404 = absent
    override suspend fun readAudioBackedUp(file: String): Boolean? {
        val r = client.get("${kv.metaUrl}/audio/backedUp/$file")
        return if (r.status == HttpStatusCode.NotFound) null else r.bodyAsText() == "true"
    }

    // plain text: the body IS the value; 404 = absent
    override suspend fun readAudioName(file: String): String? {
        val r = client.get("${kv.metaUrl}/audio/name/$file")
        return if (r.status == HttpStatusCode.NotFound) null else r.bodyAsText()
    }

    // plain text: the body IS the value; 404 = absent
    override suspend fun readAudioError(file: String): String? {
        val r = client.get("${kv.metaUrl}/audio/error/$file")
        return if (r.status == HttpStatusCode.NotFound) null else r.bodyAsText()
    }

    // plain text: the body IS the value; 404 = absent
    override suspend fun readAudioGenre(file: String): String? {
        val r = client.get("${kv.metaUrl}/audio/genre/$file")
        return if (r.status == HttpStatusCode.NotFound) null else r.bodyAsText()
    }

    // plain text: the body IS the value; 404 = absent
    override suspend fun readAudioImage(file: String): String? {
        val r = client.get("${kv.metaUrl}/audio/image/$file")
        return if (r.status == HttpStatusCode.NotFound) null else r.bodyAsText()
    }

    // plain text "true"/"false"; 404 = absent
    override suspend fun readAudioLike(file: String): Boolean? {
        val r = client.get("${kv.metaUrl}/audio/like/$file")
        return if (r.status == HttpStatusCode.NotFound) null else r.bodyAsText() == "true"
    }

    // plain text: the body IS the value; 404 = absent
    override suspend fun readAudioLyrics(file: String): String? {
        val r = client.get("${kv.metaUrl}/audio/lyrics/$file")
        return if (r.status == HttpStatusCode.NotFound) null else r.bodyAsText()
    }

    // plain text: the body IS the value; 404 = absent
    override suspend fun readAudioEditedLyrics(file: String): String? {
        val r = client.get("${kv.metaUrl}/audio/editedLyrics/$file")
        return if (r.status == HttpStatusCode.NotFound) null else r.bodyAsText()
    }

    // JSON object; null when absent
    override suspend fun readAudioElevenLabsForcedAlignment(file: String): com.example.elevenlabs.models.ForcedAlignmentResponseModel? =
        client.get("${kv.metaUrl}/audio/elevenLabsForcedAlignment/$file").body()

    // plain text: the body IS the value; 404 = absent
    override suspend fun readAudioProtagonist(file: String): String? {
        val r = client.get("${kv.metaUrl}/audio/protagonist/$file")
        return if (r.status == HttpStatusCode.NotFound) null else r.bodyAsText()
    }

    // plain text: the body IS the value; 404 = absent
    override suspend fun readAudioProject(file: String): String? {
        val r = client.get("${kv.metaUrl}/audio/project/$file")
        return if (r.status == HttpStatusCode.NotFound) null else r.bodyAsText()
    }

    // plain text: the body IS the value; 404 = absent
    override suspend fun readAudioUid(file: String): String? {
        val r = client.get("${kv.metaUrl}/audio/uid/$file")
        return if (r.status == HttpStatusCode.NotFound) null else r.bodyAsText()
    }

    // JSON array; empty when absent
    override suspend fun readAudioAudioLines(file: String): List<AudioLine> =
        client.get("${kv.metaUrl}/audio/audioLines/$file").body()

    // JSON array; empty when absent
    override suspend fun readAudioWordAlignments(file: String): List<WordAlignment> =
        client.get("${kv.metaUrl}/audio/wordAlignments/$file").body()

    // JSON array; empty when absent
    override suspend fun readAudioFaqs(file: String): List<AudioQA> =
        client.get("${kv.metaUrl}/audio/faqs/$file").body()

    // plain text: the body IS the value; 404 = absent
    override suspend fun readAudioSocialMediaBlueprint(file: String): String? {
        val r = client.get("${kv.metaUrl}/audio/socialMediaBlueprint/$file")
        return if (r.status == HttpStatusCode.NotFound) null else r.bodyAsText()
    }

    // plain text: the body IS the value; 404 = absent
    override suspend fun readAudioVideo(file: String): String? {
        val r = client.get("${kv.metaUrl}/audio/video/$file")
        return if (r.status == HttpStatusCode.NotFound) null else r.bodyAsText()
    }

    // JSON array; empty when absent
    override suspend fun readAudioLyricTokens(file: String): List<String> =
        client.get("${kv.metaUrl}/audio/lyricTokens/$file").body()

    private suspend fun postAudio(field: String, file: String, value: String) {
        client.post("${kv.metaUrl}/audio/$field") {
            contentType(ContentType.Application.Json)
            setBody(AudioWrite(file, value))
        }
    }

    override suspend fun writeAudioId(file: String, value: String) = postAudio("id", file, value)
    override suspend fun writeAudioBackedUp(file: String, value: String) = postAudio("backedUp", file, value)
    override suspend fun writeAudioName(file: String, value: String) = postAudio("name", file, value)
    override suspend fun writeAudioError(file: String, value: String) = postAudio("error", file, value)
    override suspend fun writeAudioGenre(file: String, value: String) = postAudio("genre", file, value)
    override suspend fun writeAudioImage(file: String, value: String) = postAudio("image", file, value)
    override suspend fun writeAudioLike(file: String, value: String) = postAudio("like", file, value)
    override suspend fun writeAudioLyrics(file: String, value: String) = postAudio("lyrics", file, value)
    override suspend fun writeAudioEditedLyrics(file: String, value: String) = postAudio("editedLyrics", file, value)
    override suspend fun writeAudioElevenLabsForcedAlignment(file: String, value: String) = postAudio("elevenLabsForcedAlignment", file, value)
    override suspend fun writeAudioProtagonist(file: String, value: String) = postAudio("protagonist", file, value)
    override suspend fun writeAudioProject(file: String, value: String) = postAudio("project", file, value)
    override suspend fun writeAudioUid(file: String, value: String) = postAudio("uid", file, value)
    override suspend fun writeAudioAudioLines(file: String, value: String) = postAudio("audioLines", file, value)
    override suspend fun writeAudioWordAlignments(file: String, value: String) = postAudio("wordAlignments", file, value)
    override suspend fun writeAudioFaqs(file: String, value: String) = postAudio("faqs", file, value)
    override suspend fun writeAudioSocialMediaBlueprint(file: String, value: String) = postAudio("socialMediaBlueprint", file, value)
    override suspend fun writeAudioVideo(file: String, value: String) = postAudio("video", file, value)
    override suspend fun writeAudioLyricTokens(file: String, value: String) = postAudio("lyricTokens", file, value)
}

// Body of every /audio/<field> write on the meta server.
@Serializable
private data class AudioWrite(val file: String, val value: String)

/** Pure carrier for the id3 tags MetadataService extracts (mirrors the wasm Id3Item, WasmId3.kt). */
@Serializable
data class Id3Tags(
    val lyrics: String?,                       // ORIGINAL — USLT, descriptor "" (import reads this)
    val album: String?,
    val genre: String?,
    val sunoId: String?,
    val coverImage: String?,
    // APIC LeadArtist picture, description "protagonist" (wasm-id3 lib.rs) — same shape as coverImage
    val protagonistImage: String?,
    val editedLyrics: String?,                        // edited text — USLT, descriptor "edited"
    val editedSyncedLyrics: List<WordAlignment>?,     // edited + aligned — SYLT, descriptor "edited"
)

// Maps directly to id3-read's JSON output (rust/id3-read) — one object per call, no
// array-wrapping, editedSyncedLyrics is a real nested array (not a JSON string to re-decode).
@Serializable
private data class Id3ReadResponse(
    val album: String? = null,
    val genre: String? = null,
    val lyrics: String? = null,
    val editedLyrics: String? = null,
    val editedSyncedLyrics: List<WordAlignment>? = null,
    val sunoId: String? = null,
)

// Maps strictly to your "read_xmp" hook
@Serializable
private data class XmpExifToolResponse(
    @SerialName("OriginalDocumentID") val originalDocumentId: String? = null,
    @SerialName("Genre") val genre: String? = null,
    @SerialName("Lyrics") val lyrics: String? = null,
    @SerialName("ProjectName") val projectName: String? = null,
    @SerialName("Rating") val rating: String? = null,
)

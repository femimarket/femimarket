package market.femi.services

import androidx.lifecycle.ViewModel
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import market.femi.AUDIO_FILE
import market.femi.DirRef
import market.femi.MemoryFile
import market.femi.PROTAGONIST_IMAGE_BYTES
import market.femi.PROTAGONIST_IMAGE_FILE
import market.femi.REPICK_IMAGE_BYTES
import market.femi.REPICK_IMAGE_FILE
import market.femi.SONG_BYTES

expect fun createRealFsService(kv: KvService, importDir: String? = null): FileService
fun createRealFsService2(kv: KvService) = RealDufsFsService(kv)

// The real local store: a directory on disk reached through kotlinx-io (jvm/ios/android). The
// directory IS the library — media files plus db.json live side by side (FS-as-truth). Web gets
// its own OPFS-backed actual in webMain.
class RealKotlinxIoFsService(root: String) : FileService {
    private val root = Path(root)
    init { SystemFileSystem.createDirectories(this.root) }

    override suspend fun importDirectory(): List<String> =
        SystemFileSystem.list(root)
            .filter { SystemFileSystem.metadataOrNull(it)?.isRegularFile == true }
            .map { it.name }

    override suspend fun writeBytes(name: String, bytes: ByteArray) =
        SystemFileSystem.sink(Path(root, name)).buffered().use { it.write(bytes) }

    override suspend fun readBytes(name: String): ByteArray =
        SystemFileSystem.source(Path(root, name)).buffered().use { it.readByteArray() }

    override suspend fun exists(name: String): Boolean = SystemFileSystem.exists(Path(root, name))

    override suspend fun delete(name: String) = SystemFileSystem.delete(Path(root, name), mustExist = false)

    override suspend fun search(name: String): List<String> =
        importDirectory().filter { it.contains(name, ignoreCase = true) }

    override suspend fun pickImages(): List<MemoryFile> = TODO("Not yet implemented")

    override suspend fun pickDirectory(): DirRef? = TODO("Not yet implemented")

    override fun objectUrl(name: String): String = TODO("Not yet implemented")

    override fun revokeUrl(url: String) { TODO("Not yet implemented") }

    override suspend fun saveAs(name: String, bytes: ByteArray) = writeBytes(name, bytes)

    override suspend fun restoreDirectory(): DirRef? = TODO("Not yet implemented")

    override suspend fun source(name: String): RawSource = SystemFileSystem.source(Path(root, name))

    override suspend fun commitScratch(scratchPath: String, name: String) =
        SystemFileSystem.atomicMove(Path(root, scratchPath), Path(root, name))
}


interface FileService {                                         // a named byte store; the web adapter backs it with the picked FS directory
//    var dir: DirRef?
    /** The user picks a directory; returns its filenames. Where the bytes live (FS, OPFS) is the adapter's call. */
    suspend fun importDirectory(): List<String>
    suspend fun writeBytes(name: String, bytes: ByteArray)
    suspend fun readBytes(name: String): ByteArray
    suspend fun exists(name: String): Boolean
    suspend fun delete(name: String)
    suspend fun search(name: String): List<String>
    /** The user presses "pick image" → the OS picker filtered to images. Typed per kind (not a generic
     *  pickFiles) so an image control can never return an audio/video; other kinds get their own picker. */
    suspend fun pickImages(): List<MemoryFile>
    suspend fun pickDirectory(): DirRef?
    fun objectUrl(name: String): String
    fun revokeUrl(url: String)

    suspend fun saveAs(name: String, bytes: ByteArray)
    suspend fun restoreDirectory(): DirRef?
    suspend fun source(name: String): RawSource
    suspend fun commitScratch(scratchPath: String, name: String)
}

class RealDufsFsService(
    private val kv: KvService,
    private val client: HttpClient = HttpClient()
) : FileService, ViewModel() {           // 'by delegate' forwards all Settings methods automatically

    override suspend fun search(name: String): List<String> {
        val response = client.get("${kv.fsUrl}?q=$name&simple").bodyAsText()
        return response
            .lines()
            .filter { it.isNotBlank() && !it.endsWith("/") }
    }


    override suspend fun importDirectory(): List<String> {
        val url = kv.fsUrl
        val response = client.get("$url?simple").bodyAsText()
        return response
            .lines()
            .filter { it.isNotBlank() && !it.endsWith("/") }
    }

    override suspend fun writeBytes(name: String, bytes: ByteArray): Unit  {
        val url = kv.fsUrl
        client.put("$url/$name") {
            setBody(bytes)
        }
    }

    override suspend fun readBytes(name: String): ByteArray {
        val url = kv.fsUrl
        val response = client.get("$url/$name")
        if (!response.status.isSuccess()) error("no file: $name (${response.status})")
        return response.bodyAsBytes()
    }

    override suspend fun exists(name: String): Boolean {
        val url = kv.fsUrl
        val response = client.head("$url/$name")
        return response.status.isSuccess()
    }

    override suspend fun delete(name: String): Unit  {
        val url = kv.fsUrl
        client.delete("$url/$name")
    }

    override suspend fun pickImages(): List<MemoryFile> {
        TODO("Not yet implemented")
    }

    override suspend fun pickDirectory(): DirRef? {
        TODO("Not yet implemented")
    }

    override fun objectUrl(name: String): String {
        TODO("Not yet implemented")
    }

    override fun revokeUrl(url: String) {
        TODO("Not yet implemented")
    }

    override suspend fun saveAs(name: String, bytes: ByteArray) {
        TODO("Not yet implemented")
    }

    override suspend fun restoreDirectory(): DirRef? {
        TODO("Not yet implemented")
    }

    override suspend fun source(name: String): RawSource {
        TODO("Not yet implemented")
    }

    override suspend fun commitScratch(scratchPath: String, name: String) {
        TODO("Not yet implemented")
    }


}


class FakeFileService(
    private val restorableDirectory: DirRef? = null,
    private val kv: KvService = FakeKvService(),
) : FileService {

    override suspend fun search(name: String): List<String> =
        store.keys.filter { it.contains(name, ignoreCase = true) }.toList()
    private val store = LinkedHashMap(mapOf(AUDIO_FILE to SONG_BYTES))      // the picked directory IS the store (born non-empty)
    private val pickQueue = ArrayDeque(listOf(
        MemoryFile(PROTAGONIST_IMAGE_FILE, PROTAGONIST_IMAGE_BYTES),
        MemoryFile(REPICK_IMAGE_FILE, REPICK_IMAGE_BYTES),
    ))
    override suspend fun importDirectory(): List<String> = store.keys.toList()   // user picked it; its files are already here
    override suspend fun writeBytes(name: String, bytes: ByteArray) { store[name] = bytes }
    override suspend fun readBytes(name: String): ByteArray = store[name] ?: error("no file: $name")
    override suspend fun exists(name: String): Boolean = name in store
    override suspend fun delete(name: String) { store.remove(name) }
    // Consume ONE queued pick per press (PLAN §6.2's "consumed queue"); an exhausted queue models the
    // user cancelling the picker (no selection), which pickProtagonist already treats as a no-op.
    override suspend fun pickImages(): List<MemoryFile> =
        pickQueue.removeFirstOrNull()?.let { listOf(it) } ?: emptyList()
    override suspend fun pickDirectory(): DirRef? = null
    override fun objectUrl(name: String): String = "blob:fake/$name"
    override fun revokeUrl(url: String) {}
    // A "save to the user" gesture. Under FS-as-truth there is no separate download sink to model, so
    // the saved bytes land in the same store (observable via readBytes/exists) — enough for a VM that
    // calls saveAs to be exercised without a browser anchor (GAP-6 backing).
    override suspend fun saveAs(name: String, bytes: ByteArray) { store[name] = bytes }

    // ROW 4 — silent boot reconnect: hand back the ctor-seeded DirRef (null = fresh, no folder to
    // restore). The VM's restoreDirectory intent flips isDirectoryConnected=true when this is non-null.
    override suspend fun restoreDirectory(): DirRef? = restorableDirectory

    // ROW 5 — read-only streaming accessor: a kotlinx.io Buffer over the in-memory bytes (a Buffer IS a
    // RawSource). Byte/ktor-free so the journey stays green; the force-align UPLOAD reads through this.
    override suspend fun source(name: String): RawSource =
        Buffer().apply { write(store[name] ?: error("no file: $name")) }

    // ROW 5 — atomic scratch→store commit: move the scratch entry into the store map (the veo/flux
    // DOWNLOAD lands its bytes under a scratch name, then commits them to the final store name). A
    // missing scratch entry is a no-op-with-error to surface a real mis-commit rather than pass silently.
    override suspend fun commitScratch(scratchPath: String, name: String) {
        val bytes = store.remove(scratchPath) ?: error("no scratch file: $scratchPath")
        store[name] = bytes
    }
}





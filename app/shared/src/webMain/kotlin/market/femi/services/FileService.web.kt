package market.femi.services

import kotlinx.coroutines.await
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import market.femi.DirRef
import market.femi.MemoryFile
import market.femi.deleteFile
import market.femi.getFileHandle
import market.femi.getFileOptions
import market.femi.getOpfs
import market.femi.pickFiles
import market.femi.readDirFiles
import market.femi.readFileBytes
import market.femi.toArrayBuffer
import market.femi.toByteArray

actual fun createRealFsService(kv: KvService, importDir: String?): FileService = RealOpfsFsService()

// OPFS-backed FileService (browser): JsCode.kt owns the raw OPFS interop; this adapts it to the port.
class RealOpfsFsService : FileService {

    override suspend fun importDirectory(): List<String> = readDirFiles()

    override suspend fun writeBytes(name: String, bytes: ByteArray) {
        val writable = getFileHandle(name).createWritable().await()
        writable.write(bytes.toArrayBuffer()).await()
        writable.close().await()
    }

    override suspend fun readBytes(name: String): ByteArray = readFileBytes(name).toByteArray()

    override suspend fun exists(name: String): Boolean =
        runCatching { getOpfs().getFileHandle(name, getFileOptions(false)).await() }.isSuccess

    override suspend fun delete(name: String) = deleteFile(name)

    override suspend fun search(name: String): List<String> =
        readDirFiles().filter { it.contains(name, ignoreCase = true) }

    override suspend fun pickImages(): List<MemoryFile> =
        pickFiles().map { MemoryFile(it, readBytes(it)) }

    override suspend fun pickDirectory(): DirRef? = TODO("Not yet implemented")

    override fun objectUrl(name: String): String = TODO("Not yet implemented")

    override fun revokeUrl(url: String) { TODO("Not yet implemented") }

    override suspend fun saveAs(name: String, bytes: ByteArray) = writeBytes(name, bytes)

    override suspend fun restoreDirectory(): DirRef? = TODO("Not yet implemented")

    override suspend fun source(name: String): RawSource = Buffer().apply { write(readBytes(name)) }

    override suspend fun commitScratch(scratchPath: String, name: String) {
        writeBytes(name, readBytes(scratchPath))
        deleteFile(scratchPath)
    }
}

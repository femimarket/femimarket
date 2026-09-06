@file:OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)

package market.femi

import kotlinx.cinterop.*
import kotlin.experimental.ExperimentalNativeApi
//
//actual object Ffi {
//
//
//    // ── id3 reads ───────────────────────────────────────────────────────────────
//
//    actual fun id3ReadLyrics(bytes: ByteArray): String? = readCBufferWithBytes(bytes) { inPtr, len, buf, cap ->
//        ffi.id3_read_lyrics(inPtr, len, buf, cap)
//    }
//
//    actual fun id3ReadEditedLyrics(bytes: ByteArray): String? = readCBufferWithBytes(bytes) { inPtr, len, buf, cap ->
//        ffi.id3_read_edited_lyrics(inPtr, len, buf, cap)
//    }
//
//    actual fun id3ReadAlbum(bytes: ByteArray): String? = readCBufferWithBytes(bytes) { inPtr, len, buf, cap ->
//        ffi.id3_read_album(inPtr, len, buf, cap)
//    }
//
//    actual fun id3ReadGenre(bytes: ByteArray): String? = readCBufferWithBytes(bytes) { inPtr, len, buf, cap ->
//        ffi.id3_read_genre(inPtr, len, buf, cap)
//    }
//
//    actual fun id3ReadSunoClipId(bytes: ByteArray): String? = readCBufferWithBytes(bytes) { inPtr, len, buf, cap ->
//        ffi.id3_read_suno_clip_id(inPtr, len, buf, cap)
//    }
//
//    actual fun id3ReadCover(bytes: ByteArray): ByteArray? =
//        readCBytes(bytes) { inPtr, len, outLen ->
//            ffi.id3_read_cover(inPtr, len, outLen)
//        }
//
//    actual fun id3ReadEditedSyncedLyrics(bytes: ByteArray): String? = readCBufferWithBytes(bytes) { inPtr, len, buf, cap ->
//        ffi.id3_read_edited_synced_lyrics(inPtr, len, buf, cap)
//    }
//
//    // ── id3 writes ──────────────────────────────────────────────────────────────
//
//    actual fun id3WriteEditedLyrics(bytes: ByteArray, text: String): ByteArray =
//        readCBytesNotNull(bytes) { inPtr, len, outLen ->
//            ffi.id3_write_edited_lyrics(inPtr, len, text, outLen)
//        }
//
//    actual fun id3WriteEditedSyncedLyrics(
//        bytes: ByteArray,
//        content: String
//    ): ByteArray = readCBytesNotNull(bytes) { inPtr, len, outLen ->
//        ffi.id3_write_edited_synced_lyrics(inPtr, len, content, outLen)
//    }
//
//    actual fun id3WriteProtagonist(bytes: ByteArray, picture: ByteArray): ByteArray =
//        readCBytesNotNull(bytes) { inPtr, len, outLen ->
//            ffi.id3_write_protagonist(
//                inPtr, len,
//                picture.asUByteArray().refTo(0), picture.size.toULong(),
//                outLen
//            )
//        }
//
//    // ── infer ───────────────────────────────────────────────────────────────────
//
//    actual fun inferExtension(bytes: ByteArray): String? = readCBufferWithBytes(bytes) { inPtr, len, buf, cap ->
//        ffi.inf_read_extension(inPtr, len, buf, cap)
//    }
//
//    // ── codec ───────────────────────────────────────────────────────────────────
//
//    actual fun codecExtractRgba(inputPath: String, outputPath: String) {
//        ffi.cod_extract_rgba(inputPath, outputPath)
//    }
//
//    // ── xmp reads ───────────────────────────────────────────────────────────────
//
//    actual fun xmpReadOriginalDocumentId(bytes: ByteArray): String? =
//        readCBufferWithBytes(bytes) { inPtr, len, buf, cap ->
//            ffi.xmp_read_original_document_id(inPtr, len, buf, cap)
//        }
//
//    actual fun xmpReadGenre(bytes: ByteArray): String? =
//        readCBufferWithBytes(bytes) { inPtr, len, buf, cap ->
//            ffi.xmp_read_genre(inPtr, len, buf, cap)
//        }
//
//    actual fun xmpReadLyrics(bytes: ByteArray): String? =
//        readCBufferWithBytes(bytes) { inPtr, len, buf, cap ->
//            ffi.xmp_read_lyrics(inPtr, len, buf, cap)
//        }
//
//    actual fun xmpReadRating(bytes: ByteArray): String? =
//        readCBufferWithBytes(bytes) { inPtr, len, buf, cap ->
//            ffi.xmp_read_rating(inPtr, len, buf, cap)
//        }
//
//    actual fun xmpReadProjectName(bytes: ByteArray): String? =
//        readCBufferWithBytes(bytes) { inPtr, len, buf, cap ->
//            ffi.xmp_read_project_name(inPtr, len, buf, cap)
//        }
//
//    // ── audio engine transport ──────────────────────────────────────────────────
//
//    actual fun audNew(): Long = ffi.aud_new().toLong()
//
//    actual fun audFree(handle: Long) {
//        ffi.aud_free(handle)
//    }
//
//    actual fun audLoad(handle: Long, bytes: ByteArray): Double {
//        return ffi.aud_load(handle.toULong(), bytes.asUByteArray().refTo(0), bytes.size.toULong())
//    }
//
//    actual fun audLoadPath(handle: Long, path: String): Double {
//        return ffi.aud_load_path(handle.toULong(), path)
//    }
//
//    actual fun audPlay(handle: Long, fromSec: Double) {
//        ffi.aud_play(handle.toULong(), fromSec)
//    }
//
//    actual fun audPause(handle: Long) {
//        ffi.aud_pause(handle.toULong())
//    }
//
//    actual fun audResume(handle: Long) {
//        ffi.aud_resume(handle.toULong())
//    }
//
//    actual fun audSeekTo(handle: Long, sec: Double) {
//        ffi.aud_seek_to(handle.toULong(), sec)
//    }
//
//    actual fun audSeekBy(handle: Long, sec: Double) {
//        ffi.aud_seek_by(handle.toULong(), sec)
//    }
//
//    actual fun audSetLoop(handle: Long, aSec: Double, bSec: Double) {
//        ffi.aud_set_loop(handle.toULong(), aSec, bSec)
//    }
//
//    actual fun audClearLoop(handle: Long) {
//        ffi.aud_clear_loop(handle.toULong())
//    }
//
//    actual fun audPosition(handle: Long): Double {
//        return ffi.aud_position(handle.toULong())
//    }
//
//    actual fun audState(handle: Long): Int {
//        return ffi.aud_state(handle.toULong()).toInt()
//    }
//
//    // ── offline decode ──────────────────────────────────────────────────────────
//
//    actual fun audDecodeTo16kMonoF32(inputPath: String, outPcmPath: String): Long {
//        val result = ffi.aud_decode_to_16k_mono_f32(inputPath, outPcmPath)
//        if (result < 0) throw RuntimeException("audDecodeTo16kMonoF32 failed with code: $result")
//        return result
//    }
//
//    // ── helpers ─────────────────────────────────────────────────────────────────
//
//    private inline fun readCBuffer(
//        block: (buf: CPointer<ByteVar>?, capacity: Int) -> Int
//    ): String = memScoped {
//        val required = block(null, 0)
//        val buf = allocArray<ByteVar>(required)
//        val written = block(buf, required)
//        buf.readBytes(written).decodeToString()
//    }
//
//    private inline fun readCBufferWithBytes(
//        bytes: ByteArray,
//        block: (inPtr: CValuesRef<UByteVar>?, inLen: ULong, outBuf: CPointer<ByteVar>?, capacity: Int) -> Int
//    ): String? = memScoped {
//        val inRef = bytes.asUByteArray().refTo(0)
//        val inLen = bytes.size.toULong()
//
//        val required = block(inRef, inLen, null, 0)
//        val buf = allocArray<ByteVar>(required)
//        val written = block(inRef, inLen, buf, required)
//
//        buf.readBytes(written).decodeToString()
//    }
//
//    private inline fun readCBytes(
//        bytes: ByteArray,
//        block: (inPtr: CValuesRef<UByteVar>?, inLen: ULong, outLenPtr: CPointer<ULongVar>) -> CPointer<*>?
//    ): ByteArray? = memScoped {
//        val outLen = alloc<ULongVar>()
//        val ptr = block(bytes.asUByteArray().refTo(0), bytes.size.toULong(), outLen.ptr)
//        ptr?.readBytes(outLen.value.toInt())
//    }
//
//    private inline fun readCBytesNotNull(
//        bytes: ByteArray,
//        block: (inPtr: CValuesRef<UByteVar>?, inLen: ULong, outLenPtr: CPointer<ULongVar>) -> CPointer<*>?
//    ): ByteArray = memScoped {
//        val outLen = alloc<ULongVar>()
//        val ptr = block(bytes.asUByteArray().refTo(0), bytes.size.toULong(), outLen.ptr)!!
//        ptr.readBytes(outLen.value.toInt())
//    }
//}
@file:OptIn(ExperimentalWasmJsInterop::class)

package market.femi

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsModule

// 1. The external JS bindings using the exact snake_case WASM/CAPI names.
// Kotlin/Wasm handles the String/ByteArray bridging seamlessly here.
@JsModule("ffi") // Adjust this module name to whatever your JS host object is called
external object WasmHost {

    fun id3_read_lyrics(bytes: ByteArray): String?
    fun id3_read_edited_lyrics(bytes: ByteArray): String?
    fun id3_read_album(bytes: ByteArray): String?
    fun id3_read_genre(bytes: ByteArray): String?
    fun id3_read_suno_clip_id(bytes: ByteArray): String?
    fun id3_read_cover(bytes: ByteArray): ByteArray?
    fun id3_read_edited_synced_lyrics(bytes: ByteArray): String?

    fun id3_write_edited_lyrics(bytes: ByteArray, text: String): ByteArray
    fun id3_write_edited_synced_lyrics(bytes: ByteArray, content: String): ByteArray
    fun id3_write_protagonist(bytes: ByteArray, picture: ByteArray): ByteArray

    fun inf_read_extension(bytes: ByteArray): String?

    fun cod_extract_rgba(inputPath: String, outputPath: String)

    fun xmp_read_original_document_id(bytes: ByteArray): String?
    fun xmp_read_genre(bytes: ByteArray): String?
    fun xmp_read_lyrics(bytes: ByteArray): String?
    fun xmp_read_rating(bytes: ByteArray): String?
    fun xmp_read_project_name(bytes: ByteArray): String?

    fun aud_new(): Long
    fun aud_free(handle: Long)
    fun aud_load(handle: Long, bytes: ByteArray): Double
    fun aud_load_path(handle: Long, path: String): Double
    fun aud_play(handle: Long, fromSec: Double)
    fun aud_pause(handle: Long)
    fun aud_resume(handle: Long)
    fun aud_seek_to(handle: Long, sec: Double)
    fun aud_seek_by(handle: Long, sec: Double)
    fun aud_set_loop(handle: Long, aSec: Double, bSec: Double)
    fun aud_clear_loop(handle: Long)
    fun aud_position(handle: Long): Double
    fun aud_state(handle: Long): Int

    fun aud_decode_to_16k_mono_f32(inputPath: String, outPcmPath: String): Long
}

// 2. The actual implementation delegating to the snake_case host functions.
//actual object Ffi {
//    actual fun id3ReadLyrics(bytes: ByteArray): String? = WasmHost.id3_read_lyrics(bytes)
//    actual fun id3ReadEditedLyrics(bytes: ByteArray): String? = WasmHost.id3_read_edited_lyrics(bytes)
//    actual fun id3ReadAlbum(bytes: ByteArray): String? = WasmHost.id3_read_album(bytes)
//    actual fun id3ReadGenre(bytes: ByteArray): String? = WasmHost.id3_read_genre(bytes)
//    actual fun id3ReadSunoClipId(bytes: ByteArray): String? = WasmHost.id3_read_suno_clip_id(bytes)
//    actual fun id3ReadCover(bytes: ByteArray): ByteArray? = WasmHost.id3_read_cover(bytes)
//    actual fun id3ReadEditedSyncedLyrics(bytes: ByteArray): String? = WasmHost.id3_read_edited_synced_lyrics(bytes)
//
//    actual fun id3WriteEditedLyrics(bytes: ByteArray, text: String): ByteArray = WasmHost.id3_write_edited_lyrics(bytes, text)
//    actual fun id3WriteEditedSyncedLyrics(bytes: ByteArray, content: String): ByteArray = WasmHost.id3_write_edited_synced_lyrics(bytes, content)
//    actual fun id3WriteProtagonist(bytes: ByteArray, picture: ByteArray): ByteArray = WasmHost.id3_write_protagonist(bytes, picture)
//
//    actual fun inferExtension(bytes: ByteArray): String? = WasmHost.inf_read_extension(bytes)
//
//    actual fun codecExtractRgba(inputPath: String, outputPath: String)  {
////        WasmHost.cod_extract_rgba(inputPath, outputPath)
//    }
//
//    actual fun xmpReadOriginalDocumentId(bytes: ByteArray): String? = WasmHost.xmp_read_original_document_id(bytes)
//    actual fun xmpReadGenre(bytes: ByteArray): String? = WasmHost.xmp_read_genre(bytes)
//    actual fun xmpReadLyrics(bytes: ByteArray): String? = WasmHost.xmp_read_lyrics(bytes)
//    actual fun xmpReadRating(bytes: ByteArray): String? = WasmHost.xmp_read_rating(bytes)
//    actual fun xmpReadProjectName(bytes: ByteArray): String? = WasmHost.xmp_read_project_name(bytes)
//
//    actual fun audNew(): Long = WasmHost.aud_new()
//    actual fun audFree(handle: Long) = WasmHost.aud_free(handle)
//    actual fun audLoad(handle: Long, bytes: ByteArray): Double = WasmHost.aud_load(handle, bytes)
//    actual fun audLoadPath(handle: Long, path: String): Double = WasmHost.aud_load_path(handle, path)
//    actual fun audPlay(handle: Long, fromSec: Double) = WasmHost.aud_play(handle, fromSec)
//    actual fun audPause(handle: Long) = WasmHost.aud_pause(handle)
//    actual fun audResume(handle: Long) = WasmHost.aud_resume(handle)
//    actual fun audSeekTo(handle: Long, sec: Double) = WasmHost.aud_seek_to(handle, sec)
//    actual fun audSeekBy(handle: Long, sec: Double) = WasmHost.aud_seek_by(handle, sec)
//    actual fun audSetLoop(handle: Long, aSec: Double, bSec: Double) = WasmHost.aud_set_loop(handle, aSec, bSec)
//    actual fun audClearLoop(handle: Long) = WasmHost.aud_clear_loop(handle)
//    actual fun audPosition(handle: Long): Double = WasmHost.aud_position(handle)
//    actual fun audState(handle: Long): Int = WasmHost.aud_state(handle)
//    actual fun audDecodeTo16kMonoF32(inputPath: String, outPcmPath: String): Long = WasmHost.aud_decode_to_16k_mono_f32(inputPath, outPcmPath)
//}
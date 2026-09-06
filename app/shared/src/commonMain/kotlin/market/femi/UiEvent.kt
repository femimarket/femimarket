package market.femi

import femi.app.shared.generated.resources.Res
import femi.app.shared.generated.resources.effect_clips_added_one
import femi.app.shared.generated.resources.effect_clips_added_other
import femi.app.shared.generated.resources.effect_directory_connected
import femi.app.shared.generated.resources.effect_download_ready
import femi.app.shared.generated.resources.effect_images_added_one
import femi.app.shared.generated.resources.effect_images_added_other
import femi.app.shared.generated.resources.effect_job_failed_clip_generation
import femi.app.shared.generated.resources.effect_job_failed_final_cut
import femi.app.shared.generated.resources.effect_job_failed_generic
import femi.app.shared.generated.resources.effect_job_failed_image_generation
import femi.app.shared.generated.resources.effect_lyrics_aligned_one
import femi.app.shared.generated.resources.effect_lyrics_aligned_other
import femi.app.shared.generated.resources.effect_missing_cover_art
import femi.app.shared.generated.resources.effect_operation_failed_about
import femi.app.shared.generated.resources.effect_operation_failed_align
import femi.app.shared.generated.resources.effect_operation_failed_commit_lyrics
import femi.app.shared.generated.resources.effect_operation_failed_connect_directory
import femi.app.shared.generated.resources.effect_operation_failed_delete
import femi.app.shared.generated.resources.effect_operation_failed_export
import femi.app.shared.generated.resources.effect_operation_failed_generate_about
import femi.app.shared.generated.resources.effect_operation_failed_generate_about_questions
import femi.app.shared.generated.resources.effect_operation_failed_generic
import femi.app.shared.generated.resources.effect_operation_failed_import
import femi.app.shared.generated.resources.effect_operation_failed_loop_region
import femi.app.shared.generated.resources.effect_operation_failed_protagonist
import femi.app.shared.generated.resources.effect_operation_failed_set_export
import femi.app.shared.generated.resources.effect_operation_failed_storyboard_about_required
import femi.app.shared.generated.resources.effect_operation_failed_trim_video
import femi.app.shared.generated.resources.effect_operation_failed_word_alignment
import femi.app.shared.generated.resources.effect_tracks_imported_one
import femi.app.shared.generated.resources.effect_tracks_imported_other
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString


/**
 * Strongly typed semantic keys for Workspace intent failures.
 */
enum class OperationKind {
    IMPORT,
    ALIGN,
    COMMIT_LYRICS,
    PROTAGONIST,
    WORD_ALIGNMENT,
    EXPORT,
    CONNECT_DIRECTORY,
    LOOP_REGION,
    TRIM_VIDEO,
    SET_EXPORT,
    DELETE,
    ABOUT,
    GENERATE_ABOUT,
    GENERATE_ABOUT_QUESTIONS,
    STORYBOARD_ABOUT_REQUIRED,
    GENERIC
}

/**
* Small supporting types shared by the ViewModels and ports. New to the codebase.
*/

/**
 * One-shot UI events — SEMANTIC, not display strings. The ViewModel is the shared, platform-agnostic
 * core, so it must not bake in English (or any locale). Each platform's UI maps these to a localized
 * string via its own resources (Android strings.xml, iOS String Catalog, web i18n) — the data (file,
 * count) rides on the event; the template + translation live per-platform. Tests assert on identity
 * (e.g. MissingCoverArt("song.mp3")), which survives translation.
 */
sealed interface UiEffect {
    data class TracksImported(val count: Int) : UiEffect
    data class MissingCoverArt(val file: String) : UiEffect
    data class LyricsAligned(val wordCount: Int) : UiEffect
    data class ClipsAdded(val count: Int) : UiEffect
    // A storyboard image-generation job produced `count` new frames; Workspace collected + persisted
    // them and emits this so the UI can localize a "N images added" confirmation. Sibling of ClipsAdded:
    // same shape, different media kind (images, not video clips) — the results collector's image guard
    // (r.images.isNotEmpty()) emits this exactly as the video guard emits ClipsAdded.
    data class ImagesAdded(val count: Int) : UiEffect
    // A generation/codec job failed. `job` is a semantic key (the job type), NOT a message — the
    // exception's text is English and must not ride on a locale-agnostic effect (§3.3). Log it instead.
    data class JobFailed(val job: String) : UiEffect
    data class Download(val name: String) : UiEffect
    // A Workspace INTENT failed. `operation` is a semantic key ("import"/"align"/"commit") the UI maps
    // to a localized "X failed" message — same no-English rule as JobFailed; no raw `reason` rides.
    data class OperationFailed(val operation: OperationKind) : UiEffect
//    data class OperationThrow(val fnName:String, val error: Throwable) : UiEffect
    // Parity Ledger ROW 7 (contracts-uieffect-parity-ledger / contracts-r3-directoryconnected). The
    // connectDirectory / restoreDirectory intents (M1, U11) emit this when a folder is (re)connected; the
    // UI localizes webMain's "Directory Connected!" snackbar carrying the folder name. PAYLOAD form is
    // BINDING — the earlier parameterless `data object` proposal is SUPERSEDED. Only genuinely NEW
    // UiEffect *type* in the whole delta. Its exhaustive-`when` arm lives in ui/UiEffectMessages.kt.
    data class DirectoryConnected(val name: String) : UiEffect
}


data class UiEffectMessage(
    val resource: StringResource,
    val formatArgs: List<Any>,
)

/**
 * Convenience: build a message with no format arguments (the failure templates carry their whole
 * sentence in the resource).
 */
private fun message(resource: StringResource): UiEffectMessage =
    UiEffectMessage(resource, emptyList())

/**
 * Convenience: build a message whose template consumes the given positional arguments.
 */
private fun message(resource: StringResource, vararg formatArgs: Any): UiEffectMessage =
    UiEffectMessage(resource, formatArgs.toList())

/**
 * The exhaustive semantic-effect → localized-template mapping. NO else branch on the outer `when`
 * (D6 — a new [UiEffect] variant must fail compilation right here). Singular/plural is an explicit
 * `count == 1` pick between the *_one / *_other string pair, mirroring the behavior of the retired
 * hardcoded `toDisplayMessage()` baseline (App.kt) that this mapping replaces.
 */
fun UiEffect.toMessage(): UiEffectMessage = when (this) {
    is UiEffect.TracksImported ->
        if (count == 1) {
            message(Res.string.effect_tracks_imported_one)
        } else {
            message(Res.string.effect_tracks_imported_other, count)
        }

    is UiEffect.MissingCoverArt ->
        message(Res.string.effect_missing_cover_art, file)

    is UiEffect.LyricsAligned ->
        if (wordCount == 1) {
            message(Res.string.effect_lyrics_aligned_one)
        } else {
            message(Res.string.effect_lyrics_aligned_other, wordCount)
        }

    is UiEffect.ClipsAdded ->
        if (count == 1) {
            message(Res.string.effect_clips_added_one)
        } else {
            message(Res.string.effect_clips_added_other, count)
        }

    is UiEffect.ImagesAdded ->
        if (count == 1) {
            message(Res.string.effect_images_added_one)
        } else {
            message(Res.string.effect_images_added_other, count)
        }

    is UiEffect.JobFailed ->
        // `job` is the semantic job-kind key: the BackgroundAsyncJob class simpleName that
        // JobsViewModel:51 stamps on the failure. Each known kind gets its own localized sentence;
        // the inner `when` needs an else because String is open-ended, and the generic fallback
        // guarantees an unrecognized future kind still renders localized copy — never a raw
        // developer class name.
        when (job) {
            "ImageGenAsyncJob" -> message(Res.string.effect_job_failed_image_generation)
            "VeoAsyncJob" -> message(Res.string.effect_job_failed_clip_generation)
            "MuxAsyncJob" -> message(Res.string.effect_job_failed_final_cut)
            else -> message(Res.string.effect_job_failed_generic)
        }

    is UiEffect.Download ->
        // Handler-owned (§1.4): the collector routes this to LocalDownloadHandler, not the
        // snackbar; the mapping stays total so post-save confirmations have their copy.
        message(Res.string.effect_download_ready, name)

    is UiEffect.OperationFailed ->
        // `operation` is the semantic intent key WorkspaceViewModel emits on a failed intent
        // ("import" :271, "align" :304, "commit" :331, "protagonist" :416,
        // "editWordAlignment" :459, "export" :749) plus the ~9 keys the port adds (M1 emitters). Same
        // shape as JobFailed above: one localized sentence per known key, generic fallback for future
        // keys — raw keys never render.
        when (operation) {
            OperationKind.IMPORT -> message(Res.string.effect_operation_failed_import)
            OperationKind.ALIGN -> message(Res.string.effect_operation_failed_align)
            OperationKind.COMMIT_LYRICS -> message(Res.string.effect_operation_failed_commit_lyrics)
            OperationKind.PROTAGONIST -> message(Res.string.effect_operation_failed_protagonist)
            OperationKind.WORD_ALIGNMENT -> message(Res.string.effect_operation_failed_word_alignment)
            OperationKind.EXPORT -> message(Res.string.effect_operation_failed_export)
            OperationKind.CONNECT_DIRECTORY -> message(Res.string.effect_operation_failed_connect_directory)
            OperationKind.LOOP_REGION -> message(Res.string.effect_operation_failed_loop_region)
            OperationKind.TRIM_VIDEO -> message(Res.string.effect_operation_failed_trim_video)
            OperationKind.SET_EXPORT -> message(Res.string.effect_operation_failed_set_export)
            OperationKind.DELETE -> message(Res.string.effect_operation_failed_delete)
            OperationKind.ABOUT -> message(Res.string.effect_operation_failed_about)
            OperationKind.GENERATE_ABOUT -> message(Res.string.effect_operation_failed_generate_about)
            OperationKind.GENERATE_ABOUT_QUESTIONS -> message(Res.string.effect_operation_failed_generate_about_questions)
            OperationKind.STORYBOARD_ABOUT_REQUIRED -> message(Res.string.effect_operation_failed_storyboard_about_required)
            OperationKind.GENERIC -> message(Res.string.effect_operation_failed_generic)
        }

    is UiEffect.DirectoryConnected ->
        // Parity Ledger ROW 7 — the exhaustive-`when` arm the sealed widener REQUIRES. Localizes the
        // "Directory Connected!" snackbar carrying the connected folder name.
        message(Res.string.effect_directory_connected, name)
}

/**
 * Resolve the message to the viewer-locale String. Suspend on purpose: this runs inside the
 * non-composable effects collector (a LaunchedEffect coroutine feeding SnackbarHostState), where
 * the composable `stringResource` API is unavailable — the compose-resources suspend `getString`
 * is the sanctioned resolution path there.
 */
suspend fun UiEffectMessage.resolve(): String =
    getString(resource, *formatArgs.toTypedArray())

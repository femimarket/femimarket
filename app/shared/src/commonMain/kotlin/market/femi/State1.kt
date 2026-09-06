package market.femi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.russhwolf.settings.Settings
import dev.shivathapaa.logger.api.Log
import femi.app.shared.generated.resources.Res
import femi.app.shared.generated.resources.menu_about_faqs
import femi.app.shared.generated.resources.menu_about_storyboard
import femi.app.shared.generated.resources.menu_import_directory
import femi.app.shared.generated.resources.menu_select_audio
import femi.app.shared.generated.resources.menu_setup
import femi.app.shared.generated.resources.music_settings_blueprint
import femi.app.shared.generated.resources.music_settings_faq
import femi.app.shared.generated.resources.music_settings_import
import femi.app.shared.generated.resources.music_settings_setup
import femi.app.shared.generated.resources.music_settings_song
import femi.app.shared.generated.resources.music_videos
import femi.app.shared.generated.resources.splash_care
import femi.app.shared.generated.resources.splash_match
import org.jetbrains.compose.resources.StringResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import market.femi.carer.register.CareNamesState
import market.femi.models.Audio
import market.femi.models.AudioLine
import market.femi.models.AudioQA
import market.femi.models.Image
import market.femi.models.Matrix
import market.femi.models.Video
import market.femi.models.XmpItem
import market.femi.models.getAlignedLines
import market.femi.music.studio.MusicLinesState
import kotlinx.serialization.json.JsonElement
import market.femi.services.ApiService
import market.femi.services.AudioService
import market.femi.carer.CarerCandidateCheckState
import market.femi.carer.CareJobsState
import market.femi.music.studio.MusicScenesState
import market.femi.services.CareService
import market.femi.services.CodecService
import market.femi.services.DbService
import market.femi.services.FakeAudioService
import market.femi.services.FakeKvService
import market.femi.services.FileService
import market.femi.services.KvService
import market.femi.services.LlmService
import market.femi.services.LogService
import market.femi.services.MetaService
import market.femi.services.createRealApiService
import market.femi.services.createRealAudioService
import market.femi.match.MatchCheckState
import market.femi.match.MatchQuestionnaireState
import market.femi.match.models.QuestionnaireList
import market.femi.services.MatchService
import market.femi.services.UiService
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import market.femi.money.MoneyApp
import market.femi.services.MoneyService
import market.femi.services.MusicService
import market.femi.services.MatrixService
import market.femi.services.createRealMoneyService
import market.femi.services.HttpService
import market.femi.services.createRealHttpService
import market.femi.services.createRealMusicService
import market.femi.services.createRealCareService
import market.femi.services.createRealMatchService
import market.femi.services.createRealUiService
import market.femi.services.createRealMatrixService
import market.femi.services.createRealCodecService
import market.femi.services.createRealDbService
import market.femi.services.createRealFsService2
import market.femi.services.createRealKvService
import market.femi.services.createRealLmStudioLlmService
import market.femi.services.createRealLogService
import market.femi.services.createRealMetaService
import market.femi.state.CareLoginState
import market.femi.state.MusicAboutState
import kotlin.collections.set
import kotlin.to

interface IState {
    val db: DbService
    val api: ApiService
    val fs: FileService
    val meta: MetaService
    val http: HttpService
    val kv: Settings
    val audio: AudioService
    val media: CodecService
    val log: LogService
    val llm: LlmService
    val care: CareService
    val match: MatchService
    val ui: UiService
    val matrix: MatrixService
    val musicService: MusicService
    val money: MoneyService
}


fun fakeState(): State {
    return State(
        kv = FakeKvService(),
        audio = FakeAudioService()
    )
}


class State(
    override val log: LogService = createRealLogService(),
    override val kv: KvService = createRealKvService(),
    override val llm: LlmService = createRealLmStudioLlmService(kv),
    override val fs: FileService = createRealFsService2(kv),
    override val audio: AudioService = createRealAudioService(),
    override val db: DbService = createRealDbService(kv,log),
    override val meta: MetaService = createRealMetaService(kv,fs),
    override val media: CodecService = createRealCodecService(kv),
    override val api: ApiService = createRealApiService(kv,fs),
    override val http: HttpService = createRealHttpService(kv),
    override val care: CareService = createRealCareService(kv, http),
    override val match: MatchService = createRealMatchService(kv),
    override val ui: UiService = createRealUiService(kv),
    override val matrix: MatrixService = createRealMatrixService(kv, http),
    override val musicService: MusicService = createRealMusicService(kv, http),
    override val money: MoneyService = createRealMoneyService(kv),
//    val frameClock: FakeFrameClock = FakeFrameClock(),
) :  IState, ViewModel() {
    val scope get() = viewModelScope
    val data = ActiveData()
//    val dufs: FileService = RealDufsFsService(kv)
    val nav = RouterState(this)
    val header = HeaderState(this)
    val footer = FooterState(this)
    val theme = ActiveTheme(this)
    val splash = SplashState(this)

    val timeline = ActiveTimeline(this)


    val music = MusicApp(this)
    val careApp = CareApp(this)
    val matchApp = MatchApp(this)
    val techApp = TechApp(this)
    val moneyApp = MoneyApp(this)



    var showMenu = ActiveShowState()
    val setupDialog = SetupDialog(this)
    val importDialog = ImportDialog(this)
    val audioDialog = SelectAudioDialogState(this)
    val musicLines = MusicLinesState(this)
    val login = LoginState(this)
    val lyricsDialog = EditLyricsDialogState(this)
    val faqsDialog = FaqsState(this)
    val socialMediaBlueprintDialog = SocialMediaBlueprintDialogState(this)
    val scenes = MusicScenesState(this)

//    val activeData by mutableStateOf(ActiveFieldState())
    var errorMessage by mutableStateOf<String?>(null)
    var editMode by mutableStateOf(false)
    var selectedTabIndex by mutableStateOf(0)
    val videoTrimChanges = mutableStateMapOf<String, Video>()      // Studio.kt:325
    val snapChange = mutableStateMapOf<String, Double>()           // Studio.kt:321
    var unmutedVideoId by mutableStateOf<String?>(null)
    val computedAudioStarts: Map<String, Double> by derivedStateOf {
        val starts = mutableMapOf<String, Double>()
        // group by project so two projects' clips pack on independent timelines (mirrors videosByProject).
        data.projectVideos.groupBy { it.project }.forEach { (_, clipsInProject) ->
            var currentStartMs = 0.0
            clipsInProject.forEach { video ->
                starts[video.id] = currentStartMs
                // the staged edit wins over the persisted row so an in-flight trim/speed repacks live.
                val activeVideo = videoTrimChanges[video.id] ?: video
                // defensively avoid divide-by-zero on a 0/negative speed (mirrors the source guard).
                val speedFactor = (activeVideo.speed.takeIf { it > 0 } ?: 100L) / 100.0
                currentStartMs += (activeVideo.endMs - activeVideo.startMs) / speedFactor
            }
        }
        starts
    }
    private val _effects = MutableSharedFlow<UiEffect>(extraBufferCapacity = 64, replay = 64)
    val effects: SharedFlow<UiEffect> = _effects.asSharedFlow()

    init {
        // the ONE narrow handoff with the Jobs satellite: results flow back here to persist.
        viewModelScope.launch {
//           snapshotFlow { activeAudio?.project }
//            jobs.results.collect { r ->
//                repo.upsert(videos = r.videos, images = r.images)
//                r.videos.forEach { videos[it.id] = it }
//                r.images.forEach { images[it.id] = it }
//                if (r.videos.isNotEmpty()) ui.emit(UiEffect.ClipsAdded(r.videos.size))
//            }
        }
    }


    // ── user actions (intents) — the surface a journey test drives ──

    fun loadProject(name: String) = viewModelScope.launch {
        TODO("migrate from Studio.kt:516-540 — repo.videos(name)/images(name) → projectVideos/projectImages")
    }

    fun searchMedia() = viewModelScope.launch {
        TODO("migrate from Viewport.kt:245 filterViewport — repo.searchAudios(...) honoring the filters above")
    }

    suspend fun upsertAudioAndReload(audio: Audio){
        // every Audio field → its meta endpoint, in parallel (the server serializes writes,
        // so concurrent posts are safe). Nullable fields are skipped when absent.
        coroutineScope {
            listOf(
                async { meta.writeAudioId(audio.name, audio.id) },
                async { meta.writeAudioBackedUp(audio.name, audio.backedUp.toString()) },
                async { meta.writeAudioName(audio.name, audio.name) },
                async { audio.error?.let { meta.writeAudioError(audio.name, it) } },
                async { audio.genre?.let { meta.writeAudioGenre(audio.name, it) } },
                async { meta.writeAudioImage(audio.name, audio.image) },
                async { audio.like?.let { meta.writeAudioLike(audio.name, it.toString()) } },
                async { audio.lyrics?.let { meta.writeAudioLyrics(audio.name, it) } },
                async { audio.elevenLabsForcedAlignment?.let { meta.writeAudioElevenLabsForcedAlignment(audio.name, AppJson.encodeToString(it)) } },
                async { audio.protagonist?.let { meta.writeAudioProtagonist(audio.name, it) } },
                async { meta.writeAudioProject(audio.name, audio.project) },
                async { audio.uid?.let { meta.writeAudioUid(audio.name, it) } },
                async { meta.writeAudioAudioLines(audio.name, AppJson.encodeToString(audio.audioLines)) },
                async { meta.writeAudioWordAlignments(audio.name, AppJson.encodeToString(audio.wordAlignments)) },
                async { meta.writeAudioFaqs(audio.name, AppJson.encodeToString(audio.faqs)) },
                async { audio.socialMediaBlueprint?.let { meta.writeAudioSocialMediaBlueprint(audio.name, it) } },
                async { audio.video?.let { meta.writeAudioVideo(audio.name, it) } },
                async { meta.writeAudioLyricTokens(audio.name, AppJson.encodeToString(audio.lyricTokens)) },
            ).awaitAll()
        }
        db.upsert(audios = listOf(audio))
        data.audios.map.clear()
        data.audios.map.putAll(db.audios(audio.project).associateBy { it.id })
    }

    suspend fun readAudioMeta(name: String): Audio = coroutineScope {
        // every Audio field ← its meta endpoint, in parallel — the per-field reads compose
        // the Audio here (there is no model-level read on the server). Absent fields take
        // the model's own defaults; id/image are the model's required fields, so a file
        // without them throws, same contract as before.
        val id = async { meta.readAudioId(name) }
        val backedUp = async { meta.readAudioBackedUp(name) }
        val audioName = async { meta.readAudioName(name) }
        val error = async { meta.readAudioError(name) }
        val genre = async { meta.readAudioGenre(name) }
        val image = async { meta.readAudioImage(name) }
        val like = async { meta.readAudioLike(name) }
        val lyrics = async { meta.readAudioLyrics(name) }
        val editedLyrics = async { meta.readAudioEditedLyrics(name) }
        val elevenLabsForcedAlignment = async { meta.readAudioElevenLabsForcedAlignment(name) }
        val protagonist = async { meta.readAudioProtagonist(name) }
        val project = async { meta.readAudioProject(name) }
        val uid = async { meta.readAudioUid(name) }
        val audioLines = async { meta.readAudioAudioLines(name) }
        val wordAlignments = async { meta.readAudioWordAlignments(name) }
        val faqs = async { meta.readAudioFaqs(name) }
        val socialMediaBlueprint = async { meta.readAudioSocialMediaBlueprint(name) }
        val video = async { meta.readAudioVideo(name) }
        val lyricTokens = async { meta.readAudioLyricTokens(name) }
            Audio(
                id = id.await()!!,
                backedUp = backedUp.await() ?: false,
                name = audioName.await() ?: "",
                error = error.await(),
                genre = genre.await(),
                image = image.await()!!,
                like = like.await(),
                lyrics = lyrics.await(),
                editedLyrics = editedLyrics.await(),
                elevenLabsForcedAlignment = elevenLabsForcedAlignment.await(),
                protagonist = protagonist.await(),
                project = project.await() ?: "Default",
                uid = uid.await(),
                audioLines = audioLines.await(),
                wordAlignments = wordAlignments.await(),
                faqs = faqs.await().ifEmpty { Audio.emptyFaqs() },
                socialMediaBlueprint = socialMediaBlueprint.await(),
                video = video.await(),
                lyricTokens = lyricTokens.await(),
            )
    }

    fun setup() {
        require(setupDialog.value)
        setupDialog.hide()
    }

    fun play(name: String, fromSec: Double? = null, loop: LoopRegion? = null) = viewModelScope.launch {
        timeline.loopRegion = loop
        if (!fs.exists(name)) fs.writeBytes(name, fs.readBytes(name))
        audio.play(kv.withFsUrl(name), fromSec, loop)
    }

    var isWorking by mutableStateOf(false)


//    val isWorking: Boolean by derivedStateOf {
//
//        importDialog.isImporting or header.loader.isWorking
//    }



    fun alignLyrics(audioId: String) = viewModelScope.launch {
        val audio = data.audios.map[audioId] ?: return@launch
        val lyrics =
            audio.editedLyrics ?: audio.lyrics ?: return@launch   // alignment needs the lyrics
        data.action.isAligning = true
        runCatching {
            val words = api.forceAlign(lyrics, audio.name)
            meta.writeAudioWordAlignments(audio.name, AppJson.encodeToString(words))
            val updated = audio.copy(wordAlignments = words)
            db.upsert(audios = listOf(updated))
            data.audios.map[audioId] = updated
            _effects.tryEmit(UiEffect.LyricsAligned(words.size))
        }.onFailure {
            Log.e("[alignLyrics] $it")
            _effects.tryEmit(UiEffect.OperationFailed(OperationKind.ALIGN))
        }
        data.action.isAligning = false
    }

    /** As the user types — transient only. No repo write, no id3 write. */
    fun editLyricsDraft(text: String) {
        data.draft.lyricsDraft = text
    }

    /**
     * The durable transition: draft → `editedLyrics`. Writes the FILE first (the source of truth,
     * §PLAN FS-as-truth) so a failed write can't leave a "saved" edit that the next reload wipes;
     * then updates the repo + in-memory cache. On failure the edit stays a draft and an error surfaces.
     */
    fun commitLyrics(audioId: String) = viewModelScope.launch {
        val audio = data.audios.map[audioId] ?: return@launch
        // nothing typed → nothing to commit:
        val editedLyrics = data.draft.lyricsDraft ?: return@launch
        data.action.isSavingLyrics = true
        runCatching {
            // File FIRST (durable truth) — the "edited"-descriptor frame, original untouched:
            val bytes = fs.readBytes(audio.name)
            meta.writeAudioEditedLyrics(audio.name, editedLyrics)
            // then the cache (DB + in-memory):
            val updated = audio.copy(editedLyrics = editedLyrics)
            db.upsert(audios = listOf(updated))
            data.audios.map[audioId] = updated
            // clear the draft only once it's saved:
            data.draft.lyricsDraft = null
        }.onFailure { _effects.tryEmit(UiEffect.OperationFailed(OperationKind.COMMIT_LYRICS)) }
        data.action.isSavingLyrics = false
    }

    fun editAudioLine(index: Int, text: String) {
        TODO("migrate from Studio.kt:2528-2553 — replace line in activeAudio.audioLines, persist")
    }

    fun trimVideo(id: String, deltaMs: Double) {
        TODO("migrate from Timeline.kt:394-424 drag math into videoTrimChanges")
    }

    fun saveTrims(audioId: String) {
        // dispatch a MuxJob to the Jobs satellite; results come back via the init collector.
        TODO("migrate from Studio.kt:2349-2393 muxTrimSave — build MuxJob from videoTrimChanges, jobs.execute(it)")
    }

    fun generateImageForLine(audioId: String, lineIndex: Int, prompt: String) {
        // jobs.execute(ImageGenJob(...))
        TODO("build ImageGenJob (see AsyncJob.kt:220-301) and dispatch to jobs")
    }

    fun runStoryboard(audioId: String) {
        TODO("migrate from Studio.kt:1439-1456 storyboardPipeline — fan out ImageGenJobs to jobs")
    }

    fun requestFinalCut(audioId: String) {
        TODO("build MuxJob from projectVideos and dispatch to jobs")
    }

    fun deleteMedia(videos: List<Video> = emptyList(), audios: List<Audio> = emptyList()) =
        viewModelScope.launch {
            TODO("migrate from Viewport.kt:32 deleteData — repo.delete(...) then drop from the maps")
        }

//    /** Play the active clip — hands bytes to the Playback satellite. */
//    fun playActive() = viewModelScope.launch {
//        val a = activeAudio ?: return@launch
//        playback.setClip(files.readBytes(a.name))
//        // playback.play(fromMs, loop)  ← compute bounds, see Studio.kt:769-803
//    }
//
//    /** Rehydrate from the repo — the assertion that proves data persisted through the port. */
//    suspend fun loadFromRepo() {
//        audios.clear(); repo.audios().forEach { audios[it.id] = it }
//        videos.clear(); repo.videos().forEach { videos[it.id] = it }
//    }


//    val jobs = JobsViewModel(gen, codec, ids)
//    val playback = PlaybackViewModel(audioEngine, frameClock)
//    val workspace = WorkspaceViewModel(repo, files, gen, meta, settings, secrets, ids, jobs, playback)
//
//    /**
//     * The effects the UI would have shown. NOT a peek into any impl — it's a real subscriber to
//     * workspace.effects, the single output stream the host UI collects
//     * (LaunchedEffect { vm.effects.collect { snackbar(it) } }), with Jobs' effects already folded
//     * in. Call before the journey so the subscription is live when effects fire, then assert.
//     */
//    val effects = mutableListOf<UiEffect>()
//    fun observeEffects(scope: CoroutineScope) {
//        scope.launch { workspace.effects.collect { effects += it } }
//    }
}


class ActiveEdits {
    var video by mutableStateOf<Video?>(null)
    var image by mutableStateOf<Image?>(null)
    var audio by mutableStateOf<Audio?>(null)
    var file by mutableStateOf<XmpItem?>(null)
}


data class MemoryFile(val name: String, val bytes: ByteArray) {
    override fun equals(other: Any?) = this === other
    override fun hashCode() = name.hashCode()
}

data class WordAlignmentDraft(
    val audioId: String,
    val index: Int,
    val start: Double,
    val end: Double
)

data class QueryFilter(
    val indexName: String,
    val value: String
)

class ActiveFilters {

    // 1. Search Queries
    var video by mutableStateOf<QueryFilter?>(null)
    var audio by mutableStateOf<QueryFilter?>(null)
    var image by mutableStateOf<QueryFilter?>(null)
    var file by mutableStateOf<QueryFilter?>(null)
    var matrix by mutableStateOf<QueryFilter?>(null) // 👉 Added

    // 2. Selected Projects
    var audioSearchLikedOnly by mutableStateOf(true)
    var audioSearchProjects by mutableStateOf(true)


    var videoProject by mutableStateOf<String?>(null)
    var audioProject by mutableStateOf<String?>(null)
    var fileProject by mutableStateOf<String?>(null)
    var imageProject by mutableStateOf<String?>(null)

    var audioProtagonist by mutableStateOf<String?>(null)
}


class ActiveProjects {
    var projectName by mutableStateOf<String?>(null)
    var images by mutableStateOf<List<Image>>(emptyList())
    var videos by mutableStateOf<List<Video>>(emptyList())
//    var videos2 by mutableStateOf<Array<Video2>>(emptyArray())
}




class MusicApp(private val state: State){
    val settings = listOf(
        Res.string.menu_setup,
        Res.string.menu_import_directory,
        Res.string.menu_select_audio,
        Res.string.menu_about_faqs,
        Res.string.menu_about_storyboard,
    )
    val settingsIcons = listOf(
        Res.drawable.music_settings_setup,
        Res.drawable.music_settings_import,
        Res.drawable.music_settings_song,
        Res.drawable.music_settings_faq,
        Res.drawable.music_settings_blueprint,
    )

    val about = MusicAboutState(state)
}

class TechApp(private val state: State){
    var list by mutableStateOf(listOf<market.femi.ui.models.Composable>())
    var composable by mutableStateOf<JsonElement?>(null)

    suspend fun getComposables() {
        list = state.ui.getComposables()
    }

    suspend fun getComposable(composableId: Int) {
        composable = state.ui.getComposable(composableId)
    }
}

class MatchApp(private val state: State){
    val sessionId by derivedStateOf { state.kv.matchSessionId }
    var list by mutableStateOf(listOf<QuestionnaireList>())
    var questionnaire by mutableStateOf<QuestionnaireList?>(null)
    val questions = MatchQuestionnaireState(state)
    val check = MatchCheckState(state)

    suspend fun createSession() {
        state.kv.matchSessionId = state.match.createSession()
    }

    suspend fun getQuestionnaires() {
        list = state.match.getQuestionnaires()
    }
}

class CareApp(private val state: State){
    val candidateCheck = CarerCandidateCheckState(state)

    val login = CareLoginState(state)

    val jobs = CareJobsState(state)

    val names = CareNamesState(state)

    val about = MusicAboutState(state)
}

class ActiveTimeline(private val state: State){
    var loopRegion by mutableStateOf<LoopRegion?>(null)
    val activeAudioLines by derivedStateOf {
        val inMs = loopRegion?.inMs
        val outMs = loopRegion?.outMs
        if (inMs != null && outMs != null) {
            state.data.audio?.lines.orEmpty().filter { it.startMs in inMs..<outMs }
        } else {
            val active = if (state.data.audio?.lines.orEmpty().isEmpty()) null
            else state.data.audio?.lines?.lastOrNull { it.startMs <= state.audio.positionSec } ?: state.data.audio?.lines!!.first()
            listOfNotNull(active)
        }
    }
}


class ActiveTheme(private val state: State) {
    // one seed per world — the super app's front door and each shell app wear
    // distinct schemes; navigating between them crossfades the whole palette
    private val superAppSeed = Color(0xFF6750A4)      // the M3 default (baseline) seed
    private val careSeed = Color(0xFF00897B)          // teal — care
    private val matchSeed = Color(0xFFEF6C00)         // amber — match
    private val moneySeed = Color(0xFF0E7C5A)         // emerald — money
    private val musicBaselineSeed = Color(0xFFE91E63) // pink, until a song seeds it

    private val musicSeed: Color by derivedStateOf {
        state.data.audio?.current?.let { audio ->
            val hue = ((audio.id.hashCode() % 360) + 360) % 360
            Color.hsv(hue.toFloat(), 0.55f, 0.72f)
        } ?: musicBaselineSeed
    }
    val seedColor: Color by derivedStateOf {
        if (state.nav.isCareRoute) {
            careSeed
        } else if (state.nav.isMusicRoute) {
            musicSeed
        } else if (state.nav.isMatchRoute) {
            matchSeed
        } else if (state.nav.isTechRoute) {
            matchSeed
        } else if (state.nav.isMoneyRoute) {
            moneySeed
        } else {
            superAppSeed
        }
    }
}



class ActiveAudio(initialAudio: Audio) {
    var current by mutableStateOf(initialAudio)
    val id: String get() = current.id
    val lines: List<AudioLine> by derivedStateOf {
        deriveLines(current)
    }

    private fun deriveLines(audio: Audio): List<AudioLine> = when {
        audio.audioLines.isNotEmpty() -> audio.audioLines
        audio.wordAlignments.isNotEmpty() -> audio.getAlignedLines()
            .mapIndexed { index, alignedLine ->
                AudioLine(
                    id = index,
                    text = alignedLine.text,
                    startMs = alignedLine.start,
                    expands = emptyList(),
                    themes = emptyList(),
                    scenes = emptyList(),
                )
            }

        else -> emptyList()
    }
}


open class ActiveShowState(val load:(Boolean) -> Unit = {}, private val onShow: () -> Unit = {}) {
    private var state by mutableStateOf(false)
    // Change this to a standard property read/write
    var value: Boolean
        get() = state
        set(v) {
            state = v
        }
    fun click() {
        state = true
        onShow()
    }
    fun hide() {
        state = false
    }
    fun toggle(){
        state = !state
    }

    var isWorking by mutableStateOf(false)
    var err by mutableStateOf("")

//    fun working(
//        scope: CoroutineScope,
//        log: LogService,
//        name: String,
//        body: suspend () -> Unit
//    ) = scope.launch {
//        isWorking = true
//        err = ""
//        try {
//            body()
//        } catch (e: Exception) {
//            log.e(e) { "[$name]" }
//            err = e.toString()
//        } finally {
//            isWorking = false
//        }
//    }

    fun working(
        scope: CoroutineScope,
        log: LogService,
        name: String,
        requireShown: Boolean = true,
        setWorking: (Boolean) -> Unit = { isWorking = it; load(it) },
        body: suspend () -> Unit
    ) = scope.launch {
        setWorking(true)
        err = ""
        try {
            if (requireShown) require(value) { "[$name] dialog not shown" }
            body()
        } catch (e: Exception) {
            log.e(e) { "[$name]" }
            err = e.toString()
        } finally {
            setWorking(false)
        }
    }
}


class ActiveFieldState(s: String="") {
    private var state by mutableStateOf(s)
    val value: String get() = state
    fun type(s: String) {
        state = s
    }
    fun clear() {
        state = ""
    }
}

class ActiveObjectState<T> {
    private var state by mutableStateOf<T?>(null)
    val value: T? get() = state
    fun select(s: T) {
        state = s
    }
    fun clear() {
        state = null
    }
}

class SetupDialog(state: State) : ActiveShowState(onShow = { state.showMenu.hide() })
class ImportDialog(val state: State) : ActiveShowState(onShow = { state.showMenu.hide() }) {



    fun addSong() = state.scope.launch {
        if (state.kv.matrixAccessToken.isEmpty()) {
            state.nav.openLogin()
            return@launch
        }
        state.isWorking = true
        try {
            val file = FileKit.openFilePicker(type = FileKitType.File("mp3")) ?: return@launch
            state.musicService.uploadAsset(file.name, file.readBytes())
        } catch (e: Exception) {
            state.log.e(e) { "[addSong]" }
            state.errorMessage = e.toString()
        } finally {
            state.isWorking = false
        }
    }

    fun importDirectory() = state.scope.launch {
        state.data.audio = null
        state.isWorking = true
//        isImporting = true
        try {
            runCatching {
                val filenames =
                    state.fs.importDirectory()            // host boundary: user picks a directory; returns its filenames

                println("files $filenames")
                // Read each file's bytes, then its id3 + xmp — in parallel across files AND across the two
                // parsers (mirrors the real handler: files.map { async { … } }.awaitAll()). Each read is
                // isolated so one file's (or one parser's) failure can't sink the others.
                val parsed = coroutineScope {
                    filenames.map { filename ->
                        async {
                            val id3 = async {
                                runCatching { state.readAudioMeta(filename) }.getOrNull()
                            }
//                            val xmp = async {
//                                    runCatching {
//                                        state.meta.readXmp(
//                                            filename
//                                        )
//                                    }.getOrNull()
//                            }
                            Pair(filename, id3.await())
                        }
                    }.awaitAll()
                }

                // One directory, MANY media types. The real handler (Studio.kt:1086-1147) branches by
                // extension and builds the matching entity from the matching metadata: Audio from id3 (mp3),
                // Video from xmp (mp4), Image from xmp (png). Only the mp3 path is wired in the real app
                // today; the mp4/png branches are stubs carrying the intent, exactly as Studio.kt leaves them.
                val newAudios = mutableListOf<Audio>()
                val newVideos = mutableListOf<Video>()
                val newImages = mutableListOf<Image>()

                parsed.forEach { (filename, audio) ->

                    when {
                        // mp3 → Audio, from its id3 (+ the embedded cover written out as its own image file).
                        filename.endsWith("mp3") -> {
                            if (audio == null) {
                                println("audio 4 u $audio")
                                error("no id3")
                            }
                            if (audio.image.isBlank()) return@launch
                            println("go1 $filename ${audio.socialMediaBlueprint}")
                            newAudios += audio

                        }
                        // mp4 → Video, from its XMP (not id3). Studio.kt:1131 sketches the eventual mapping:
                        //   Video(project = xmp.projectName, genre = xmp.dmGenre, lyrics = xmp.dmLyrics,
                        //         orgId = xmp.mmOriginalDocumentID)
                        // Not wired yet, same as the real handler: Video carries `project` but not genre/
                        // lyrics/orgId, so it waits on those fields landing on Video.
                        filename.endsWith("mp4") -> {
                            // newVideos += Video(id = ids.uuidV7(), name = filename, project = xmp?.projectName ?: "Default")
                        }
                        // png → Image, from its XMP. Empty in the real handler too (Studio.kt:1144); mapping TBD.
                        filename.endsWith("png") -> {
                        }
                    }
                }

                if (newAudios.isNotEmpty() || newVideos.isNotEmpty() || newImages.isNotEmpty()) {
                    state.db.upsert(audios = newAudios, videos = newVideos, images = newImages)
                    newAudios.forEach { state.data.audios.map[it.id] = it }
                    newVideos.forEach { state.data.videos.map[it.id] = it }
                    newImages.forEach { state.data.images.map[it.id] = it }
                }

                require(newAudios.isNotEmpty() || newVideos.isNotEmpty() || newImages.isNotEmpty())

//                state._effects.tryEmit(UiEffect.TracksImported(newAudios.size))
                println()
                Unit
            }.onFailure {
                Log.e("[importDirectory]: sorry $it")
                println("[importDirectory] $it")
//                _effects.tryEmit(UiEffect.OperationFailed(OperationKind.IMPORT))
                Unit
            }
        } finally {
            state.isWorking = false
//            isImporting = false
            state.showMenu.hide()
            println("boom ${ state.data.audios.map.isNotEmpty()}")
        }
    }
}
class SelectAudioDialogState(
    val state: State,
    private val log: LogService=createRealLogService("SelectAudioDialogState")
) : ActiveShowState(onShow = { state.showMenu.hide() }) {
    val enabled by derivedStateOf {
        state.data.audios.map.isNotEmpty()
    }
    val lyrics = ActiveFieldState()
    val project = ActiveFieldState()
    var audio = ActiveObjectState<Audio>()

    fun searchLyrics(text: String="", group:String="") = state.scope.launch {
        lyrics.type(text)
        project.type(group)
        try {
            val audios = state.db.searchAudioLyrics(lyrics.value, project.value)
            state.data.audios.map.clear()
            state.data.audios.map.putAll(audios.associateBy { it.id })
        } catch (e: Exception) {
            Log.e("[audioDialogSearchLyrics] $e")
        }
    }

    fun playAudio(audio: Audio){
        state.audioDialog.audio.select(audio)
        state.play(audio.name)
    }

    fun pauseAudio(){
        state.audio.pause()
        state.audioDialog.audio.clear()
    }

    fun confirm() = state.scope.launch {
        require(audio.value != null) {
            state.log.e {
                "[selectAudio] audio not set"
            }
        }
        state.data.action.isSelectingAudio = true
        try {
            state.data.audio = ActiveAudio(audio.value!!)

            state.data.videos.map.clear()
            state.data.videos.map.putAll(
                state.db.videos(audio.value!!.project).associateBy { it.id }
            )

            state.data.images.map.clear()
            state.data.images.map.putAll(
                state.db.images(audio.value!!.project).associateBy { it.id }
            )

            require(state.data.audio !== null)
        } catch (e: Exception) {
            log.e(e) { "fn=confirm" }
        } finally {
            state.data.action.isSelectingAudio = false
            hide()
        }
    }
}
data class SplashPage(val title: StringResource, val route: Route)

class SplashState(private val state: State) {
    val pages = listOf(
        SplashPage(Res.string.music_videos, MusicAboutRoute),
        SplashPage(Res.string.splash_care, CareRulesRoute),
        SplashPage(Res.string.splash_match, MatchCheckRoute),
    )
    val pageVideos: List<String> = listOf(
        "splash.mp4",
        "care.mp4",
    )
}


class EditLyricsDialogState(val state: State) : ActiveShowState(onShow = { state.showMenu.hide() }) {
    val lyrics = ActiveFieldState(state.data.audio?.current?.lyrics().orEmpty())
    var isSaving by mutableStateOf(false)
    fun confirmLyrics() = state.scope.launch {
        isSaving = true
        try {
            if (lyrics.value.isNotBlank() && state.data.audio!!.current.lyrics() != lyrics.value) {
                state.data.audio!!.current = state.data.audio!!.current.copy(editedLyrics = lyrics.value)
                state.upsertAudioAndReload(state.data.audio!!.current)
            }
        } catch (e: Exception) {
            state.log.e { "[confirmLyrics] $e" }
        } finally {
            isSaving = false
            lyrics.clear()
        }
    }
}

class FaqsState(
    private val state: State,
    private val log: LogService=createRealLogService("AboutFaqsDialogState")
) : ActiveShowState(onShow = { state.showMenu.hide() }) {
    val current: List<AudioQA> by derivedStateOf {
        state.data.audio?.current?.faqs ?: Audio.emptyFaqs()
    }
    private var edited by mutableStateOf<List<AudioQA>?>(null)

    var faqs: List<AudioQA>
        get() = edited ?: current      // untouched → always the live model
        set(value) { edited = value }

    var isExpanded = ActiveShowState()
    fun editQuestion(value:String, index: Int){
        faqs = faqs.toMutableList()
            .also { it[index] = it[index].copy(question = value) }
    }
    fun editAnswer(value:String, index: Int){
        faqs = faqs.toMutableList()
            .also { it[index] = it[index].copy(answer = value) }
    }

    fun generateFaqs(): Job = working(state.scope, log, ::generateFaqs.name) {
        val prompt = """
        Analyze the following lyrics and generate exactly 10 distinct, insightful questions about the track's protagonist, themes, setting, and narrative arc.
        Output strictly as a numbered list from 1 to 10.

        Lyrics:
        ${state.data.audio!!.current.lyrics()}
    """.trimIndent()
        val response = state.llm.generateText(prompt)
        val newQuestions = response.lines()
            .map { it.trim() }
            .filter { it.matches(Regex("^\\d+\\..+")) }
            .map { it.substringAfter(".").trim() }

        require(newQuestions.size == 10) { "AI did not return exactly 10 questions. Please try again." }

        val updatedQas = newQuestions.mapIndexed { i, q ->
            faqs.getOrNull(i)?.copy(question = q)
                ?: AudioQA(question = q, answer = "")
        }
        val finalQas = (updatedQas + List(10) {
            AudioQA(
                question = "",
                answer = ""
            )
        }).take(10)
        faqs = finalQas
    }

    fun confirm(): Job = working(state.scope, log, ::confirm.name) {
        state.data.audio!!.current = state.data.audio!!.current.copy(faqs = faqs)
        state.upsertAudioAndReload(state.data.audio!!.current)
        edited = null      // disarm the override — next open tracks the live model again
        hide()
    }

    fun cancel() {
        edited = null      // discard edits — cancel changes nothing
        hide()
    }
}

//val prompt = """
//            Please describe the vibe or energy of these lyrics:
//
//            $formattedQas
//        """.trimIndent()

class SocialMediaBlueprintDialogState(
    val state: State,
    private val log: LogService=createRealLogService("SocialMediaBlueprintDialogState")
) : ActiveShowState(onShow = { state.showMenu.hide() }) {
    val current by derivedStateOf {
        state.data.audio?.current?.socialMediaBlueprint ?: ""
    }
    private var edited by mutableStateOf("")
    var blueprint
        get() = edited.ifBlank { current }      // untouched → always the live model
        set(value) { edited = value }
    var isExpanded = ActiveShowState()

    fun generateBlueprint(): Job = working(state.scope, log, ::generateBlueprint.name) {
        require(state.data.audio?.current?.faqs !== null)
        val faqs = state.data.audio?.current?.faqs!!
        val formattedQas = faqs.joinToString("\n\n") { "Q: ${it.question}\nA: ${it.answer}" }
        //        val prompt = """
//            Based on the following questions and answers about a music track, generate a comprehensive, detailed 'About' section for the track.
//            This should be a cohesive summary exploring the protagonist, themes, setting, and narrative arc in a long, unstructured format.
//
//            $formattedQas
//        """.trimIndent()
        val prompt = """
            i gave prompt of i want to make a post to multiple platforms about a song. Each platform gets following perspective: - What is song about as creator - Your feedback as creator - Snapshot perspective from moment in lyric - How you went about creating song and inspirations - Couples one liner memes derived from song In order to do this, instead of answering these questions one by one. I figured it'll be better if i told AI to ask me questions to more fluidly transfer context and mind state to understand beyond fixed questions. I've been thinking about how to do this deterministically. I'm thinking have AI ask me 10 questions then have json object with 10 - 20 fields representing aspects deduced from questionning. Then I can use that object to derive the actual wording to post to each platform.
            Please just return formatted json as reponse nothing more nothing less.

            $formattedQas
        """.trimIndent()
        val res = state.llm.generateText(prompt)
        require(runCatching { AppJson.parseToJsonElement(res) }.getOrNull() is JsonObject) { "LLM did not return a JSON object: $res" }
        blueprint = res
    }

    fun confirm(): Job = working(state.scope, log, ::confirm.name) {
        with(state.data.audio!!) {
            current = current.copy(
                socialMediaBlueprint = blueprint
            )
            println("the bluepint ${current.socialMediaBlueprint}")
            state.upsertAudioAndReload(current)
            println("blueprint saved")
        }
        blueprint = ""
        hide()
    }

    fun cancel() {
        edited = ""      // discard edits — cancel changes nothing
        hide()
    }
}






class StoryboardState(val state: State)  {
    var isProcessing by mutableStateOf(false)


    fun process(){

    }
}


//
//class ActiveShow {
//    var showMenu = ActiveShowState()
//    var showStoryboardDialog = ActiveShowState() { showMenu.hide() }
//    var showSettingsDialog = ActiveShowState() { showMenu.hide() }
//    var showVeoPromptDialog = ActiveShowState() { showMenu.hide() }
//    var showProjectLibraryDialog = ActiveShowState() { showMenu.hide() }
//    var showNewAudioDialog by mutableStateOf<String?>(null)
//    var showSunoSyncDialog = ActiveShowState() { showMenu.hide() }
//    var showMatrixLibraryDialog = ActiveShowState() { showMenu.hide() }
//    var showExportDialog = ActiveShowState() { showMenu.hide() }
////    var selectAudio = ActiveSelectAudioDialog  {showMenu.hide() }
//    var showImportFilesDialog = ActiveShowState() { showMenu.hide() }
//    var showForm = ActiveShowState() { showMenu.hide() }
//    var showProtagonistDialog = ActiveShowState() { showMenu.hide() }
//    var showLyricsDialog = ActiveShowState() { showMenu.hide() }
//    var showAboutDialog = ActiveShowState() { showMenu.hide() }
//    var showElevenLabsDialog = ActiveShowState() { showMenu.hide() }
//    var showWordAlignmentDialog = ActiveShowState() { showMenu.hide() }
//    var showQwen3AsrDialog = ActiveShowState() { showMenu.hide() }
//    var showLmStudioDialog = ActiveShowState() { showMenu.hide() }
//    var showKeysConfigDialog = ActiveShowState() { showMenu.hide() }
//    var showSequenceEditor = ActiveShowState() { showMenu.hide() }
//
//
//}

class ActiveAction {
    var isSelectingAudio by mutableStateOf(false)
    var isAligning by mutableStateOf(false)
    var isSavingLyrics by mutableStateOf(false)
    var isSavingAlignment by mutableStateOf(false)
    var isSavingProtagonist by mutableStateOf(false)
    var isDirectoryConnected by mutableStateOf(false)
    var isSavingAbout by mutableStateOf(false)
    var isGeneratingAbout by mutableStateOf(false)
}

class ActiveDraft {
    var lyricsDraft by mutableStateOf<String?>(null)
    var wordAlignmentDraft by mutableStateOf<WordAlignmentDraft?>(null)
    var protagonistDraft by mutableStateOf<MemoryFile?>(null)
}

//class ActiveField {
//    var filters by mutableStateOf(ActiveFieldState())
//    var lyricsDraft by mutableStateOf<String?>(null)
//    var wordAlignmentDraft by mutableStateOf<WordAlignmentDraft?>(null)
//    var protagonistDraft by mutableStateOf<MemoryFile?>(null)
//}

class StateAudios {
    val map = mutableStateMapOf<String, Audio>()
    val list: List<Audio> by derivedStateOf {
        map.values.toList()
    }
}

class StateImages {
    val map = mutableStateMapOf<String, Image>()
    val list: List<Image> by derivedStateOf {
        map.values.toList()
    }
    val leemImageMap by derivedStateOf {
        list.groupBy { "${it.audioLineText}|${it.theme}" }
    }
}

class StateVideos {
    val map = mutableStateMapOf<String, Video>()
    val list: List<Video> by derivedStateOf {
        map.values.toList()
    }
    val linstVideos by derivedStateOf  {
//        val groupedResult = flib.groupVideosByAudio(activeProject.videos)
        val grouped = list.groupBy { it.audioLineId.toString() }
//        println("[diag] linstVideos: totalVideos=${activeProject.videos.size}, keyCount=${grouped.keys.size}, sampleKeys=${grouped.keys.take(3).toList()}")
        grouped
    }
}

class ActiveData(onShow: () -> Unit = {}) {
    var filters by mutableStateOf<ActiveFilters>(ActiveFilters())
    var edits by mutableStateOf<ActiveEdits>(ActiveEdits())
    var projects by mutableStateOf<ActiveProjects>(ActiveProjects())

    var videos = StateVideos()
    var audios = StateAudios()
    var images = StateImages()
    var files_ = mutableStateMapOf<Int, XmpItem>()
    var matrixs = mutableStateMapOf<String, Matrix>()


    var audio by mutableStateOf<ActiveAudio?>(null)
    private val cost by derivedStateOf {
        val lines = audio?.lines ?: return@derivedStateOf Cost() to emptyMap()
        val leem = images.leemImageMap
        val map = audio?.lines?.associateWith { line ->
            Cost().apply {
                calculate(listOf(line), leem)
            }
        } ?: emptyMap()
        val total = Cost().apply {
            calculate(lines, leem)
        }
        total to map
    }
    val totalCost: Cost
        get() = cost.first
    val lineCostMap: Map<AudioLine, Cost>
        get() = cost.second

//    var projectName by mutableStateOf<String?>(null)
    val projectVideos = mutableStateListOf<Video>()
    val projectImages = mutableStateListOf<Image>()

    var activeSequence by mutableStateOf<Video?>(null)

    val leemImageMap: Map<String, List<Image>> by derivedStateOf {
        images.list.groupBy { "${it.audioLineText}|${it.theme}" }
    }

    val draft by mutableStateOf(ActiveDraft())
    val action by mutableStateOf(ActiveAction())
//    val dialogs by mutableStateOf(ActiveShow())

    private fun deriveLines(audio: Audio): List<AudioLine> = when {
        audio.audioLines.isNotEmpty() -> audio.audioLines
        audio.wordAlignments.isNotEmpty() -> audio.getAlignedLines()
            .mapIndexed { index, alignedLine ->
                AudioLine(
                    id = index,
                    text = alignedLine.text,
                    startMs = alignedLine.start,
                    expands = emptyList(),
                    themes = emptyList(),
                    scenes = emptyList(),
                )
            }

        else -> emptyList()
    }
}

class Cost {
    private val expandCost = 0.01
    private val sceneCost = 0.01
    private val imageCost = 0.10
    val videoCost = 3.2

    var expands by mutableStateOf(0)
    var scenes by mutableStateOf(0)
    var images by mutableStateOf(0)

    var missingExpands by mutableStateOf(0)
    var missingScenes by mutableStateOf(0)
    var missingImages by mutableStateOf(0)

    val completion: Int get() {
        val completed = expands + scenes + images
        val total = completed + missingExpands + missingScenes + missingImages
        return if (total > 0) ((completed.toFloat() / total) * 100).toInt() else 0
    }

    val expandsCost get() = expands * expandCost
    val scenesCost get() = scenes * sceneCost
    val imagesCost get() = images * imageCost
    val missingExpandsCost get() = missingExpands * expandCost
    val missingScenesCost get() = missingScenes * sceneCost
    val missingImagesCost get() = missingImages * imageCost

    val totalCost get() = expandsCost + scenesCost + imagesCost
    val totalPendingCost get() = missingExpandsCost + missingScenesCost + missingImagesCost

    fun calculate(audioLines: List<AudioLine>, imageMap: Map<String, List<Image>>) {
        var tempExpand = 0
        var tempScene = 0
        var tempImage = 0
        var tempMissingExpand = 0
        var tempMissingScene = 0
        var tempMissingImage = 0
        audioLines.forEach { line ->
            line.themes.forEachIndexed { index, theme ->
                if (theme.theme.isNotBlank()) {
                    // Count Expands
                    if (theme.expand.isNullOrBlank()) {
                        tempMissingExpand++
                    } else {
                        tempExpand++
                    }

                    // Count Scenes
                    if (theme.scene.isNullOrBlank()) {
                        tempMissingScene++
                    } else {
                        tempScene++
                    }

                    // Count Images
                    if (!imageMap.containsKey("${line.text}|${theme.theme}")) {
                        tempMissingImage++
                    } else {
                        tempImage++
                    }
                }
            }
        }
        expands = tempExpand
        scenes = tempScene
        images = tempImage
        missingExpands = tempMissingExpand
        missingScenes = tempMissingScene
        missingImages = tempMissingImage
    }
}



const val TEST_FS_URL = "https://fs.femi.market"
const val TEST_DB_URL = "https://db.femi.market"
const val TEST_CODEC_URL = "https://codec.femi.market"
const val TEST_META_URL = "https://meta.femi.market"
const val TEST_LLM_URL = "https://llm.femi.market"
const val TEST_API_URL = "https://femi.market"
const val TEST_DB_USER = "admin"
const val TEST_DB_PASS = "abc123"
const val TEST_MATRIX_ACCESS_TOKEN = ""
const val TEST_CANDIDATE_ID = ""
const val TEST_MATCH_SESSION_ID = ""
const val TEST_MATRIX_REFRESH_TOKEN = ""
const val TEST_MATRIX_CLIENT_ID = ""
const val TEST_MATRIX_URL = "https://matrix.femi.market"
const val ID = "3"
const val FS_URL_KEY = "FS_URL_KEY$ID"
const val DB_URL_KEY = "DB_URL_KEY$ID"
const val CODEC_URL_KEY = "CODEC_URL_KEY$ID"
const val META_URL_KEY = "META_URL_KEY$ID"
const val LLM_URL_KEY = "LLM_URL_KEY$ID"
const val API_URL_KEY = "API_URL_KEY$ID"
const val CANDIDATE_ID_KEY = "CANDIDATE_ID_KEY$ID"
const val MATCH_SESSION_ID_KEY = "MATCH_SESSION_ID_KEY$ID"
const val DB_USER_KEY = "DB_USER_KEY$ID"
const val DB_PASS_KEY = "DB_PASS_KEY$ID"
const val MUSIC_INFO_KEY = "MUSIC_INFO_KEY$ID"
const val CARE_LOGIN_KEY = "CARE_LOGIN_KEY$ID"
const val MATRIX_ACCESS_TOKEN_KEY = "MATRIX_ACCESS_TOKEN_KEY$ID"
const val MATRIX_REFRESH_TOKEN_KEY = "MATRIX_REFRESH_TOKEN_KEY$ID"
const val MATRIX_CLIENT_ID_KEY = "MATRIX_CLIENT_ID_KEY$ID"
const val MATRIX_URL_KEY = "MATRIX_URL_KEY$ID"
const val TABLE_AUDIO = "audios"
const val TABLE_VIDEO = "videos"
const val TABLE_IMAGE = "images"
const val TABLE_MATRIX = "matrixs"

const val AUDIO_FILE = "song.mp3"
const val VIDEO_FILE = "video.mp4"
val RGBA_FILE = "video.rgba".encodeToByteArray()

const val AUDIO_IMAGE_FILE = "song.png"
const val LYRICS = "Hello world\nsecond line"
const val ALBUM = "Neon Nights"
const val EDITED_LYRICS = "Hello world\nsecond line\nthird line"
val SONG_BYTES = ByteArray(32) { it.toByte() }
const val PROTAGONIST_IMAGE_FILE = "hero.png"
val PROTAGONIST_IMAGE_BYTES = byteArrayOf(9, 9, 9)
const val AUDIO_PROTAGONIST_FILE = "song-protagonist.png"
const val REPICK_IMAGE_FILE = "repick.jpg"
val REPICK_IMAGE_BYTES = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 7)
const val AUDIO_PROTAGONIST_FILE_JPG = "song-protagonist.jpg"
const val STORYBOARD_THEME = "neon skyline"
const val IMAGE_PROMPT = "a neon skyline at night, cinematic wide shot"
const val LINE0_TEXT = "Hello world"
// Built in setUp(), AFTER Dispatchers.setMain — see the note there.
//    lateinit var app: StudioHarness

const val DIR_KEY = "fileStoreRootPath12DDddeeeE22323"
const val TOKEN_SYMBOL = "FEMI"

fun String.toRgbaFileName(): String {
    require(endsWith(".mp4", ignoreCase = true)) {
        "File must end with .mp4 extension (got: $this)"
    }
    return replaceAfterLast('.', "rgba")
}

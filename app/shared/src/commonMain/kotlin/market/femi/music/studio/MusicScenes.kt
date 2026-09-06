package market.femi.music.studio

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import femi.app.shared.generated.resources.Res
import market.femi.BlogSection
import market.femi.BlogTitle
import femi.app.shared.generated.resources.music_themes_context_label
import femi.app.shared.generated.resources.music_themes_context_placeholder
import femi.app.shared.generated.resources.music_themes_context_support
import femi.app.shared.generated.resources.music_themes_goal_placeholder
import femi.app.shared.generated.resources.music_themes_goal_support
import femi.app.shared.generated.resources.music_themes_goal_label
import femi.app.shared.generated.resources.music_themes_meaning
import femi.app.shared.generated.resources.music_themes_meaning_title
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import market.femi.ActiveFieldState
import market.femi.ActiveShowState
import market.femi.State
import org.jetbrains.compose.resources.stringResource
import market.femi.StoryboardTimelineHeader
import market.femi.models.AudioLine
import market.femi.models.AudioTheme
import market.femi.models.Image
import market.femi.models.ImageModel
import market.femi.toHoursMinuteSeconds
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

//
//fun decodeJpg(jpgBytes: ByteArray): ImageBitmap {
//    return jpgBytes.decodeToImageBitmap()
//}
//
//@Composable
//fun JpgGpuCanvas(
//    jpgBytes: ByteArray?,
//    modifier: Modifier = Modifier
//) {
//    Canvas(modifier = modifier.fillMaxSize()) {
//        jpgBytes?.let { bytes ->
//            val bitmap = decodeJpg(bytes)
//            // Draws directly on the GPU canvas
//            drawImage(image = bitmap)
//        }
//    }
//}
//

class MusicScenesState(val state: State) : ActiveShowState(onShow = { state.showMenu.hide() }) {
    var isCommittingLine by mutableStateOf(false)
    private var picked by mutableStateOf<AudioLine?>(null)
    private var edited by mutableStateOf<AudioLine?>(null)
    val current by derivedStateOf { state.data.audio?.lines?.find { it.id == picked?.id } }
    var line: AudioLine?
        get() = edited ?: current                 // untouched → always the live model
        set(value) { edited = value }
    fun pickLine(it: AudioLine){
        picked = it
        edited = null
    }
    private var commitJob: Job? = null
    private fun commitLine() {
        commitJob?.cancel()
        commitJob = state.scope.launch {
            delay(1000.milliseconds)
            working(
                scope = state.scope,
                log = state.log,
                name = "commitLine",
                requireShown = false,
                setWorking = { isCommittingLine = it },
            ) {
                with(state.data.audio!!) {
                    val lines = current.audioLines.toMutableList()
                    lines[lines.indexOfFirst { it.id == line!!.id }] = line!!
                    current = current.copy(audioLines = lines)
                    state.upsertAudioAndReload(current)
                }
                edited = null
            }.join()
        }
    }
    fun setLineContext(context: String) {
        line = line?.copy(context = context)?.also { commitLine() }
    }
    fun setLineGoal(goal: String){
        line = line?.copy(goal = goal)?.also { commitLine() }
    }

    val audioLine by derivedStateOf { state.nav.lineRouteAudioLine }
    private var editedThemes by mutableStateOf<List<AudioTheme>>(emptyList())
    private var editedExpands by mutableStateOf<List<String>>(emptyList())
    private var editedScenes by mutableStateOf<List<String>>(emptyList())
    var draftThemes
        get() = editedThemes.ifEmpty { state.nav.lineRouteAudioLine?.themes.orEmpty() }
        set(value) { editedThemes = value }
    var expands: List<String>
        get() = editedExpands.ifEmpty { state.nav.lineRouteAudioLine?.expands.orEmpty() }
        set(value) { editedExpands = value }
    var scenes: List<String>
        get() = editedScenes.ifEmpty { state.nav.lineRouteAudioLine?.scenes.orEmpty() }
        set(value) { editedScenes = value }
    val isGenerated = state.nav.lineRouteAudioLine?.scenes.orEmpty().isNotEmpty() || state.nav.lineRouteAudioLine?.expands.orEmpty().isNotEmpty()


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


@Composable
fun MusicScenes(
    state: State,
){
//    val audio100 = activeEdits.audio ?: return@Scaffold
    LazyColumn(
//        state = audioWorkspaceColumnState,
        modifier = Modifier
            .fillMaxWidth()
//            .weight(1f)
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(vertical = 24.dp)
    ) {

        item {
            BlogTitle(title = state.scenes.line!!.text)
        }

        item {
            BlogSection(
                title = stringResource(Res.string.music_themes_meaning_title),
                desc = stringResource(Res.string.music_themes_meaning),
            ) {
                OutlinedTextField(
                    enabled = !state.scenes.isCommittingLine,
                    value = state.scenes.line!!.context.orEmpty(),
                    onValueChange = { state.scenes.setLineContext(it) },
                    label = { Text(stringResource(Res.string.music_themes_context_label)) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null) },
                    placeholder = { Text(stringResource(Res.string.music_themes_context_placeholder)) },
                    supportingText = { Text(stringResource(Res.string.music_themes_context_support)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    enabled = !state.scenes.isCommittingLine,
                    value = state.scenes.line!!.goal.orEmpty(),
                    onValueChange = { state.scenes.setLineGoal(it) },
                    label = { Text(stringResource(Res.string.music_themes_goal_label)) },
                    leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null) },
                    placeholder = { Text(stringResource(Res.string.music_themes_goal_placeholder)) },
                    supportingText = { Text(stringResource(Res.string.music_themes_goal_support)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // 4: STORYBOARD TIMELINE
        item {
            StoryboardTimelineHeader(
                state,
//                onGenerateClick = {
//                    scope.launch {
//                        processPipeline(
//                            generateText = {
//                                callLmStudioResponse(localLmStudioUrl,it)
//                            },
//                            generateImage = { generateImage(it, viewport) },
//                            snack = snack,
//                            audio = audio100,
//                            audioLines = audioLines,
//                            imageMap = leemImageMap,
//                            setWorking = { isWorking = it },
//                            onLinesUpdated = { newLines ->
//                                val updatedAudio = audio100.copy(audioLines = newLines)
//                                activeEdits.audio = updatedAudio
//                                scope.launch { viewport.appendData(db=db, filters=filters, audios=listOf(updatedAudio)) }
//                            }
//                        )
//                    }
//                }
            )
        }

        item {
            AudioLineCard(state)
        }
//        items(state.timeline.activeAudioLines, key = { it.startMs }) { audioLine ->
//            val globalIndex = state.scenes.lines.indexOf(audioLine)
//            AudioLineCard(
//                state,
////                onSetExport = { selectedVideo, lineVideos ->
////                    val updatedVideos = lineVideos.map { it.copy(export = it.id == selectedVideo.id) }
////                    scope.launch { viewport.appendData(db=db, filters=filters, videos=updatedVideos) }
////                },
////                onAddToVideoQueue = { selectedImage ->
////                    if (videoKeyframes.size < 3 && !videoKeyframes.contains(selectedImage)) {
////                        videoKeyframes = videoKeyframes + selectedImage
////                    } else if (videoKeyframes.size >= 3) {
////                        scope.launch { snack.showSnackbar("Maximum 3 frames allowed.") }
////                    }
////                },
////                audioLine = audioLine,
////                imageMap = leemImageMap,
////                linstVideos = linstVideos,
////                onEditVideo = { activeEdits.video = it },
////                updateItem = { updatedLine ->
////                    println("global index $globalIndex")
////                    if (globalIndex != -1) {
////                        val newAudioLines = audioLines.toMutableList()
////                        newAudioLines[globalIndex] = updatedLine
////                        activeEdits.audio = audio100.copy(audioLines = newAudioLines)
////                    }
////                },
////                onEditSequence = {},
////                db = db
//            )
//        }
//                                itemsIndexed(audioLines) { index, audioLine ->
//                                    AudioLineCard(
//                                        onSetExport = { selectedVideo, lineVideos ->
//                                            val updatedVideos = lineVideos.map { it.copy(export = it.id == selectedVideo.id) }
//                                            scope.launch { viewport.appendData(db=db, filters=filters, videos=updatedVideos) }
//                                        },
//                                        onAddToVideoQueue = { selectedImage ->
//                                            if (videoKeyframes.size < 3 && !videoKeyframes.contains(selectedImage)) {
//                                                videoKeyframes = videoKeyframes + selectedImage
//                                            } else if (videoKeyframes.size >= 3) {
//                                                scope.launch { snack.showSnackbar("Maximum 3 frames allowed.") }
//                                            }
//                                        },
//                                        audioLine = audioLine,
//                                        imageMap = leemImageMap,
//                                        linstVideos = linstVideos, // 👉 PASS IT HERE
//                                        onEditVideo = { activeEdits.video = it },
//                                        updateItem = { updatedLine ->
//                                            val newAudioLines = audioLines.toMutableList()
//                                            newAudioLines[index] = updatedLine
//                                            activeEdits.audio = audio100.copy(audioLines = AppJson.encodeToString(ListSerializer(AudioLine.serializer()), newAudioLines))
//                                        },
//                                        onEditSequence = { videos -> activeSequence = Pair(audioLine.text, videos) }
//                                    )
//                                }

    }
}

@OptIn(ExperimentalStdlibApi::class, ExperimentalUuidApi::class)
@Composable
fun AudioLineCard(
    state: State,
//    onSetExport: (Video, List<Video>) -> Unit,
//    audioLine: AudioLine,
//    imageMap: Map<String, List<Image>>,
//    linstVideos: Map<String, List<Video>>,
//    updateItem: (AudioLine) -> Unit,
//    onAddToVideoQueue: (Image) -> Unit,
//    onEditVideo: (Video) -> Unit, // 👉 ADD THIS LINE
//    onEditSequence: (List<Video>) -> Unit, // 👉 2. UPDATE SIGNATURE
//    db: IDBPDatabase
) {
//    var draftThemes by remember(audioLine.themes) { mutableStateOf(audioLine.themes.toList()) }
//    var draftExpands by remember(audioLine.expands) { mutableStateOf(audioLine.expands.orEmpty().toList()) }
//    var draftScenes by remember(audioLine.scenes) { mutableStateOf(audioLine.scenes.orEmpty().toList()) }
//    val isGenerated = audioLine.scenes.orEmpty().isNotEmpty() || audioLine.expands.orEmpty().isNotEmpty()
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        /*
        * AudioLineCard Header
        * */
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = state.scenes.line!!.text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = state.scenes.line!!.startMs.toHoursMinuteSeconds(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (state.scenes.draftThemes.isEmpty()) {
            FilledTonalButton(
                onClick = { state.scenes.draftThemes = mutableListOf(
                    AudioTheme(
                        theme = "",
                        expand = null,
                        scene = null
                    )
                )},
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Setup", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Setup Storyboard Scene")
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val flattenedCards = state.scenes.draftThemes.flatMapIndexed { i, theme ->

                    val saved = state.scenes.line!!.themes.getOrNull(i) !== null

                    val matchedImages = state.data.leemImageMap["${state.scenes.line!!.text}|${theme.theme}"]
                    if (matchedImages.isNullOrEmpty()) {
                        listOf(Triple(i, theme, Pair(null as Image?, saved)))
                    } else {
                        matchedImages.map { Triple(i, theme, Pair(it, saved)) }
                    }
                }
                flattenedCards.forEach { (i, theme, pair) ->
                    key(i) {
                        val matchedImage = pair.first
                        val themeSaved = pair.second

                        val sceneText = theme.scene
                        val expandText = theme.expand

                        val uiImage = matchedImage?.copy(
                            audioLineGoal = state.scenes.line!!.goal,
                            audioLineContext = state.scenes.line!!.context,
                            theme = theme.theme,
                            scene = sceneText,
                            expand = expandText
                        ) ?: Image(
                            id = Uuid.random().toString(),
                            model = ImageModel.Unknown,
                            name = "",
                            project = "Default",
                            audioLineText = state.scenes.line!!.text,
                            audioLineGoal = state.scenes.line!!.goal,
                            audioLineContext = state.scenes.line!!.context,
                            startMs = state.scenes.line!!.startMs,
                            theme = theme.theme,
                            expand = expandText,
                            scene = sceneText,
                        )

//                        var isEditingTheme by remember { mutableStateOf(uiImage.theme.isNullOrBlank() && !isGenerated) }
//                        var isCardExpanded by remember { mutableStateOf(uiImage.theme.isNullOrBlank() && !isGenerated) }
                        var isCardExpanded = true
//                        var showImageMenu by remember { mutableStateOf(false) }
                        var showImageMenu by remember { mutableStateOf(false) }
//                        var showThemeMenu by remember { mutableStateOf(false) }

//                        MediaSlotCard(
//                            isExpanded = isCardExpanded,
//                            mediaContent = {
//                                Box(){
//                                    AsyncImage(
//                                        model = state.kv.withFsUrl(uiImage.name),
//                                        contentDescription = null,
//                                        modifier = Modifier.fillMaxSize()
//                                    )
//                                    Box(modifier = Modifier.align(Alignment.TopEnd)) {
//                                        Box {
//                                            IconButton(
//                                                onClick = { showImageMenu = true },
//                                                modifier = Modifier.padding(8.dp).background(
//                                                    Color.Black.copy(alpha = 0.5f),
//                                                    CircleShape
//                                                ).size(32.dp)
//                                            ) {
//                                                Icon(
//                                                    Icons.Default.MoreVert,
//                                                    "Options",
//                                                    tint = Color.White,
//                                                    modifier = Modifier.size(18.dp)
//                                                )
//                                            }
//                                            DropdownMenu(
//                                                expanded = showImageMenu,
//                                                onDismissRequest = { showImageMenu = false }) {
//                                                if (uiImage.name.isNotBlank()) {
//                                                    DropdownMenuItem(
//                                                        text = { Text("Add to Video Sequence") },
//                                                        leadingIcon = {
//                                                            Icon(
//                                                                Icons.Default.Add,
//                                                                "Add",
//                                                                tint = MaterialTheme.colorScheme.primary
//                                                            )
//                                                        },
//                                                        onClick = {
//                                                            showImageMenu =
//                                                                false; onAddToVideoQueue(uiImage)
//                                                        }
//                                                    )
//                                                }
//                                                DropdownMenuItem(
//                                                    text = {
//                                                        Text(
//                                                            "Delete",
//                                                            color = MaterialTheme.colorScheme.error
//                                                        )
//                                                    },
//                                                    leadingIcon = {
//                                                        Icon(
//                                                            Icons.Default.Delete,
//                                                            "Delete",
//                                                            tint = MaterialTheme.colorScheme.error
//                                                        )
//                                                    },
//                                                    onClick = {
//                                                        showImageMenu = false
//                                                        println("what is index $i and size draftThemes ${state.themeScreenState.draftThemes.size}")
//                                                        val newThemes = state.themeScreenState.draftThemes.toMutableList()
//                                                            .apply { removeAt(i) }
//                                                        val newExpands =
//                                                            state.themeScreenState.expands.toMutableList()
//                                                                .apply { if (size > i) removeAt(i) }
//                                                        val newScenes = state.themeScreenState.scenes.toMutableList()
//                                                            .apply { if (size > i) removeAt(i) }
//
//                                                        state.themeScreenState.draftThemes = newThemes
//                                                        state.themeScreenState.expands = newExpands
//                                                        state.themeScreenState.scenes = newScenes
//
//                                                        val newContext =
//                                                            if (newThemes.isEmpty()) null else state.themeScreenState.audioLine!!.context
//                                                        val newGoal =
//                                                            if (newThemes.isEmpty()) null else state.themeScreenState.audioLine!!.goal
//
//                                                        updateItem(
//                                                            state.themeScreenState.audioLine!!.copy(
//                                                                themes = state.themeScreenState.draftThemes,
//                                                                expands = state.themeScreenState.expands,
//                                                                scenes = state.themeScreenState.scenes,
//                                                                context = newContext,
//                                                                goal = newGoal
//                                                            )
//                                                        )
//                                                    }
//                                                )
//                                            }
//                                        }
//                                    }
//                                }
//                                NativeMedia(
//                                    filename = uiImage.name,
//                                    type = MediaType.Image,
//                                    modifier = Modifier.fillMaxSize(),
//                                    topRight = {
//
//                                    }
//                                )
//                            },
//                            headlineContent = {
//                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
//                                    Text(if (isGenerated) uiImage.theme ?: "" else "Theme", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
//                                    BasicTextField(
//                                        enabled = isEditingTheme && !isGenerated,
//                                        value = if (isGenerated) uiImage.model.toString() else uiImage.theme ?: "",
//                                        onValueChange = { newTheme -> draftThemes = draftThemes.toMutableList().apply { set(i, get(i).copy(theme = newTheme)) } },
//                                        textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
//                                        modifier = Modifier.fillMaxWidth(),
//                                        decorationBox = { inner -> if (uiImage.theme.isNullOrEmpty()) Text("Add Theme...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)); inner() }
//                                    )
//                                }
//                            },
//                            trailingContent = {
//                                Row(verticalAlignment = Alignment.CenterVertically) {
//                                    if (!isGenerated) {
//                                        Box {
//                                            IconButton(onClick = { showThemeMenu = true }, modifier = Modifier.size(32.dp)) {
//                                                Icon(Icons.Default.MoreVert, "Options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
//                                            }
//                                            DropdownMenu(expanded = showThemeMenu, onDismissRequest = { showThemeMenu = false }) {
//                                                DropdownMenuItem(
//                                                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
//                                                    leadingIcon = { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error) },
//                                                    onClick = {
//                                                        showThemeMenu = false
//                                                        val newThemes = draftThemes.toMutableList().apply { removeAt(i) }
//                                                        val newExpands = draftExpands.toMutableList().apply { if (size > i) removeAt(i) }
//                                                        val newScenes = draftScenes.toMutableList().apply { if (size > i) removeAt(i) }
//                                                        draftThemes = newThemes
//                                                        draftExpands = newExpands
//                                                        draftScenes = newScenes
//                                                        val newContext = if (newThemes.isEmpty()) null else audioLine.context
//                                                        val newGoal = if (newThemes.isEmpty()) null else audioLine.goal
//                                                        updateItem(audioLine.copy(themes = draftThemes, expands = draftExpands, scenes = draftScenes, context = newContext, goal = newGoal))
//                                                    }
//                                                )
//                                                println("is uiimage.theme null or blank ${uiImage.theme}")
//                                                if (!themeSaved && isEditingTheme) {
//                                                    DropdownMenuItem(text = { Text("Save") }, leadingIcon = { Icon(Icons.Default.Check, "Save", tint = MaterialTheme.colorScheme.primary) }, onClick = { showThemeMenu = false; isEditingTheme = false; updateItem(audioLine.copy(themes = draftThemes)) })
//                                                }
//                                                if (!isEditingTheme) {
//                                                    DropdownMenuItem(text = { Text("Edit") }, leadingIcon = { Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary) }, onClick = { showThemeMenu = false; isEditingTheme = true })
//                                                    DropdownMenuItem(text = { Text("Add Theme") }, leadingIcon = { Icon(Icons.Default.Add, "Add", tint = MaterialTheme.colorScheme.primary) }, onClick = { showThemeMenu = false; draftThemes = draftThemes + AudioTheme(theme = "", expand = null, scene = null) })
//                                                }
//                                            }
//                                        }
//                                    }
//
//                                    if (isGenerated) {
//                                        IconButton(onClick = { isCardExpanded = !isCardExpanded }, modifier = Modifier.size(32.dp)) {
//                                            Icon(
//                                                imageVector = if (isCardExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
//                                                contentDescription = "Expand",
//                                                modifier = Modifier.size(20.dp),
//                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
//                                            )
//                                        }
//                                    }
//                                }
//                            },
//                            expandedContent = {
//                                if (!isGenerated) {
//                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
//                                        Text("Line Context", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
//                                        BasicTextField(value = uiImage.audioLineContext ?: "", onValueChange = { updateItem(audioLine.copy(context = it)) }, textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface), modifier = Modifier.fillMaxWidth(), decorationBox = { inner -> if (uiImage.audioLineContext.isNullOrBlank()) Text("Add Line Context...", style=MaterialTheme.typography.bodySmall, color=MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.5f)); inner() })
//                                    }
//                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
//                                        Text("Line Goal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
//                                        BasicTextField(value = uiImage.audioLineGoal ?: "", onValueChange = { updateItem(audioLine.copy(goal = it)) }, textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface), modifier = Modifier.fillMaxWidth(), decorationBox = { inner -> if (uiImage.audioLineGoal.isNullOrBlank()) Text("Add Line Goal...", style=MaterialTheme.typography.bodySmall, color=MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.5f)); inner() })
//                                    }
//                                } else {
//                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
//                                        Text("Goal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
//                                        Text(uiImage.audioLineGoal.takeIf { !it.isNullOrBlank() } ?: "No Goal provided.", style = MaterialTheme.typography.bodySmall)
//                                    }
//                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
//                                        Text("Context", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
//                                        Text(uiImage.audioLineContext.takeIf { !it.isNullOrBlank() } ?: "No Context provided.", style = MaterialTheme.typography.bodySmall)
//                                    }
//                                    if (!uiImage.expand.isNullOrBlank()) {
//                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
//                                            Text("Generated Expand", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
//                                            Text(uiImage.expand, style = MaterialTheme.typography.bodySmall)
//                                        }
//                                    }
//                                    if (!uiImage.scene.isNullOrBlank()) {
//                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
//                                            Text("Generated Scene", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
//                                            Text(uiImage.scene, style = MaterialTheme.typography.bodySmall)
//                                        }
//                                    }
//                                }
//                            }
//                        )
                    }
                }
            }
        }

//        val lineVideos = linstVideos[audioLine.id.toString()] ?: emptyList()
//        if (lineVideos.isNotEmpty()) {
//
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(horizontal = 8.dp, vertical = 12.dp)
//                    .horizontalScroll(rememberScrollState()),
//                horizontalArrangement = Arrangement.spacedBy(12.dp)
//            ) {
//                lineVideos.forEach { video ->
//                    key(video.id) {
//                        var isExpanded by remember { mutableStateOf(false) }
//                        var showMenu by remember { mutableStateOf(false) }
//                        val videoState = remember { NativeVideoState() }
//
//                        // Decode the original drafted image ONLY to extract the Theme
//                        val sourceImage = remember(video.inputImages) {
//                            video.inputImages?.firstOrNull()
//                        }
//
//                        val isExported = video.export == true
//                        MediaSlotCard(
//                            modifier = Modifier
//                                .width(180.dp)
//                                .animateContentSize()
//                                .border(
//                                    width = if (isExported) 3.dp else 0.dp,
//                                    color = if (isExported) MaterialTheme.colorScheme.primary else Color.Transparent,
//                                    shape = MaterialTheme.shapes.medium
//                                ),
//                            isExpanded = isExpanded,
//                            mediaContent = {
//                                NativeMedia(
//                                    filename = video.name,
//                                    type = MediaType.Video(state = videoState, showControls = false),
//                                    modifier = Modifier.fillMaxSize(),
//                                    topRight = {
//                                        Box {
//                                            IconButton(
//                                                onClick = { showMenu = true },
//                                                modifier = Modifier.padding(8.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape).size(32.dp)
//                                            ) {
//                                                Icon(Icons.Default.MoreVert, "Options", tint = Color.White, modifier = Modifier.size(18.dp))
//                                            }
//                                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
//                                                DropdownMenuItem(
//                                                    text = { Text("Edit Clip") },
//                                                    leadingIcon = { Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary) },
//                                                    onClick = {
//                                                        showMenu = false
//                                                        onEditVideo(video)
//                                                    }
//                                                )
//                                                DropdownMenuItem(
//                                                    text = { Text("Set as Export") },
//                                                    leadingIcon = { Icon(Icons.Default.Check, "Export", tint = MaterialTheme.colorScheme.primary) },
//                                                    onClick = {
//                                                        showMenu = false
//                                                        onSetExport(video, lineVideos)
//                                                    }
//                                                )
//                                            }
//                                        }
//                                    },
//                                    bottomRight = {
//                                        val isMuted by videoState.isMuted.collectAsState()
//                                        IconButton(
//                                            onClick = { videoState.mute(!isMuted) },
//                                            modifier = Modifier
//                                                .padding(8.dp)
//                                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
//                                                .size(32.dp)
//                                        ) {
//                                            Icon(
//                                                imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
//                                                contentDescription = if (isMuted) "Unmute" else "Mute",
//                                                tint = Color.White,
//                                                modifier = Modifier.size(18.dp)
//                                            )
//                                        }
//                                    }
//                                )
//                            },
//                            headlineContent = {
//                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
//                                    Row(verticalAlignment = Alignment.CenterVertically) {
//                                        Text(
//                                            text = sourceImage?.theme ?: "Unknown Theme",
//                                            style = MaterialTheme.typography.labelSmall,
//                                            color = MaterialTheme.colorScheme.primary
//                                        )
//
//                                        // 👉 Show the Check icon if this clip is the active export
//                                        if (video.export == true) {
//                                            Spacer(modifier = Modifier.width(4.dp))
//                                            Icon(
//                                                imageVector = Icons.Default.Check,
//                                                contentDescription = "Active Export",
//                                                tint = MaterialTheme.colorScheme.primary,
//                                                modifier = Modifier.size(14.dp)
//                                            )
//                                        }
//                                    }
//                                    Text(
//                                        text = video.model.toString(),
//                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
//                                        maxLines = 1,
//                                        overflow = TextOverflow.Ellipsis
//                                    )
//                                }
//                            },
//                            trailingContent = {
//                                IconButton(onClick = { isExpanded = !isExpanded }, modifier = Modifier.size(32.dp)) {
//                                    Icon(
//                                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
//                                        contentDescription = "Expand",
//                                        modifier = Modifier.size(20.dp),
//                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
//                                    )
//                                }
//                            },
//                            expandedContent = {
//                                // Strictly just the video prompt
//                                if (!video.prompt.isNullOrBlank()) {
//                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
//                                        Text("Video Prompt", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
//                                        Text(video.prompt, style = MaterialTheme.typography.bodySmall)
//                                    }
//                                } else {
//                                    Text(
//                                        text = "No prompt available.",
//                                        style = MaterialTheme.typography.bodySmall,
//                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
//                                    )
//                                }
//                            }
//                        )
//                    }
//                }
//            }
//        }

        HorizontalDivider(
            modifier = Modifier.padding(top = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun MediaSlotCard(
    modifier: Modifier = Modifier.width(180.dp).animateContentSize(),
    mediaContent: @Composable () -> Unit,
    headlineContent: @Composable () -> Unit,
    trailingContent: @Composable () -> Unit,
    isExpanded: Boolean, // State is hoisted to the parent so the chevron can toggle it
    expandedContent: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // 1. The Media Slot (Strict 160.dp height to match LeemCard)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                mediaContent()
            }

            // 2. The Headline & Trailing Slot (Strictly uses ListItem)
            ListItem(
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                headlineContent = headlineContent,
                trailingContent = trailingContent
            )

            // 3. The Expanded Content Slot
            if (isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    expandedContent()
                }
            }
        }
    }
}
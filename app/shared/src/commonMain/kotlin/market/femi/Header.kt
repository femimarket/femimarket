@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package market.femi

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoAwesomeMosaic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import femi.app.shared.generated.resources.Res
import femi.app.shared.generated.resources.*
import market.femi.models.allAnswered
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

class HeaderState(private val state: State) {
    var loader = ActiveShowState()

}

@Composable
fun Header(state: State){
//    val currentPositionSec by state.audio.positionSec.collectAsState()
//    val isPlaying by state.audio.isPlaying.collectAsState()
    println("is audio null in header ${state.data.audio?.current != null}")

    Column {
        TopAppBar(
            subtitle = {
                AnimatedContent(
                    targetState = if (state.data.audio?.current != null) null else state.nav.subtitle,
                    transitionSpec = { fadeIn(tween(600)) togetherWith fadeOut(tween(600)) },
                ) { subtitle ->
                    subtitle?.let { Text(stringResource(it)) }
                }
            },
            title = {
                AnimatedContent(
                    targetState = state.data.audio?.current != null,
                    transitionSpec = { fadeIn(tween(600)) togetherWith fadeOut(tween(600)) },
                ) { audio ->
                    if (audio) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(end = 8.dp)
                        ) {
                            Text(
                                text = state.audio.positionSec.toHoursMinuteSeconds(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Slider(
                                value = state.audio.positionSec.toFloat(),
                                valueRange = 0f..maxOf(state.audio.duration.toFloat(), 1f),
                                onValueChange = { state.audio.seek(it.toDouble()) },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    activeTrackColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    inactiveTrackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
                                )
                            )
                            Text(
                                text = state.audio.duration.toHoursMinuteSeconds(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    } else {
                        AnimatedContent(
                            targetState = state.nav.title,
                            transitionSpec = { fadeIn(tween(600)) togetherWith fadeOut(tween(600)) },
                        ) { title ->
                            Text(stringResource(title), maxLines = 1)
                        }
                    }
                }

            },
            navigationIcon = {
                if (state.nav.backStack.size > 1) {
                    IconButton(onClick = { state.nav.goBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.data.audio != null){
                            AsyncImage(
                                model = state.kv.withFsUrl(state.data.audio!!.current.image),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Image(
                                painter = painterResource(Res.drawable.logo),
                                contentDescription = "Logo",
                            )
//                        Icon(
//                            imageVector = Icons.Default.MusicNote,
//                            contentDescription = "No Audio",
//                            tint = MaterialTheme.colorScheme.onSurfaceVariant
//                        )
                        }

                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            actions = {
                IconButton(
                    onClick = {
                        state.data.audio?.let {
                            if (state.audio.isPlaying) state.audio.pause() else state.play(it.current.name)
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (state.audio.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.audio.isPlaying) "Pause" else "Play"
                    )
                }
//                IconButton(onClick = { showMatrixLibraryDialog = true }) {
//                    Icon(Icons.Default.Palette, contentDescription = "Style Library")
//                }
                Box {
                    IconButton(
                        onClick = { state.nav.openInfo() },
                        Modifier.focusProperties { canFocus = !state.showMenu.value },
                    ) {
//                        if (videoTrimChanges.isNotEmpty()) {
                        if (false) {
//                            BadgedBox(
//                                badge = { Badge { Text(videoTrimChanges.size.toString()) } }
//                            ) {
//                                Icon(Icons.Filled.MoreVert, contentDescription = "Menu with pending edits")
//                            }
                        } else {
                            Icon(Icons.Filled.AutoAwesomeMosaic, contentDescription = stringResource(Res.string.header_settings))
                        }
                    }
                    DropdownMenu(
                        expanded = state.showMenu.value,
                        onDismissRequest = { state.showMenu.hide() }
                    ) {
//                        if (videoTrimChanges.isNotEmpty()) {
//                            DropdownMenuItem(
//                                text = { Text("Sync ${activeEditProjectName?.uppercase()} (${videoTrimChanges.size})") },
//                                leadingIcon = { Icon(Icons.Default.Save, contentDescription = null) },
//                                onClick = {
//                                    menuOpen = false
//                                }
//                            )
//                            DropdownMenuItem(
//                                text = { Text("Discard Edits", color = MaterialTheme.colorScheme.error) },
//                                leadingIcon = { Icon(Icons.Default.Clear, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
//                                onClick = {
//                                    videoTrimChanges.clear()
//                                    menuOpen = false
//                                }
//                            )
//                            HorizontalDivider()
//                        }
//                        DropdownMenuItem(
//                            text = { Text("Update Library") },
//                            onClick = {
//                                scope.launch {
//                                    runCatching {
//                                        val files = window.pickDirectory()
////                                                    showNewAudioDialog = file
////                                                    saveDirHandle(db,dd)
////                                                    dir = dd
//                                    }.onSuccess {
//                                        snack.showSnackbar("Directory Connected!")
//                                    }.onFailure {
//                                        snack.showSnackbar(it.toString())
//                                    }
//                                    menuOpen = false
//                                }
//                            }
//                        )
//                        DropdownMenuItem(
//                            text = { Text("Change Project") },
//                            onClick = {
//                                state.activeData.dialogs.showAudioDialog.click()
//                            }
//                        )
//                        if (activeEdits.audio != null) {
//                            Text(
//                                text = "TRACK CONTENT",
//                                style = MaterialTheme.typography.labelSmall,
//                                fontWeight = FontWeight.Bold,
//                                color = MaterialTheme.colorScheme.primary,
//                                modifier = Modifier.padding(8.dp)
//                            )
//                            DropdownMenuItem(text = { Text("Edit Protagonist") }, onClick = { menuOpen = false; showProtagonistDialog = true })
//                            DropdownMenuItem(text = { Text("Edit Lyrics") }, onClick = { menuOpen = false; showLyricsDialog = true })
//                            DropdownMenuItem(text = { Text("Edit About (Project & Genre)") }, onClick = { menuOpen = false; showAboutDialog = true })
//                            HorizontalDivider()
//                            DropdownMenuItem(text = { Text("Edit ElevenLabs Config") }, onClick = { menuOpen = false; showElevenLabsDialog = true })
//                            DropdownMenuItem(text = { Text("Edit Qwen Config") }, onClick = { menuOpen = false; showQwen3AsrDialog = true })
//                            DropdownMenuItem(text = { Text("Edit LM Studio Config") }, onClick = { menuOpen = false; showLmStudioDialog = true })
//                            HorizontalDivider()
//                            DropdownMenuItem(text = { Text("Edit Word Alignment") }, onClick = { menuOpen = false; showWordAlignmentDialog = true })
//                            HorizontalDivider()
//                            DropdownMenuItem(text = { Text("Storyboard Generation") }, onClick = { menuOpen = false; showStoryboardDialog = true })
//                            HorizontalDivider()
//                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.menu_setup)) },
                            onClick = { state.setupDialog.click() }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.menu_import_directory)) },
                            enabled = !state.isWorking,
                            onClick = { state.importDialog.importDirectory() }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.menu_select_audio)) },
                            onClick = { state.audioDialog.click() },
                            enabled = state.audioDialog.enabled,
                        )
                        DropdownMenuItem(
                            enabled = state.data.audio !== null,
                            text = { Text(stringResource(Res.string.menu_about_faqs)) },
                            onClick = { state.faqsDialog.click() }
                        )
                        DropdownMenuItem(
                            enabled = state.data.audio?.current?.faqs?.allAnswered == true,
                            text = { Text(stringResource(Res.string.menu_about_storyboard)) },
                            onClick = { state.socialMediaBlueprintDialog.click() }
                        )
//                        DropdownMenuItem(
//                            text = { Text("Change Directory") },
//                            onClick = {
//                                scope.launch {
//                                    runCatching {
//                                        val dd = window.pickDirectory()
//                                        saveDirHandle(db,dd)
//                                        dir = dd
//                                    }.onSuccess {
//                                        snack.showSnackbar("Directory Connected!")
//                                    }.onFailure {
//                                        snack.showSnackbar(it.toString())
//                                    }
//                                    menuOpen = false
//                                }
//                            }
//                        )
//                        DropdownMenuItem(
//                            text = { Text("Export db") },
//                            onClick = {
//                                scope.launch {
//                                    exportDatabase(db)
//                                    menuOpen = false
//                                }
//                            }
//                        )
//                        DropdownMenuItem(
//                            text = { Text("Import db") },
//                            onClick = {
//                                scope.launch {
////                                                importDatabase(db,viewport,filters)
//                                    menuOpen = false
//                                }
//                            }
//                        )
//                        DropdownMenuItem(
//                            text = { Text("export") },
//                            onClick = {
//                                scope.launch {
//                                    showExportDialog = true
//                                    menuOpen = false
//                                }
//                            }
//                        )
//                        DropdownMenuItem(
//                            text = { Text("Migrate OPFS to Current Dir") },
//                            onClick = {
//                                menuOpen = false
//                                val currentDir = dir // Ensure we capture the non-null state
//                                if (currentDir != null) {
//                                    scope.launch {
//                                        isWorking = true
//                                        snack.showSnackbar("Migrating files to network drive...")
//
//                                        runCatching {
//                                            // Set deleteAfter = true if you want it to empty OPFS as it goes
//                                            migrateOpfsToTarget(targetDir = currentDir, deleteAfter = false)
//                                        }.onSuccess {
//                                            snack.showSnackbar("Migration Complete!")
//                                        }.onFailure {
//                                            snack.showSnackbar("Migration Failed: ${it.message}")
//                                        }
//                                        isWorking = false
//                                    }
//                                } else {
//                                    scope.launch { snack.showSnackbar("No directory selected!") }
//                                }
//                            }
//                        )
                    }
                }
            }
        )
        AnimatedVisibility(visible = state.isWorking) {
//            if (activeJobs.progress > 0f) {
//                // Show exact percentage (Muxing / Downloading)
//                LinearProgressIndicator(
//                    progress = { activeJobs.progress },
//                    modifier = Modifier.fillMaxWidth().height(2.dp),
//                    color = MaterialTheme.colorScheme.primary,
//                    trackColor = Color.Transparent
//                )
//            } else {
                // Show sweeping animation while waiting on the API
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )
//            }
        }
    }
}

@Preview
@Composable
fun HeaderPreview(){
    Header(fakeState())
}
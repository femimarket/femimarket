package market.femi.music

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AssistChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import coil3.compose.AsyncImage
import femi.app.shared.generated.resources.Res
import femi.app.shared.generated.resources.menu_import_directory
import femi.app.shared.generated.resources.music_select_import
import femi.app.shared.generated.resources.music_select_import_title
import femi.app.shared.generated.resources.music_select_results
import femi.app.shared.generated.resources.music_select_results_title
import femi.app.shared.generated.resources.music_select_search
import femi.app.shared.generated.resources.music_select_add_song
import femi.app.shared.generated.resources.music_select_servers
import femi.app.shared.generated.resources.music_select_title
import femi.app.shared.generated.resources.music_settings_import
import femi.app.shared.generated.resources.music_settings_song
import market.femi.State
import market.femi.dialogs.HighlightText
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs

private val GRADIENTS = listOf(
    Color(0xFFE57373) to Color(0xFFF06292), Color(0xFFBA68C8) to Color(0xFF9575CD),
    Color(0xFF7986CB) to Color(0xFF64B5F6), Color(0xFF4FC3F7) to Color(0xFF4DD0E1),
    Color(0xFF4DB6AC) to Color(0xFF81C784), Color(0xFFAED581) to Color(0xFFFF8A65),
    Color(0xFFFFB74D) to Color(0xFFA1887F),
)

// Artwork-first browse grouped by project (mirrors TabSection, specific to Audio). The selected
// project lives in state (state.audioDialog.project); "See All" re-searches scoped to it, back
// clears it. Each project = a full-span header + up to 12 preview cards; a card selects the audio.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MusicSelect(state: State) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = stringResource(Res.string.music_select_import_title),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND.dp).fillMaxWidth(),
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = stringResource(Res.string.music_select_import),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND.dp).fillMaxWidth(),
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Image(
                        painter = painterResource(Res.drawable.music_settings_import),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentWidth(Alignment.CenterHorizontally)
                            .widthIn(max = WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND.dp)
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(MaterialTheme.shapes.extraLarge),
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND.dp).fillMaxWidth().padding(bottom = 8.dp),
                    ) {
                        AssistChip(
                            onClick = { state.nav.openMusicServer() },
                            label = { Text(stringResource(Res.string.music_select_servers)) },
                        )
                        AssistChip(
                            onClick = { state.importDialog.importDirectory() },
                            enabled = !state.isWorking,
                            label = { Text(stringResource(Res.string.menu_import_directory)) },
                        )
                        AssistChip(
                            onClick = { state.importDialog.addSong() },
                            enabled = !state.isWorking,
                            label = { Text(stringResource(Res.string.music_select_add_song)) },
                        )
                    }
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = stringResource(Res.string.music_select_title),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND.dp).fillMaxWidth(),
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = stringResource(Res.string.music_select_search),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND.dp).fillMaxWidth(),
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Image(
                        painter = painterResource(Res.drawable.music_settings_song),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentWidth(Alignment.CenterHorizontally)
                            .widthIn(max = WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND.dp)
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(MaterialTheme.shapes.extraLarge),
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    OutlinedTextField(
                        value = state.audioDialog.lyrics.value,
                        onValueChange = { state.audioDialog.searchLyrics(it) },   // group defaults "" → clears project filter
                        modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND.dp).fillMaxWidth(),
                        placeholder = { Text("Search lyrics or keywords...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Lyrics") },
                        trailingIcon = {
                            if (state.audioDialog.lyrics.value.isNotEmpty()) {
                                IconButton(onClick = { state.audioDialog.searchLyrics() }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = MaterialTheme.shapes.extraLarge,
                    )
                }

                if (state.data.audios.list.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = stringResource(Res.string.music_select_results_title),
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND.dp).fillMaxWidth().padding(top = 8.dp),
                        )
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = stringResource(Res.string.music_select_results),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND.dp).fillMaxWidth(),
                        )
                    }
                }

                    state.data.audios.list.groupBy { it.project.takeIf { p -> p.isNotBlank() } ?: "Default" }
                        .forEach { (project, projectAudios) ->

                            // Full-span project header — "See All" re-searches scoped to it; back-arrow clears.
                            item(span = { GridItemSpan(maxLineSpan) }, key = "hdr-$project") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            state.audioDialog.searchLyrics(
                                                state.audioDialog.lyrics.value,
                                                if (state.audioDialog.project.value.isNotBlank()) "" else project,
                                            )
                                        }
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (state.audioDialog.project.value.isNotBlank()) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Back to projects",
                                                modifier = Modifier.padding(end = 4.dp).size(20.dp),
                                            )
                                        }
                                        HighlightText(
                                            text = project.uppercase(),
                                            query = state.audioDialog.lyrics.value,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                        )
                                    }
                                    if (state.audioDialog.project.value.isBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("See All (${projectAudios.size})")
                                            Spacer(Modifier.width(4.dp))
                                            Icon(
                                                Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = "View project",
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }
                                }
                            }

                            // The cards — the ONE place the card is written. Preview 12 per project until drilled in.
                            items(
                                if (state.audioDialog.project.value.isNotBlank()) projectAudios else projectAudios.take(12),
                                key = { it.id },
                            ) { audio ->
                                val brush = remember(audio.id) {
                                    val (a, b) = GRADIENTS[abs(audio.id.hashCode()) % GRADIENTS.size]
                                    Brush.linearGradient(listOf(a, b))
                                }
                                // Up to 3 lyric lines containing any search word; null when there's no query/match.
                                val snippet = remember(audio.lyrics, state.audioDialog.lyrics.value) {
                                    val q = state.audioDialog.lyrics.value.trim()
                                    val lyrics = audio.lyrics
                                    if (q.isBlank() || lyrics.isNullOrBlank() || !lyrics.contains(q, ignoreCase = true)) null
                                    else lyrics.split("\n")
                                        .filter { line -> q.split("\\s+".toRegex()).any { line.contains(it, ignoreCase = true) } }
                                        .take(3).joinToString("\n")
                                }

                                Column(Modifier.fillMaxWidth()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .clip(MaterialTheme.shapes.medium)
                                            .background(brush),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (!audio.image.isNullOrBlank()) {
                                            AsyncImage(
                                                model = state.kv.withFsUrl(audio.image),
                                                contentDescription = audio.project,
                                                modifier = Modifier.fillMaxSize(),
                                            )
                                        } else {
                                            Text(
                                                text = audio.genre?.uppercase() ?: audio.project.uppercase(),
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                maxLines = 3,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(8.dp),
                                            )
                                        }

                                        if (audio.like == true) {
                                            Icon(
                                                imageVector = Icons.Default.Favorite,
                                                contentDescription = "Favorite",
                                                tint = Color(0xFFE91E63),
                                                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp).size(20.dp),
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                if (!state.audio.isPlaying) state.audioDialog.playAudio(audio)
                                                    else state.audioDialog.pauseAudio()
                                            },
                                            modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape),
                                        ) {
                                            Icon(
                                                if (state.audio.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = if (state.audio.isPlaying) "Pause" else "Play",
                                                tint = Color.White,
                                            )
                                        }
                                    }

                                    if (snippet != null) {
                                        HighlightText(
                                            text = snippet,
                                            query = state.audioDialog.lyrics.value,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(top = 4.dp).fillMaxWidth(),
                                        )
                                    }
                                }
                            }
                        }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Button(
                            onClick = { state.audioDialog.confirm() },
                            enabled = state.audioDialog.audio.value != null,
                        ) { Text("Confirm") }
                    }
                }
    }
}

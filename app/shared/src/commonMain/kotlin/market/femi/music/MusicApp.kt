package market.femi.music

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import market.femi.State
import market.femi.toFemi
import market.femi.toHoursMinuteSeconds


@Composable
fun MusicApp(
    state: State,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // ==========================================
        // LYRICS SIDEBAR
        // ==========================================
        Text(
            text = "Lyric Lines",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            itemsIndexed(
                items = state.data.audio?.lines.orEmpty(),
            ) { index, lineItem ->

                val lineVideos = state.data.videos.linstVideos[lineItem.id.toString()] ?: emptyList()
                val hasExport = lineVideos.any { it.export }
                val hasVideo = lineVideos.isNotEmpty()
                val hasImage = lineItem.themes.any { theme ->
                    theme.theme.isNotBlank() && state.data.images.leemImageMap.containsKey("${lineItem.text}|${theme.theme}")
                }

                ListItem(
                    modifier = Modifier.clickable { state.musicLines.set(index); state.nav.openMusicLine(index) },
                    headlineContent = {
                        Text(
                            text = lineItem.text,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    supportingContent = {
                        Text(lineItem.startMs.toHoursMinuteSeconds())
                    },
                    trailingContent = {
                        // 👉 The combined Cost + Stage Icon Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = state.data.lineCostMap[lineItem]!!.totalCost.toFemi(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            when {
                                hasExport -> Icon(Icons.Default.CheckCircle, "Exported", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                hasVideo -> Icon(Icons.Default.Movie, "Has Videos", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                                hasImage -> Icon(Icons.Default.Image, "Has Images", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                )

                LinearProgressIndicator(
                    progress = { state.data.lineCostMap[lineItem]!!.completion / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round,
                    drawStopIndicator = {}
                )
            }
        }
    }
}
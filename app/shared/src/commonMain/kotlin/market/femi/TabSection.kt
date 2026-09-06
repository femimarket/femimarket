package market.femi

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlin.collections.component1
import kotlin.collections.component2


@Composable
fun <T> TabSection(
    items: List<T>,
    workspaceItem: T? = null, // 👉 NEW: The currently selected item to edit
    selectedProject: String?,
    onProjectSelect: (String?) -> Unit,
    getProject: (T) -> String,
    getId: (T) -> String,
    searchQuery: String = "",
    topBar: @Composable () -> Unit,
    rowItemCard: @Composable (T) -> Unit,   // 👉 New slot for horizontal rows
    gridItemCard: @Composable (T) -> Unit,   // 👉 New slot for vertical grid
    workspace: @Composable ((T) -> Unit)? = null // 👉 NEW: The Workspace Composable slot
) {
    if (workspaceItem != null && workspace != null) {
        workspace(workspaceItem)
        return // Short-circuit so the list doesn't render underneath
    }

    val itemsByProject by remember(items) {
        derivedStateOf {
            items.groupBy { getProject(it).takeIf { p -> p.isNotBlank() } ?: "Default" }
        }
    }

    // 👉 1. INITIALIZE THE SCROLL STATES
    val listState = rememberLazyListState()
    LaunchedEffect(searchQuery) {
        if (selectedProject == null && items.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        topBar()

        if (selectedProject == null) {
            LazyColumn(
                state = listState, // 👉 ATTACHED STATE HERE
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                itemsByProject.forEach { (project, projectItems) ->
                    item(key = project) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                HighlightText(
                                    text = project.uppercase(),
                                    query = searchQuery,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )

                                // The isolated, bounded click target
                                TextButton(onClick = { onProjectSelect(project) }) {
                                    Text("See All (${projectItems.size})")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "View Project Grid",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(items = projectItems.take(10), key = { getId(it) }) { item ->
                                    Box(modifier = Modifier.clickable { onProjectSelect(project) }) {
                                        rowItemCard(item) // 👉 Call Row Card
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            val currentItems = itemsByProject[selectedProject] ?: emptyList()

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 16.dp, bottom = 16.dp).fillMaxWidth()
            ) {
                IconButton(onClick = { onProjectSelect(null) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Projects")
                }
                HighlightText(
                    text = selectedProject.uppercase(),
                    query = searchQuery,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 186.dp),
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = currentItems, key = { getId(it) }) { item ->
                    gridItemCard(item) // 👉 Call Grid Card
                }
            }
        }
    }
}

@Composable
fun HighlightText(
    text: String,
    query: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    highlightColor: Color = MaterialTheme.colorScheme.onTertiaryContainer,
    highlightBackground: Color = MaterialTheme.colorScheme.tertiaryContainer,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val annotatedString = remember(text, query, highlightColor, highlightBackground) {
        val queryStr = query.trim()
        val searchWords = if (queryStr.isNotEmpty()) {
            queryStr.split("\\s+".toRegex()).filter { it.isNotBlank() }
        } else emptyList()

        if (searchWords.isEmpty() || text.isBlank()) {
            AnnotatedString(text)
        } else {
            val matchRanges = mutableListOf<IntRange>()
            for (word in searchWords) {
                var startIndex = 0
                while (true) {
                    val idx = text.indexOf(word, startIndex, ignoreCase = true)
                    if (idx == -1) break
                    matchRanges.add(idx until (idx + word.length))
                    startIndex = idx + word.length
                }
            }

            if (matchRanges.isEmpty()) {
                AnnotatedString(text)
            } else {
                matchRanges.sortBy { it.first }
                val mergedRanges = mutableListOf<IntRange>()
                for (range in matchRanges) {
                    if (mergedRanges.isEmpty()) {
                        mergedRanges.add(range)
                    } else {
                        val last = mergedRanges.last()
                        if (range.first <= last.last + 1) {
                            mergedRanges[mergedRanges.size - 1] = last.first..maxOf(last.last, range.last)
                        } else {
                            mergedRanges.add(range)
                        }
                    }
                }

                buildAnnotatedString {
                    var currentIndex = 0
                    for (range in mergedRanges) {
                        if (range.first > currentIndex) {
                            append(text.substring(currentIndex, range.first))
                        }
                        withStyle(
                            style = SpanStyle(
                                background = highlightBackground,
                                color = highlightColor,
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append(text.substring(range.first, range.last + 1))
                        }
                        currentIndex = range.last + 1
                    }
                    if (currentIndex < text.length) {
                        append(text.substring(currentIndex))
                    }
                }
            }
        }
    }

    Text(
        text = annotatedString,
        modifier = modifier,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = overflow
    )
}

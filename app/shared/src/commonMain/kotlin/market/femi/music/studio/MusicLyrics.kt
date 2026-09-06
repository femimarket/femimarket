package market.femi.music.studio

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import market.femi.State
import market.femi.deprecated.TextField


@Composable
fun MusicLyrics(
    state: State
) {
    if (!state.lyricsDialog.value) return
    AlertDialog(
        onDismissRequest = { state.lyricsDialog.hide() },
        title = { Text("Edit Lyrics") },
        text = {
            TextField(
                value = state.lyricsDialog.lyrics.value,
                onValueChange = { state.lyricsDialog.lyrics.type(it) },
                label = "Lyrics"
            )
        },
        confirmButton = { TextButton(onClick = {state.lyricsDialog.hide()}) { Text("Close") } }
    )
}
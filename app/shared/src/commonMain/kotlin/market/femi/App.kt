package market.femi

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.materialkolor.rememberDynamicColorScheme
import market.femi.music.studio.MusicFaqs
import market.femi.music.studio.MusicBlueprint
import market.femi.music.studio.MusicLyrics

@Composable
fun App(
    state: State= remember { State() }
) {
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos {}
            state.audio.sync()
        }
    }
    val animated by animateColorAsState(state.theme.seedColor, tween(600))   // crossfade
    val scheme = rememberDynamicColorScheme(                            // seed → full M3 scheme
        seedColor = animated,
        isDark = isSystemInDarkTheme(),
    )
    MaterialTheme(colorScheme = scheme) {
        Router(state)
//        Scaffold(
//            topBar = { Header(state) },
//            bottomBar = { Footer(state) },
//        ) { paddingValues ->
//            Box(Modifier.fillMaxSize().padding(paddingValues)) {
//                Router(state)
//            }
////            Row(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
////                LyricsSidebar(state)
////                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
////
////            }
//        }
    }
}

@Preview
@Composable
fun PreviewApp(){
    App(fakeState())
}
package market.femi.music

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import femi.app.shared.generated.resources.Res
import femi.app.shared.generated.resources.menu_about_faqs
import femi.app.shared.generated.resources.menu_lines
import femi.app.shared.generated.resources.menu_lyrics
import femi.app.shared.generated.resources.music_info_lyrics
import femi.app.shared.generated.resources.music_info_scenes
import femi.app.shared.generated.resources.menu_about_storyboard
import femi.app.shared.generated.resources.music_settings_blueprint
import femi.app.shared.generated.resources.music_settings_faq
import market.femi.State
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicStudio(state: State) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // about two cards visible plus a peek
        HorizontalMultiBrowseCarousel(
            state = rememberCarouselState { 4 },
            modifier = Modifier.fillMaxSize().padding(16.dp),
            preferredItemWidth = maxWidth / 2,
            itemSpacing = 8.dp,
        ) { index ->
            when (index) {
                0 -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .maskClip(MaterialTheme.shapes.extraLarge)
                        .clickable { state.lyricsDialog.click() },
                ) {
                    Image(
                        painter = painterResource(Res.drawable.music_info_lyrics),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.verticalGradient(listOf(Color(0xCC000000), Color(0x80000000), Color(0xE6000000))),
                        ),
                    )
                    Text(
                        text = stringResource(Res.string.menu_lyrics),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center),
                    )
                }
                3 -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .maskClip(MaterialTheme.shapes.extraLarge)
                        .clickable { state.nav.openMusicLyrics() },
                ) {
                    Image(
                        painter = painterResource(Res.drawable.music_info_scenes),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.verticalGradient(listOf(Color(0xCC000000), Color(0x80000000), Color(0xE6000000))),
                        ),
                    )
                    Text(
                        text = stringResource(Res.string.menu_lines),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center),
                    )
                }
                1 -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .maskClip(MaterialTheme.shapes.extraLarge)
                        .clickable { state.faqsDialog.click() },
                ) {
                    Image(
                        painter = painterResource(Res.drawable.music_settings_faq),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.verticalGradient(listOf(Color(0xCC000000), Color(0x80000000), Color(0xE6000000))),
                        ),
                    )
                    Text(
                        text = stringResource(Res.string.menu_about_faqs),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center),
                    )
                }
                2 -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .maskClip(MaterialTheme.shapes.extraLarge)
                        .clickable { state.socialMediaBlueprintDialog.click() },
                ) {
                    Image(
                        painter = painterResource(Res.drawable.music_settings_blueprint),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.verticalGradient(listOf(Color(0xCC000000), Color(0x80000000), Color(0xE6000000))),
                        ),
                    )
                    Text(
                        text = stringResource(Res.string.menu_about_storyboard),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center),
                    )
                }
            }
        }
    }
}

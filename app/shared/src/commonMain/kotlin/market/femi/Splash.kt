package market.femi

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import femi.app.shared.generated.resources.Res
import femi.app.shared.generated.resources.music_videos
import femi.app.shared.generated.resources.splash_care
import femi.app.shared.generated.resources.splash_match
import femi.app.shared.generated.resources.splash_money
import femi.app.shared.generated.resources.splash_tech
import io.github.kdroidfilter.composemediaplayer.InitialPlayerState
import kotlin.math.roundToInt
import io.github.kdroidfilter.composemediaplayer.VideoPlayerSurface
import io.github.kdroidfilter.composemediaplayer.rememberVideoPlayerState
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.seconds

// The super-app front door: a Material 3 carousel of shell apps — hero-sized cards,
// the next app peeking, cards morphing through their masks. Tapping a card enters the
// app by pushing its route. Each card carries a live video background (muted,
// looping, scrimmed — the web Home layering).

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Splash(state: State) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // hero sizing: the focused item takes the width minus one visible peek
        val heroWidth = maxWidth - 72.dp
        HorizontalMultiBrowseCarousel(
            state = rememberCarouselState { 4 },
            modifier = Modifier.fillMaxSize().padding(16.dp),
            preferredItemWidth = heroWidth,
            itemSpacing = 8.dp,
        ) { index ->
            when (index) {
                0 -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .maskClip(MaterialTheme.shapes.extraLarge),
                ) {
                    val player = rememberVideoPlayerState()

                    LaunchedEffect(Unit) {
                        player.loop = true
                        player.volume = 0f
                        player.openUri(state.kv.withFsUrl("splash.mp4"), initializeplayerState = InitialPlayerState.PAUSE)
                        delay(2.seconds)
                        player.play()
                    }
                    // the surface can't be canvas-clipped by the carousel mask, so lay it out
                    // to the mask instead: maskRect is the item's visible rectangle, animating
                    // with the morph — the surface always occupies exactly its own card.
                    // The scrim, title, and click target ride in the surface's overlay slot —
                    // on iOS the video is a native UIKit view that sits above sibling
                    // composables and swallows their taps; the overlay is the library's
                    // sanctioned layer above the video on every platform.
                    VideoPlayerSurface(
                        playerState = player,
                        modifier = Modifier.layout { measurable, constraints ->
                            val rect = carouselItemDrawInfo.maskRect
                            val placeable = measurable.measure(
                                Constraints.fixed(rect.width.roundToInt(), rect.height.roundToInt()),
                            )
                            layout(constraints.maxWidth, constraints.maxHeight) {
                                placeable.place(rect.left.roundToInt(), rect.top.roundToInt())
                            }
                        },
                        contentScale = ContentScale.Crop,
                        overlay = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(listOf(Color(0xCC000000), Color(0x80000000), Color(0xE6000000))),
                                    )
                                    .clickable { state.nav.openMusicApp() },
                            ) {
                                Text(
                                    text = stringResource(Res.string.music_videos),
                                    style = MaterialTheme.typography.displayMedium,
                                    color = Color.White,
                                    // the overlay is maskRect-sized: on the narrow peek strip the
                                    // title must clip like the video does, never re-wrap vertically
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center),
                                )
                            }
                        },
                    )
                }
                1 -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .maskClip(MaterialTheme.shapes.extraLarge),
                ) {
                    val player = rememberVideoPlayerState()
                    LaunchedEffect(Unit) {
                        player.loop = true
                        player.volume = 0f
                        player.openUri(state.kv.withFsUrl("care.mp4"),initializeplayerState = InitialPlayerState.PAUSE)
                        delay(2.seconds)
                        player.play()
                    }
                    VideoPlayerSurface(
                        playerState = player,
                        modifier = Modifier.layout { measurable, constraints ->
                            val rect = carouselItemDrawInfo.maskRect
                            val placeable = measurable.measure(
                                Constraints.fixed(rect.width.roundToInt(), rect.height.roundToInt()),
                            )
                            layout(constraints.maxWidth, constraints.maxHeight) {
                                placeable.place(rect.left.roundToInt(), rect.top.roundToInt())
                            }
                        },
                        contentScale = ContentScale.Crop,
                        overlay = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(listOf(Color(0xCC000000), Color(0x80000000), Color(0xE6000000))),
                                    )
                                    .clickable { state.nav.openCare() },
                            ) {
                                Text(
                                    text = stringResource(Res.string.splash_care),
                                    style = MaterialTheme.typography.displayMedium,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center),
                                )
                            }
                        },
                    )
                }
                2 -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .maskClip(MaterialTheme.shapes.extraLarge),
                ) {
                    val player = rememberVideoPlayerState()
                    LaunchedEffect(Unit) {
                        player.loop = true
                        player.volume = 0f
                        player.openUri(state.kv.withFsUrl("tech.mp4"), initializeplayerState = InitialPlayerState.PAUSE)
                        delay(2.seconds)
                        player.play()
                    }
                    VideoPlayerSurface(
                        playerState = player,
                        modifier = Modifier.layout { measurable, constraints ->
                            val rect = carouselItemDrawInfo.maskRect
                            val placeable = measurable.measure(
                                Constraints.fixed(rect.width.roundToInt(), rect.height.roundToInt()),
                            )
                            layout(constraints.maxWidth, constraints.maxHeight) {
                                placeable.place(rect.left.roundToInt(), rect.top.roundToInt())
                            }
                        },
                        contentScale = ContentScale.Crop,
                        overlay = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(listOf(Color(0xCC000000), Color(0x80000000), Color(0xE6000000))),
                                    )
                                    .clickable { state.nav.openTechApp() },
                            ) {
                                Text(
                                    text = stringResource(Res.string.splash_tech),
                                    style = MaterialTheme.typography.displayMedium,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center),
                                )
                            }
                        },
                    )
                }
                3 -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .maskClip(MaterialTheme.shapes.extraLarge),
                ) {
                    val player = rememberVideoPlayerState()
                    LaunchedEffect(Unit) {
                        player.loop = true
                        player.volume = 0f
                        player.openUri(state.kv.withFsUrl("money.mp4"), initializeplayerState = InitialPlayerState.PAUSE)
                        delay(2.seconds)
                        player.play()
                    }
                    VideoPlayerSurface(
                        playerState = player,
                        modifier = Modifier.layout { measurable, constraints ->
                            val rect = carouselItemDrawInfo.maskRect
                            val placeable = measurable.measure(
                                Constraints.fixed(rect.width.roundToInt(), rect.height.roundToInt()),
                            )
                            layout(constraints.maxWidth, constraints.maxHeight) {
                                placeable.place(rect.left.roundToInt(), rect.top.roundToInt())
                            }
                        },
                        contentScale = ContentScale.Crop,
                        overlay = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(listOf(Color(0xCC000000), Color(0x80000000), Color(0xE6000000))),
                                    )
                                    .clickable { state.nav.openMoneyHome() },
                            ) {
                                Text(
                                    text = stringResource(Res.string.splash_money),
                                    style = MaterialTheme.typography.displayMedium,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center),
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

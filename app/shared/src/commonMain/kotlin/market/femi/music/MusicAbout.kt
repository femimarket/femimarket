package market.femi.music

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import femi.app.shared.generated.resources.Res
import femi.app.shared.generated.resources.music_info_belief
import femi.app.shared.generated.resources.music_info_belief_title
import femi.app.shared.generated.resources.music_info_enter
import femi.app.shared.generated.resources.music_info_goal
import femi.app.shared.generated.resources.music_info_goal_title
import femi.app.shared.generated.resources.music_info_service
import femi.app.shared.generated.resources.music_info_service_title
import femi.app.shared.generated.resources.music_info_what
import femi.app.shared.generated.resources.music_info_what_title
import femi.app.shared.generated.resources.music_info_why
import femi.app.shared.generated.resources.music_info_why_title
import io.github.kdroidfilter.composemediaplayer.VideoPlayerSurface
import io.github.kdroidfilter.composemediaplayer.rememberVideoPlayerState
import market.femi.State
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
fun MusicAbout(state: State) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val viewportHeight = maxHeight
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(Res.string.music_info_what_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(Res.string.music_info_what),
                style = MaterialTheme.typography.bodyLarge,
            )
            val player = rememberVideoPlayerState()
            LaunchedEffect(Unit) {
                player.loop = true
                player.volume = 0f
                player.openUri(state.kv.withFsUrl("music-info-what.mp4"))
            }
            // the video is a native view on iOS and can't be trusted to clip an
            // overscanned surface, so it fills its window statically — only the
            // images parallax
            VideoPlayerSurface(
                playerState = player,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(MaterialTheme.shapes.extraLarge),
                contentScale = ContentScale.Crop,
            )

            Text(
                text = stringResource(Res.string.music_info_why_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(Res.string.music_info_why),
                style = MaterialTheme.typography.bodyLarge,
            )
            val whyY = remember { mutableStateOf(0f) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .onGloballyPositioned { whyY.value = it.positionInParent().y },
            ) {
                Image(
                    painter = painterResource(Res.drawable.music_info_why),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .offset {
                            // 80dp taller than its window, sliding through it at the
                            // pace of the window's trip across the viewport: bottom
                            // slice showing as it enters, top slice as it leaves —
                            // travel is bounded so no empty edge ever shows
                            val progress = ((scrollState.value + viewportHeight.toPx() - whyY.value) /
                                (viewportHeight.toPx() + 200.dp.toPx())).coerceIn(0f, 1f)
                            IntOffset(0, ((progress - 1f) * 80.dp.toPx()).roundToInt())
                        },
                )
            }

            Text(
                text = stringResource(Res.string.music_info_belief_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(Res.string.music_info_belief),
                style = MaterialTheme.typography.bodyLarge,
            )
            val beliefY = remember { mutableStateOf(0f) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .onGloballyPositioned { beliefY.value = it.positionInParent().y },
            ) {
                Image(
                    painter = painterResource(Res.drawable.music_info_belief),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .offset {
                            val progress = ((scrollState.value + viewportHeight.toPx() - beliefY.value) /
                                (viewportHeight.toPx() + 200.dp.toPx())).coerceIn(0f, 1f)
                            IntOffset(0, ((progress - 1f) * 80.dp.toPx()).roundToInt())
                        },
                )
            }

            Text(
                text = stringResource(Res.string.music_info_goal_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(Res.string.music_info_goal),
                style = MaterialTheme.typography.bodyLarge,
            )
            val goalY = remember { mutableStateOf(0f) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .onGloballyPositioned { goalY.value = it.positionInParent().y },
            ) {
                Image(
                    painter = painterResource(Res.drawable.music_info_goal),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .offset {
                            val progress = ((scrollState.value + viewportHeight.toPx() - goalY.value) /
                                (viewportHeight.toPx() + 200.dp.toPx())).coerceIn(0f, 1f)
                            IntOffset(0, ((progress - 1f) * 80.dp.toPx()).roundToInt())
                        },
                )
            }

            Text(
                text = stringResource(Res.string.music_info_service_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(Res.string.music_info_service),
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(
                onClick = { state.music.about.openMusicApp() },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(stringResource(Res.string.music_info_enter))
            }
        }
    }
}

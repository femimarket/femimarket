//@file:OptIn(ExperimentalWasmJsInterop::class, ExperimentalComposeUiApi::class)
//
package market.femi
//
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.BoxScope
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.size
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.BrokenImage
//import androidx.compose.material.icons.filled.Image
//import androidx.compose.material3.CircularProgressIndicator
//import androidx.compose.material3.Icon
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.DisposableEffect
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.rememberCoroutineScope
//import androidx.compose.runtime.rememberUpdatedState
//import androidx.compose.runtime.setValue
//import androidx.compose.runtime.withFrameNanos
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.ExperimentalComposeUiApi
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.drawBehind
//import androidx.compose.ui.graphics.BlendMode
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.viewinterop.WebElementView
//import kotlinx.browser.document
//import kotlinx.coroutines.isActive
//import kotlinx.coroutines.launch
//import org.w3c.dom.CanvasRenderingContext2D
//import org.w3c.dom.HTMLCanvasElement
//import org.w3c.dom.HTMLElement
//import org.w3c.dom.HTMLImageElement
//import org.w3c.dom.HTMLVideoElement
//import org.w3c.dom.ImageBitmap
//import org.w3c.dom.events.Event
//import kotlin.js.ExperimentalWasmJsInterop
//import kotlin.js.JsArray
//import kotlin.js.get
//import kotlin.js.length
//import kotlin.js.toJsString
//
//sealed interface MediaType {
//    data object Image : MediaType
//    data class Video(
//        val state: NativeVideoState? = null,
//        val showControls: Boolean = false,
//        val autoplay: Boolean = true
//    ) : MediaType
//    data class BitmapVideo(
//        val bitmaps: JsArray<ImageBitmap>?,
//        val currentMs: Long,
//        val durationMs: Long
//    ) : MediaType
//    data class OwnedCanvas(
//        val canvas: HTMLCanvasElement,
//    ) : MediaType
//}
//
//@Composable
//fun NativeMedia(
//    filename: String?,
//    modifier: Modifier = Modifier,
//    type: MediaType = MediaType.Image,
//    topRight: @Composable BoxScope.() -> Unit = {},
//    bottomRight: @Composable BoxScope.() -> Unit = {},
//    bottomLeft: @Composable BoxScope.() -> Unit = {},
//    topLeft: @Composable BoxScope.() -> Unit = {}
//) {
//    var objectUrl by remember { mutableStateOf<String?>(null) }
//    var isError by remember { mutableStateOf(false) }
//    val scope = rememberCoroutineScope()
//
//    // 1. Create the Element (Exhaustive)
//    val htmlElement = remember(type::class) {
//        when (type) {
//            is MediaType.OwnedCanvas -> type.canvas
//            is MediaType.BitmapVideo -> document.createElement("canvas") as HTMLCanvasElement
//            is MediaType.Video -> document.createElement("video") as HTMLVideoElement
//            is MediaType.Image -> document.createElement("img") as HTMLImageElement
//        }
//    }
//
//    // 2. Load the Data (Exhaustive)
//    when (type) {
//        is MediaType.BitmapVideo, is MediaType.OwnedCanvas -> {
//            // Wasm memory is passed directly; no OPFS loading required.
//        }
//        is MediaType.Image, is MediaType.Video -> {
//            DisposableEffect(filename) {
//                isError = false
//                objectUrl = null
//                var resolvedUrl: String? = null
//
//                val job = scope.launch {
//                    if (!filename.isNullOrBlank()) {
//                        runCatching {
//                            resolvedUrl = readBlob(filename)
//                            objectUrl = resolvedUrl
//                        }.onFailure { isError = true }
//                    }
//                }
//
//                onDispose {
//                    job.cancel()
//                    resolvedUrl?.let { WebURL.revokeObjectURL(it.toJsString()) }
//                }
//            }
//        }
//    }
//
//    // 3. Attach the Engine (Exhaustive)
//    when (type) {
//        is MediaType.BitmapVideo -> {
//            val canvasElement = htmlElement as HTMLCanvasElement
//
//            val liveBitmaps by rememberUpdatedState(type.bitmaps)
//            val liveCurrentMs by rememberUpdatedState(type.currentMs)
//            val liveDurationMs by rememberUpdatedState(type.durationMs)
//
//            LaunchedEffect(Unit) {
//                val ctx = canvasElement.getContext("2d") as CanvasRenderingContext2D
//                var lastDrawnIndex = -1
//                var lastDrawnBitmapsRef: JsArray<ImageBitmap>? = null
//
//                while (isActive) {
//                    withFrameNanos { }
//
//                    val currentBitmaps = liveBitmaps
//                    val currentMs = liveCurrentMs
//                    val durationMs = liveDurationMs
//
//                    if (currentBitmaps != null && currentBitmaps.length > 0 && durationMs > 0) {
//                        val firstFrame = currentBitmaps[0]
//                        if (firstFrame != null && canvasElement.width != firstFrame.width) {
//                            canvasElement.width = firstFrame.width
//                            canvasElement.height = firstFrame.height
//                        }
//
//                        val msPerFrame = durationMs.toDouble() / currentBitmaps.length
//                        var frameIndex = (currentMs / msPerFrame).toInt()
//
//                        if (frameIndex < 0) frameIndex = 0
//                        if (frameIndex >= currentBitmaps.length) frameIndex = currentBitmaps.length - 1
//
//                        if (frameIndex != lastDrawnIndex || currentBitmaps !== lastDrawnBitmapsRef) {
//                            val bmp = currentBitmaps[frameIndex]
//                            if (bmp != null) {
//                                drawBitmapToCanvas(ctx, bmp, canvasElement.width.toDouble(), canvasElement.height.toDouble())
//                                lastDrawnIndex = frameIndex
//                                lastDrawnBitmapsRef = currentBitmaps
//                            }
//                        }
//                    } else {
//                        if (lastDrawnBitmapsRef != null) {
//                            ctx.clearRect(0.0, 0.0, canvasElement.width.toDouble(), canvasElement.height.toDouble())
//                            lastDrawnIndex = -1
//                            lastDrawnBitmapsRef = null
//                        }
//                    }
//                }
//            }
//        }
//        is MediaType.Video -> {
//            DisposableEffect(objectUrl, type) {
//                htmlElement.style.objectFit = "cover"
//                val videoElement = htmlElement as HTMLVideoElement
//                var onTimeUpdate: ((Event) -> Unit)? = null
//                var onDurationChange: ((Event) -> Unit)? = null
//                var onPlay: ((Event) -> Unit)? = null
//                var onPause: ((Event) -> Unit)? = null
//
//                if (objectUrl != null) {
//                    videoElement.src = objectUrl!!
//                    videoElement.autoplay = type.autoplay
//                    videoElement.muted = true
//                    videoElement.loop = type.autoplay
//                    if (!type.autoplay) videoElement.preload = "metadata"
//
//                    type.state?.let { state ->
//                        state.play = { videoElement.play() }
//                        state.pause = { videoElement.pause() }
//                        state.seekTo = { videoElement.currentTime = it / 1000.0 }
//                        state.mute = {
//                            videoElement.muted = it
//                            state._isMuted.value = it
//                        }
//
//                        onTimeUpdate = { _: Event -> state._currentPosition.value = (videoElement.currentTime * 1000).toLong() }.also { videoElement.addEventListener("timeupdate", it) }
//                        onDurationChange = { _: Event -> state._duration.value = (videoElement.duration * 1000).toLong() }.also { videoElement.addEventListener("durationchange", it) }
//                        onPlay = { _: Event -> state._isPlaying.value = true }.also { videoElement.addEventListener("play", it) }
//                        onPause = { _: Event -> state._isPlaying.value = false }.also { videoElement.addEventListener("pause", it) }
//                    }
//                }
//
//                onDispose {
//                    videoElement.pause()
//                    videoElement.src = ""
//                    onTimeUpdate?.let { videoElement.removeEventListener("timeupdate", it) }
//                    onDurationChange?.let { videoElement.removeEventListener("durationchange", it) }
//                    onPlay?.let { videoElement.removeEventListener("play", it) }
//                    onPause?.let { videoElement.removeEventListener("pause", it) }
//                }
//            }
//        }
//        is MediaType.Image -> {
//            DisposableEffect(objectUrl) {
//                htmlElement.style.objectFit = "cover"
//                if (objectUrl != null) {
//                    (htmlElement as HTMLImageElement).src = objectUrl!!
//                }
//                onDispose {
//                    (htmlElement as HTMLImageElement).src = ""
//                }
//            }
//        }
//        is MediaType.OwnedCanvas -> {}
//    }
//
//    // 4. Render the UI (Exhaustive)
//    Box(
//        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
//        contentAlignment = Alignment.Center
//    ) {
//        when (type) {
//            is MediaType.BitmapVideo, is MediaType.OwnedCanvas -> {
//                WebElementView(
//                    factory = { htmlElement },
//                    modifier = Modifier.fillMaxSize()
//                        .drawBehind { drawRect(color = Color.Transparent, blendMode = BlendMode.Clear) },
//                    update = { el ->
//                        el.style.width = "100%"
//                        el.style.height = "100%"
//                        el.style.objectFit = "contain"
//                        el.style.backgroundColor = "black"
//                        val wrapper = el.parentElement as? HTMLElement
//                        if (wrapper != null) {
//                            wrapper.style.zIndex = "-1"
//                            if (wrapper.style.position.isEmpty()) wrapper.style.position = "absolute"
//                        }
//                    }
//                )
//            }
//            is MediaType.Image, is MediaType.Video -> {
//                if (isError) {
//                    Icon(Icons.Default.BrokenImage, "Media Error", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(24.dp))
//                } else if (filename.isNullOrBlank()) {
//                    Icon(Icons.Default.Image, "No Media", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
//                } else if (objectUrl == null) {
//                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
//                } else {
//                    WebElementView(
//                        factory = { htmlElement },
//                        modifier = Modifier.fillMaxSize().drawBehind { drawRect(color = Color.Transparent, blendMode = BlendMode.Clear) },
//                        update = { el ->
//                            el.style.width = "100%"
//                            el.style.height = "100%"
//
//                            if (type is MediaType.Video) {
//                                val videoElement = el as HTMLVideoElement
//                                videoElement.controls = type.showControls
//                                videoElement.onwheel = { event -> event.preventDefault() }
//                            }
//
//                            val wrapper = el.parentElement as? HTMLElement
//                            if (wrapper != null) {
//                                wrapper.style.zIndex = "-1"
//                                if (wrapper.style.position.isEmpty()) wrapper.style.position = "absolute"
//                            }
//                        }
//                    )
//                }
//            }
//        }
//
//        // 5. Unified Overlays
//        Box(modifier = Modifier.align(Alignment.TopEnd)) { topRight() }
//        Box(modifier = Modifier.align(Alignment.BottomEnd)) { bottomRight() }
//        Box(modifier = Modifier.align(Alignment.BottomStart)) { bottomLeft() }
//        Box(modifier = Modifier.align(Alignment.TopStart)) { topLeft() }
//    }
//}
//
//@JsFun("function(ctx, bmp, w, h) { ctx.drawImage(bmp, 0, 0, w, h); }")
//external fun drawBitmapToCanvas(ctx: CanvasRenderingContext2D, bmp: ImageBitmap, w: Double, h: Double)

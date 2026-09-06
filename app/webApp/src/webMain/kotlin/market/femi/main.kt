package market.femi

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.dom.MODULE
import org.w3c.dom.Worker
import org.w3c.dom.WorkerOptions
import org.w3c.dom.WorkerType
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.Promise

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport {

        App()
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
suspend fun <T : JsAny?> Promise<T>.await(): T = suspendCancellableCoroutine { cont ->
    this.then(
        onFulfilled = { result ->
            cont.resume(result)
            null // Return null to satisfy JS Promise signature
        },
        onRejected = { error ->
            // Wrap JS error in a Kotlin exception
            cont.resumeWithException(RuntimeException("Promise rejected: $error"))
            null
        }
    )
}
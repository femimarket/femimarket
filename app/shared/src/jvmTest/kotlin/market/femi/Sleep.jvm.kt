package market.femi

import kotlin.time.Duration

actual fun sleep(duration: Duration) {
    Thread.sleep(duration.inWholeMilliseconds)
}
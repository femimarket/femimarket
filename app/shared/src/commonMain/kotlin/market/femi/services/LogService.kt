package market.femi.services

import co.touchlab.kermit.Logger

/**
 * Thin wrapper over Kermit's global [Logger], mirroring its trailing-lambda call shape so you can
 * write `log.e { "msg" }`. Injecting it as a service (vs. calling `Logger` directly) lets tests
 * swap in a fake. Not `suspend` — Kermit logging is synchronous.
 *
 * Laziness is preserved: the impl forwards through a lambda literal `{ message() }`, which Kermit's
 * inline methods only invoke when the level is actually enabled — so the message string is never
 * built for a disabled level. `tag` null → Kermit's default tag; `throwable` for the "log a caught
 * exception" case, e.g. `log.e(err) { "boom" }`.
 */
interface LogService {
    fun i(throwable: Throwable? = null, tag: String? = null, message: () -> String)
    fun w(throwable: Throwable? = null, tag: String? = null, message: () -> String)
    fun e(throwable: Throwable? = null, tag: String? = null, message: () -> String)
    fun d(throwable: Throwable? = null, tag: String? = null, message: () -> String)
}

//fun createRealLogService(): LogService = RealKermitLogService()
fun createRealLogService(tag: String? = null): LogService =
    RealKermitLogService(if (tag != null) Logger.withTag(tag) else Logger)

class RealKermitLogService(private val logger: Logger = Logger) : LogService {
    override fun i(throwable: Throwable?, tag: String?, message: () -> String) =
        if (tag != null) logger.i(throwable, tag) { message() } else logger.i(throwable) { message() }

    override fun w(throwable: Throwable?, tag: String?, message: () -> String) =
        if (tag != null) logger.w(throwable, tag) { message() } else logger.w(throwable) { message() }

    override fun e(throwable: Throwable?, tag: String?, message: () -> String) =
        if (tag != null) logger.e(throwable, tag) { message() } else logger.e(throwable) { message() }

    override fun d(throwable: Throwable?, tag: String?, message: () -> String) =
        if (tag != null) logger.d(throwable, tag) { message() } else logger.d(throwable) { message() }
}

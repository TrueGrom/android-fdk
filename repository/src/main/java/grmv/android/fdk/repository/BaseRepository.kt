package grmv.android.fdk.repository

import grmv.android.fdk.httperror.HttpError
import grmv.android.fdk.httperror.HttpErrorMapper
import grmv.android.fdk.logging.LogSink
import grmv.android.fdk.logging.loggerForClass
import grmv.android.fdk.utils.Either
import grmv.android.fdk.utils.toLeft
import grmv.android.fdk.utils.toRight
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext

/**
 * Base class for data-layer repositories that need to dispatch work onto an I/O thread
 * and convert HTTP-client failures into the SDK's typed [HttpError] model.
 *
 * Subclasses are expected to wrap their network calls in either [httpSafeCall] (to obtain
 * an [Either] result) or [http] (to let the typed error propagate), and to call [ioContext]
 * when work must run off the calling thread. Note that [httpSafeCall] and [http] do NOT
 * switch dispatchers; wrap them in [ioContext] (or otherwise confine them) if the underlying
 * call is blocking and is not already main-safe.
 *
 * Inject a custom [BaseDispatchers] in tests to run on a test-controlled dispatcher.
 *
 * @param dispatchers Dispatcher set used by [ioContext]; override in tests to control threading.
 * @param errorMapper Consumer-supplied mapper that translates raw HTTP-client exceptions into
 *   typed [HttpError] values for [httpSafeCall] and [http].
 */
abstract class BaseRepository(
    private val dispatchers: BaseDispatchers = BaseDispatchers(),
    private val errorMapper: HttpErrorMapper
) {

    /** Logger tagged with the concrete subclass name; available to subclasses for diagnostics. */
    protected val logger: LogSink by lazy { loggerForClass() }

    /**
     * Executes [block] on the I/O dispatcher and returns its result.
     *
     * Suspends the caller until [block] completes; cancellation is propagated normally.
     *
     * @param block Suspending work to run on the I/O thread pool.
     * @return The value produced by [block].
     */
    protected suspend fun <T> ioContext(block: suspend CoroutineScope.() -> T): T {
        return withContext(dispatchers.io) {
            block()
        }
    }

    /**
     * Executes [block] and wraps the outcome in [Either], never throwing.
     *
     * [CancellationException] is rethrown immediately to preserve structured-concurrency
     * cancellation. All other [Throwable]s are passed to [HttpErrorMapper] and returned
     * as [Either.Left].
     *
     * @param block The suspending call to protect.
     * @return [Either.Right] with the result on success, or [Either.Left] with a typed
     *   [HttpError] on failure.
     */
    protected suspend fun <T : Any> httpSafeCall(
        block: suspend () -> T
    ): Either<HttpError, T> = try {
        block().toRight()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        errorMapper.map(e).toLeft()
    }

    /**
     * Executes [block], returning its result and throwing a typed [HttpError] on failure.
     *
     * Like [httpSafeCall], this does not switch dispatchers; combine with [ioContext] if needed.
     *
     * Prefer [httpSafeCall] when the caller needs to inspect the error; use this when the
     * mapped error should propagate to an upstream handler (e.g. a ViewModel error sink).
     * As with [httpSafeCall], [CancellationException] is rethrown unchanged.
     *
     * @param block The suspending HTTP call to execute.
     * @return The value produced by [block] on success.
     * @throws HttpError The [HttpErrorMapper]-mapped error when [block] fails.
     */
    protected suspend fun <T : Any> http(block: suspend () -> T): T {
        return httpSafeCall { block() }.unwrap()
    }

    private fun <T> Either<HttpError, T>.unwrap(): T {
        return when (this) {
            is Either.Left -> throw this.value
            is Either.Right -> this.value
        }
    }

    /**
     * Logs this [Throwable] at error level through [logger].
     *
     * @param message Optional context string prepended to the log entry.
     */
    protected fun Throwable.log(message: String = "") {
        logger.e(this, message)
    }
}
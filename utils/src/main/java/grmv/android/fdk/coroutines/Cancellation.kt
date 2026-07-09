package grmv.android.fdk.coroutines

import java.util.concurrent.CancellationException
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Re-throws the receiver if it is a [CancellationException], otherwise does nothing.
 *
 * Call this inside `catch (e: Throwable)` blocks to ensure structured concurrency is not
 * silently swallowed.
 *
 * Carries a Kotlin contract: when this call returns normally, the receiver is known not to
 * be a [CancellationException].
 */
@OptIn(ExperimentalContracts::class)
fun <T: Throwable> T.rethrowCancellation() {
    contract { returns() implies (this@rethrowCancellation !is CancellationException) }
    if (this is CancellationException) {
        throw this
    }
}

/**
 * Re-throws the failure cause if it is a [CancellationException]; otherwise returns the receiver unchanged.
 *
 * Use when a `Result` was produced by code that may have caught coroutine cancellation, to restore
 * proper structured-concurrency propagation before processing the result.
 *
 * @return The same `Result` instance, after ensuring cancellation is not suppressed.
 */
fun <T> Result<T>.unwrapCancellation(): Result<T> {
    return this.onFailure { it.rethrowCancellation() }
}

/**
 * Runs [block] and wraps the outcome in a [Result], re-throwing any [CancellationException] instead
 * of capturing it as a failure.
 *
 * Drop-in replacement for `runCatching` in coroutine contexts where swallowing cancellation would
 * break structured concurrency.
 *
 * @param block Suspending action to execute.
 * @return [Result.success] with the block's return value, or [Result.failure] for any non-cancellation
 * throwable.
 */
suspend fun <T> runCatchingRethrowCancellation(block: suspend () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (e: Throwable) {
        e.rethrowCancellation()
        Result.failure(e)
    }
}

/**
 * Invokes [block] when the `Result` holds a non-cancellation failure, then returns the receiver.
 *
 * Unlike [Result.onFailure], cancellation exceptions are re-thrown instead of forwarded to [block],
 * keeping structured concurrency intact.
 *
 * Because this function is `inline`, [block] may use non-local returns to exit the enclosing function.
 *
 * @param block Called with the failure cause for all throwables except [CancellationException].
 * @return The same `Result` instance.
 */
inline infix fun <T> Result<T>.onError(block: (Throwable) -> Unit): Result<T> {
    return this.onFailure { e ->
        e.rethrowCancellation()
        block(e)
    }
}

/**
 * Invokes [block] on both success and non-cancellation failure, then returns the receiver unchanged.
 *
 * A [CancellationException] is re-thrown by [onError] before [block] runs, preserving structured
 * concurrency; wrap the chain in a plain `try/finally` if cleanup must run even on cancellation.
 *
 * Because this function is `inline`, [block] may use non-local returns to exit the enclosing function.
 *
 * @param block Side effect to run for any settled result that is not a cancellation.
 * @return The same `Result` instance, so further chaining is possible.
 */
inline infix fun <T> Result<T>.onAnyResult(block: () -> Unit): Result<T> {
    return onError { block() }.onSuccess { block() }
}
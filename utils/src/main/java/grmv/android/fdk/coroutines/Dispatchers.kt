package grmv.android.fdk.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


/**
 * Executes [block] on this dispatcher and returns its result.
 *
 * Shorthand for `withContext(dispatcher) { ... }`, allowing a more readable call-site style:
 * `Dispatchers.IO.context { readFile() }`.
 *
 * @param block Suspending action to run on this dispatcher; receives a [CoroutineScope] so
 * structured-concurrency primitives like `launch` and `async` are available inside.
 * @return The value produced by [block].
 */
@OptIn(ExperimentalContracts::class)
suspend fun <T> CoroutineDispatcher.context(block: suspend CoroutineScope.() -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return withContext(this) {
        block()
    }
}
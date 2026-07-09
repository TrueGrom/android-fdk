package grmv.android.fdk.state

import grmv.android.fdk.state.RemoteData.Fetched
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Returns the payload when this is [RemoteData.Fetched]; throws [ClassCastException] otherwise.
 *
 * Prefer [fetchedOrDefault] or [ifFetched] in production paths where the state is uncertain.
 * Use this only when a non-fetched state is a programming error.
 */
fun <T> RemoteData<T>.unwrap(): T = (this as Fetched<T>).data

/** Returns the payload when [RemoteData.Fetched], otherwise the result of [default]. */
fun <T> RemoteData<T>.fetchedOrDefault(default: () -> T): T =
    (this as? Fetched<T>)?.data ?: default()

/** Returns `true` when this is [RemoteData.Fetched], smart-casting the receiver on `true`. */
@OptIn(ExperimentalContracts::class)
fun <T> RemoteData<T>.isFetched(): Boolean {
    contract { returns(true) implies (this@isFetched is Fetched<T>) }
    return this is Fetched<T>
}

/**
 * Casts to [RemoteData.Fetched], returning the wrapper rather than the bare payload.
 *
 * Prefer over [unwrap] when you need access to the [RemoteData.Fetched] type (e.g. pattern
 * matching or future fields). Throws [ClassCastException] when not fetched.
 */
fun <T> RemoteData<T>.mustBeFetched(): Fetched<T> = this as Fetched<T>

/** Invokes [action] with the payload when this is [RemoteData.Fetched]; otherwise a no-op. */
fun <T> RemoteData<T>.ifFetched(action: (T) -> Unit) {
    if (this is Fetched) action(data)
}

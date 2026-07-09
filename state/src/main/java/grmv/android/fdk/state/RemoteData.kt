package grmv.android.fdk.state

import androidx.compose.runtime.Immutable

/**
 * Represents the lifecycle of a remote data request: in-flight, succeeded, or failed.
 *
 * Exhaustive `when` expressions over this sealed class cover all three states without a fallback.
 */
@Immutable
sealed class RemoteData<out T> {

    /**
     * Stable, state-distinct key for the current variant.
     *
     * Differs across [Loading], [Fetched], and [Error] but is constant within a variant, making it
     * a convenient discriminator for Compose `key(...)` blocks or animated content keyed on the
     * lifecycle phase rather than the payload.
     */
    abstract val contentKey: String

    /**
     * The request completed successfully and [data] holds the result.
     *
     * @param data The payload returned by the remote source.
     */
    @Immutable
    data class Fetched<T>(val data: T) : RemoteData<T>() {
        override val contentKey: String = "fetched_remote_data"
    }

    /**
     * The request failed. [error] carries the cause for logging or display.
     *
     * @param error The throwable that caused the failure.
     */
    @Immutable
    data class Error(val error: Throwable) : RemoteData<Nothing>() {
        override val contentKey: String = "error_remote_data"
    }

    /** The request is in progress and no result is available yet. */
    @Immutable
    object Loading : RemoteData<Nothing>() {
        override val contentKey: String = "loading_remote_data"
    }

    companion object {
        /** Creates a [Fetched] holding [data]. */
        fun <T> fetched(data: T): RemoteData<T> = Fetched(data)

        /** Creates an [Error] holding [error]. */
        fun error(error: Throwable): RemoteData<Nothing> = Error(error)

        /** Returns the [Loading] singleton. */
        fun loading(): RemoteData<Nothing> = Loading
    }
}
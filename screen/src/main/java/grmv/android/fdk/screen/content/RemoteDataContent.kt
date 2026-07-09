package grmv.android.fdk.screen.content

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import grmv.android.fdk.state.RemoteData
import grmv.android.fdk.state.RemoteData.Error
import grmv.android.fdk.state.RemoteData.Fetched
import grmv.android.fdk.state.RemoteData.Loading
import grmv.android.fdk.state.StateViewModel
import grmv.android.fdk.state.traits.RemoteDataState

/**
 * Collects this ViewModel's [RemoteDataState] and renders its [RemoteData] via the [content] DSL.
 *
 * State collection is lifecycle-aware: the flow is suspended while the host lifecycle is below
 * `STARTED` and resumes automatically on return to foreground.
 *
 * @param T The data type carried by [RemoteData.Fetched].
 * @param S The state type; must implement [RemoteDataState].
 * @param content DSL block — call [RemoteDataScopeBuilder.Fetched], [RemoteDataScopeBuilder.Loading],
 *   and/or [RemoteDataScopeBuilder.Error] to provide slot composables. Unset slots fall back to their
 *   defaults (see [RemoteDataScopeBuilder]).
 */
@Composable
fun <T, S : RemoteDataState<S, T>> StateViewModel<S, *>.Fetchable(
    content: RemoteDataScopeBuilder<T>.() -> Unit,
) {
    // `state` here is the collected snapshot, shadowing the receiver's StateFlow of the same name.
    val state by state.collectAsStateWithLifecycle()
    state.remoteData.Fetchable(content)
}

/**
 * Renders this [RemoteData] through the [content] DSL, dispatching to the
 * [RemoteDataScopeBuilder.Fetched], [RemoteDataScopeBuilder.Loading], or
 * [RemoteDataScopeBuilder.Error] slot for the current state.
 *
 * Prefer the [StateViewModel] overload when this value comes from a ViewModel — it handles
 * lifecycle-aware collection automatically. Use this overload when you already hold a [RemoteData]
 * instance (e.g. from a nested state field).
 *
 * @param T The data type carried by [RemoteData.Fetched].
 * @param content DSL block configuring slots for each [RemoteData] variant.
 */
@Composable
fun <T> RemoteData<T>.Fetchable(content: RemoteDataScopeBuilder<T>.() -> Unit) {
    val scope = RemoteDataScopeBuilderImpl<T>().apply(content).build()
    when (this) {
        is Fetched -> scope.fetched(data)
        is Loading -> scope.loading()
        is Error -> scope.error(error)
    }
}

/**
 * DSL for configuring per-state slots inside [RemoteData.Fetchable].
 *
 * All slots are optional — omitting one activates the default:
 * - [Fetched]: no-op (nothing rendered).
 * - [Loading]: centered [LoadingDefaults.Loading] sourced from [LocalLoadingDefaults].
 * - [Error]: centered [LoadingDefaults.Error] sourced from [LocalLoadingDefaults] (no retry action).
 *
 * For the common error case, prefer [retry] over [Error]: it renders the app-wide
 * [LoadingDefaults.Error] and wires its retry action for you.
 *
 * @param T The data type exposed to the [Fetched] slot.
 */
interface RemoteDataScopeBuilder<T> {
    /** Renders [content] when data has been fetched successfully, passing the loaded value. */
    fun Fetched(content: @Composable (T) -> Unit)

    /**
     * Overrides the default loading UI for this call site.
     *
     * When omitted, the [LocalLoadingDefaults] loading indicator is shown centered in a
     * `fillMaxSize` box — the same indicator supplied by [LoadingDefaults.Loading].
     */
    fun Loading(content: @Composable () -> Unit)

    /**
     * Renders [content] when the fetch ended in an error, passing the [Throwable].
     *
     * Defaults to the app-wide [LoadingDefaults.Error] (from [LocalLoadingDefaults]) with no retry
     * action; set this slot to fully customize, or use [retry] to keep the default UI with a retry.
     */
    fun Error(content: @Composable (Throwable) -> Unit)

    /**
     * Renders the app-wide [LoadingDefaults.Error] UI (from [LocalLoadingDefaults]) for a failed
     * fetch, centered in a `fillMaxSize` box, wiring its retry action to [onRetry].
     *
     * Use this when you want the default error presentation but need to supply retry behavior; use
     * [Error] to fully customize the slot. [retry] and [Error] write the same error slot, so calling
     * both keeps only the last one.
     */
    fun retry(onRetry: (Throwable) -> Unit)
}

private class RemoteDataScopeBuilderImpl<T> : RemoteDataScopeBuilder<T> {
    private var fetched: @Composable (T) -> Unit = {}
    private var loading: @Composable () -> Unit = {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            with(LocalLoadingDefaults.current) { Loading() }
        }
    }
    private var error: @Composable (Throwable) -> Unit = defaultError(onRetry = {})

    override fun Fetched(content: @Composable (T) -> Unit) { fetched = content }
    override fun Loading(content: @Composable () -> Unit) { loading = content }
    override fun Error(content: @Composable (Throwable) -> Unit) { error = content }
    override fun retry(onRetry: (Throwable) -> Unit) { error = defaultError(onRetry) }
    fun build() = RemoteDataScopeImpl(fetched, loading, error)
}

/** Centered app-wide [LoadingDefaults.Error] (from [LocalLoadingDefaults]), wired to [onRetry]. */
private fun defaultError(onRetry: (Throwable) -> Unit): @Composable (Throwable) -> Unit = { e ->
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        with(LocalLoadingDefaults.current) { Error(e, onRetry) }
    }
}

private class RemoteDataScopeImpl<T>(
    val fetched: @Composable (T) -> Unit,
    val loading: @Composable () -> Unit,
    val error: @Composable (Throwable) -> Unit,
)

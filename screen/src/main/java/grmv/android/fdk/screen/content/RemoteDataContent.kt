package grmv.android.fdk.screen.content

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
 * Precedence (state-slot animation): [RemoteDataScopeBuilder.transition] > [LocalContentTransitions]
 * > [FdkNoContentTransitions], i.e. no animation unless one is provided.
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
 * Precedence (state-slot animation): [RemoteDataScopeBuilder.transition] > [LocalContentTransitions]
 * > [FdkNoContentTransitions], i.e. no animation unless one is provided. While a transition runs,
 * slots can read [LocalContentTransitionScope] to animate their own children.
 *
 * @param T The data type carried by [RemoteData.Fetched].
 * @param content DSL block configuring slots for each [RemoteData] variant.
 */
@Composable
fun <T> RemoteData<T>.Fetchable(content: RemoteDataScopeBuilder<T>.() -> Unit) {
    val scope = RemoteDataScopeBuilderImpl<T>().apply(content).build()
    // A call site that set the slot wins outright, including when it returns null to opt out of an
    // app-wide provider — an elvis here would send that null on to the provider instead.
    val perCall = scope.transition
    val transform =
        if (perCall != null) perCall() else LocalContentTransitions.current.transform()
    if (transform == null) {
        // Shadowed so a slot nested under an animated ancestor cannot mistake that ancestor's
        // scope for its own; this is a composition group only, no layout node.
        CompositionLocalProvider(LocalContentTransitionScope provides null) {
            scope.Dispatch(this)
        }
    } else {
        AnimatedContent(
            targetState = this,
            // AnimatedContent's default alignment is TopStart; the container is sized to the union
            // of the outgoing and incoming slots while both are present, so centring keeps a
            // smaller slot in place instead of pinning it to the corner.
            contentAlignment = Alignment.Center,
            // Keyed on the lifecycle phase, so a new Fetched payload recomposes without
            // re-running the transition.
            contentKey = { it.contentKey },
            transitionSpec = { transform },
            label = "Fetchable",
        ) { state ->
            // `this` is the AnimatedContentScope of the running transition; publishing it lets a
            // slot animate its own children. Left null on the un-animated path above, where there
            // is no transition to attach to.
            CompositionLocalProvider(LocalContentTransitionScope provides this) {
                scope.Dispatch(state)
            }
        }
    }
}

@Composable
private fun <T> RemoteDataScopeImpl<T>.Dispatch(state: RemoteData<T>) {
    when (state) {
        is Fetched -> fetched(state.data)
        is Loading -> loading()
        is Error -> error(state.error)
    }
}

/**
 * DSL for configuring per-state slots inside [RemoteData.Fetchable].
 *
 * All slots are optional — omitting one activates the default:
 * - [Fetched]: no-op (nothing rendered).
 * - [Loading]: centered [LoadingDefaults.Loading] sourced from [LocalLoadingDefaults].
 * - [Error]: centered [LoadingDefaults.Error] sourced from [LocalLoadingDefaults] (no retry action).
 * - [transition]: the current [LocalContentTransitions], itself [FdkNoContentTransitions] (no
 *   animation) unless a provider is installed.
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

    /**
     * Overrides the state-slot transition for this call site.
     *
     * Return `null` to swap slots in a single frame — this is how a call site opts out of an
     * app-wide [LocalContentTransitions] provider. When this slot is left unset, the current
     * [LocalContentTransitions] applies, which itself defaults to [FdkNoContentTransitions] (no
     * animation).
     *
     * [spec] runs in composition, so it may read `MaterialTheme` and other composition locals:
     *
     * ```
     * transition {
     *     val spec = tween<Float>(AppMotion.MediumDuration)
     *     ContentTransform(fadeIn(spec), fadeOut(spec), sizeTransform = null)
     * }
     * ```
     *
     * While a transition is running, the slots can read [LocalContentTransitionScope] to animate
     * their own children; it is `null` whenever this resolves to `null`.
     *
     * The result is not expected to change at runtime. Going between `null` and non-`null` while
     * composed switches between two distinct subtrees and resets the slot content's state; changing
     * one non-`null` transform for another applies only from the next state change onwards, because
     * `AnimatedContent` remembers the transform it resolved for the current one.
     */
    fun transition(spec: @Composable () -> ContentTransform?) = Unit
}

private class RemoteDataScopeBuilderImpl<T> : RemoteDataScopeBuilder<T> {
    private var fetched: @Composable (T) -> Unit = {}
    private var loading: @Composable () -> Unit = {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            with(LocalLoadingDefaults.current) { Loading() }
        }
    }
    private var error: @Composable (Throwable) -> Unit = defaultError(onRetry = {})
    private var transition: (@Composable () -> ContentTransform?)? = null

    override fun Fetched(content: @Composable (T) -> Unit) { fetched = content }
    override fun Loading(content: @Composable () -> Unit) { loading = content }
    override fun Error(content: @Composable (Throwable) -> Unit) { error = content }
    override fun retry(onRetry: (Throwable) -> Unit) { error = defaultError(onRetry) }
    override fun transition(spec: @Composable () -> ContentTransform?) { transition = spec }
    fun build() = RemoteDataScopeImpl(fetched, loading, error, transition)
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
    val transition: (@Composable () -> ContentTransform?)?,
)

package grmv.android.fdk.state

import androidx.compose.runtime.Stable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

/**
 * Pull-to-refresh contract mixed into a ViewModel and consumed by the `FdKitRefresh*` containers.
 *
 * Exposes the in-flight flag as [refreshing] and a [refresh] trigger. The screen module's
 * `FdKitRefreshContainer` / `FdKitRefreshColumn` / `FdKitRefreshLazyColumn` accept a [RefreshOwner]
 * receiver and wire both automatically.
 *
 * Errors are deliberately NOT handled here: refresh work must route failures through the same path
 * as any other operation (e.g. the ViewModel's `ErrorEmitter`).
 *
 * End-to-end usage — mix in [RefreshController] by class delegation and bind it in `init`:
 * ```
 * class FeedViewModel(
 *     private val refresher: RefreshController = RefreshController(),
 * ) : BaseViewModel(), RefreshOwner by refresher {
 *
 *     init { refresher.initialize(viewModelScope, ::reload) }
 *
 *     private suspend fun reload() { /* same code path as the initial load */ }
 * }
 * ```
 * Screen side:
 * ```
 * viewModel.FdKitRefreshLazyColumn { items(...) { ... } }
 * ```
 *
 * Screens that keep the refreshing flag inside their [BaseState] (e.g. via a custom trait) can
 * skip this contract entirely and call the primitive `FdKitRefresh*` overloads
 * (`isRefreshing` + `onRefresh`) instead — the two approaches interoperate without adapters.
 */
@Stable
interface RefreshOwner {
    /** `true` while refresh work is in flight. */
    val refreshing: StateFlow<Boolean>

    /** Starts the refresh work; ignored while a refresh is already in flight. */
    fun refresh()
}

/**
 * Default [RefreshOwner] implementation intended as a `by`-delegation target.
 *
 * Two-phase setup: construct as a constructor default (the `by` expression cannot reference
 * `viewModelScope`), then bind [scope] and [work] via [initialize] in `init`. Calling [refresh]
 * before [initialize] throws [UninitializedPropertyAccessException] — an intentional fail-fast.
 *
 * Concurrent [refresh] calls coalesce into one run; the flag resets on completion, failure, and
 * cancellation alike — including a [refresh] issued after [scope] was already cancelled.
 * Exceptions from [work] propagate to [scope] untouched.
 */
@Stable
class RefreshController : RefreshOwner {
    private lateinit var scope: CoroutineScope
    private lateinit var work: suspend () -> Unit

    private val _refreshing = MutableStateFlow(false)
    override val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    /**
     * Binds the coroutine [scope] the refresh runs in and the [work] it performs.
     *
     * @param scope typically `viewModelScope`.
     * @param work the refresh operation; handle errors inside (or let your error system catch them).
     */
    fun initialize(scope: CoroutineScope, work: suspend () -> Unit) {
        this.scope = scope
        this.work = work
    }

    override fun refresh() {
        if (!_refreshing.compareAndSet(expect = false, update = true)) return
        val job = scope.launch {
            yield() // guarantee `refreshing = true` is observable even if `work` never suspends
            work()
        }
        // Not a `finally` inside the coroutine: launching on an already-cancelled scope produces a
        // coroutine whose body never runs, which would leave the flag raised forever. A completion
        // handler fires for that case too.
        job.invokeOnCompletion { _refreshing.value = false }
    }
}

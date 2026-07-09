package grmv.android.fdk.screen.paging

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.Vertical
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Horizontal
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import grmv.android.fdk.screen.LocalContentPaddingDefaults
import kotlinx.coroutines.flow.Flow

/**
 * Renders this [Flow] of [PagingData] as a pull-to-refresh [LazyColumn], driven by the [content] DSL.
 *
 * Handles refresh/append/prepend load states and the empty case. The load-state presentations
 * (initial loading, refresh error, append loading/error) resolve in this order of precedence:
 *
 * 1. **Per-call slot** — a slot set in the [content] DSL ([PagingScopeBuilder.Loading],
 *    [PagingScopeBuilder.Error], [PagingScopeBuilder.AppendLoading], [PagingScopeBuilder.AppendError])
 *    always wins for that one list.
 * 2. **App-wide default** — otherwise the current [LocalPagingDefaults] is used.
 * 3. **Material3 fallback** — [LocalPagingDefaults] itself defaults to [Material3PagingDefaults].
 *
 * The fallback's loading spinners delegate to the shared `LocalLoadingDefaults`. To skin *only* the
 * loader across both paging and `Fetchable` lists, provide `ProvideLoadingDefaults` instead of a full
 * [PagingDefaults].
 *
 * ### Providing app-wide defaults
 *
 * To skin *every* paged list with your design system's loaders instead of the Material3 fallback,
 * implement [PagingDefaults] and install it once, high in the composition (e.g. at the app/theme
 * root), via [ProvidePagingDefaults]:
 *
 * ```
 * object AppPagingDefaults : PagingDefaults {
 *     @Composable override fun LazyItemScope.Loading() { /* your full-list loader */ }
 *     @Composable override fun LazyItemScope.Error(retry: () -> Unit) { /* your error + retry */ }
 *     @Composable override fun LazyItemScope.AppendLoading() { /* your footer loader */ }
 *     @Composable override fun LazyItemScope.AppendError(retry: () -> Unit) { /* your footer error */ }
 * }
 *
 * // App root — applies to all PagingContent below it:
 * ProvidePagingDefaults(AppPagingDefaults) {
 *     AppContent()
 * }
 * ```
 *
 * Every [PagingContent] beneath the provider now uses those loaders by default; a call site can
 * still override a single state via its DSL slot. [PagingScopeBuilder.Item],
 * [PagingScopeBuilder.Empty] and [PagingScopeBuilder.Prepend] are not part of [PagingDefaults] —
 * `Item` is required per call, `Empty`/`Prepend` default to nothing.
 *
 * ### Programmatic refresh
 *
 * Beyond pull-to-refresh, a call site can drive [LazyPagingItems.refresh]/[LazyPagingItems.retry]
 * itself — e.g. from a toolbar button or an incoming event. Hoist a [FdkPagingController] with
 * [rememberPagingController], pass it in, and call it:
 *
 * ```
 * val paging = rememberPagingController()
 * Button(onClick = paging::refresh) { Text("Reload") }
 * flow.PagingContent(itemKey = { it.id }, controller = paging) { Item { _, x -> Row(x) } }
 * ```
 *
 * @param itemKey stable key for each item, used for efficient list updates.
 * @param contentPadding padding around the list content; defaults to the current [ContentPaddingDefaults].
 * @param controller optional handle for triggering [refresh][FdkPagingController.refresh]/
 *   [retry][FdkPagingController.retry] programmatically; create it with [rememberPagingController].
 * @param content the slot DSL describing item, load-state and header presentations for this list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : Any> Flow<PagingData<T>>.PagingContent(
    itemKey: (T) -> String,
    contentPadding: PaddingValues = LocalContentPaddingDefaults.current.contentPadding,
    verticalArrangement: Vertical = Arrangement.spacedBy(12.dp),
    horizontalAlignment: Horizontal = Alignment.CenterHorizontally,
    controller: FdkPagingController? = null,
    content: PagingScopeBuilder<T>.() -> Unit,
) {
    val scope = PagingScopeBuilderImpl<T>().apply(content).build()
    val items = collectAsLazyPagingItems()
    controller?.let { c ->
        DisposableEffect(c, items) {
            c.bind(items)
            onDispose { c.unbind(items) }
        }
    }
    var isRefreshing by rememberSaveable { mutableStateOf(false) }
    val refreshState = rememberPullToRefreshState()
    EndRefreshOnLoadingEvent(isRefreshing, items) {
        isRefreshing = false
    }
    val listState = rememberLazyListState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        state = refreshState,
        onRefresh = {
            isRefreshing = true
            items.refresh()
        },
    ) {
        LazyColumn(
            state = listState,
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
        ) {
            scope.prepend?.invoke(this)
            items.ifEmpty {
                item("paging_slot_empty") { scope.empty(this) }
            }
            items.expand(
                isRefreshing = isRefreshing,
                ready = {
                    items(
                        items.itemCount,
                        key = items.itemKey(itemKey),
                    ) { index ->
                        items[index]?.let { element ->
                            scope.item(this, index, element)
                        }
                    }
                    items.ifAppendPageLoading {
                        item("paging_slot_append_loading") { scope.appendLoading(this) }
                    }
                    items.ifAppendPageError {
                        item("paging_slot_append_error") { scope.appendError(this) { items.retry() } }
                    }
                },
                loading = {
                    item("paging_slot_loading") {
                        scope.loading(this)
                    }
                },
                error = {
                    item("paging_slot_error") {
                        scope.error(this) { items.retry() }
                    }
                },
            )
        }
    }
}

/**
 * DSL receiver for configuring the slot composables of a single [PagingContent] call.
 *
 * [Item] is the only required slot. All others fall back to [LocalPagingDefaults] when unset,
 * which itself defaults to [Material3PagingDefaults].
 *
 * @param T the item type held in the [androidx.paging.PagingData] stream.
 */
interface PagingScopeBuilder<T : Any> {
    /** Content for each loaded item, given its index and value. */
    fun Item(content: @Composable LazyItemScope.(Int, T) -> Unit)

    /** Full-list initial loading content; defaults to a centered progress indicator. */
    fun Loading(content: @Composable LazyItemScope.() -> Unit)

    /** Full-list refresh error content; defaults to a message with a retry button. */
    fun Error(content: @Composable LazyItemScope.(retry: () -> Unit) -> Unit)

    /** Content shown when the list is empty; defaults to empty. */
    fun Empty(content: @Composable LazyItemScope.() -> Unit)

    /** Append (next page) loading footer; defaults to a small progress indicator. */
    fun AppendLoading(content: @Composable LazyItemScope.() -> Unit)

    /** Append (next page) error footer; defaults to a message with a retry button. */
    fun AppendError(content: @Composable LazyItemScope.(retry: () -> Unit) -> Unit)

    /** Optional header items emitted before the list content. */
    fun Prepend(content: LazyListScope.() -> Unit)
}

private class PagingScopeBuilderImpl<T : Any> : PagingScopeBuilder<T> {
    private var item: @Composable LazyItemScope.(Int, T) -> Unit = { _, _ -> }
    private var loading: @Composable LazyItemScope.() -> Unit = {
        with(LocalPagingDefaults.current) { Loading() }
    }
    private var error: @Composable LazyItemScope.(retry: () -> Unit) -> Unit = { retry ->
        with(LocalPagingDefaults.current) { Error(retry) }
    }
    private var empty: @Composable LazyItemScope.() -> Unit = {}
    private var appendLoading: @Composable LazyItemScope.() -> Unit = {
        with(LocalPagingDefaults.current) { AppendLoading() }
    }
    private var appendError: @Composable LazyItemScope.(retry: () -> Unit) -> Unit = { retry ->
        with(LocalPagingDefaults.current) { AppendError(retry) }
    }
    private var prepend: (LazyListScope.() -> Unit)? = null

    override fun Item(content: @Composable LazyItemScope.(Int, T) -> Unit) { item = content }
    override fun Loading(content: @Composable LazyItemScope.() -> Unit) { loading = content }
    override fun Error(content: @Composable LazyItemScope.(retry: () -> Unit) -> Unit) { error = content }
    override fun Empty(content: @Composable LazyItemScope.() -> Unit) { empty = content }
    override fun AppendLoading(content: @Composable LazyItemScope.() -> Unit) { appendLoading = content }
    override fun AppendError(content: @Composable LazyItemScope.(retry: () -> Unit) -> Unit) { appendError = content }
    override fun Prepend(content: LazyListScope.() -> Unit) { prepend = content }

    fun build() = PagingScopeImpl(item, loading, error, empty, appendLoading, appendError, prepend)
}

/**
 * Hoistable handle for driving a [PagingContent] list programmatically.
 *
 * Create one with [rememberPagingController], pass it to [PagingContent], then call [refresh] or
 * [retry] from anywhere in the composition (buttons, event effects, etc.). Calls are no-ops until
 * the controller is bound to a live list, and after it leaves the composition.
 */
@Stable
class FdkPagingController internal constructor() {
    private var items: LazyPagingItems<*>? = null

    internal fun bind(items: LazyPagingItems<*>) { this.items = items }

    internal fun unbind(items: LazyPagingItems<*>) {
        if (this.items === items) this.items = null
    }

    /** Reload the list from the first page. Mirrors [LazyPagingItems.refresh]. */
    fun refresh() { items?.refresh() }

    /** Retry the failed load. Mirrors [LazyPagingItems.retry]. */
    fun retry() { items?.retry() }
}

/** Remembers a [FdkPagingController] for hoisting refresh/retry control out of [PagingContent]. */
@Composable
fun rememberPagingController(): FdkPagingController = remember { FdkPagingController() }

private class PagingScopeImpl<T : Any>(
    val item: @Composable LazyItemScope.(Int, T) -> Unit,
    val loading: @Composable LazyItemScope.() -> Unit,
    val error: @Composable LazyItemScope.(retry: () -> Unit) -> Unit,
    val empty: @Composable LazyItemScope.() -> Unit,
    val appendLoading: @Composable LazyItemScope.() -> Unit,
    val appendError: @Composable LazyItemScope.(retry: () -> Unit) -> Unit,
    val prepend: (LazyListScope.() -> Unit)?,
)

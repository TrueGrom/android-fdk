package grmv.android.fdk.screen.paging

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.Vertical
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Horizontal
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import grmv.android.fdk.screen.LocalContentPaddingDefaults
import grmv.android.fdk.screen.content.LocalContentTransitions
import kotlinx.coroutines.flow.Flow

/**
 * Renders this [Flow] of [PagingData] as a pull-to-refresh [LazyColumn], driven by the [content] DSL.
 *
 * For content laid out as tiles rather than rows, [PagingGridContent] is the same thing over a
 * `LazyVerticalGrid`. For a paged *section* inside a list or grid the call site already owns, there
 * is a `pagingItems` extension on each scope — this composable is a thin wrapper over the
 * `LazyListScope` one. All of them resolve their load-state slots through the same
 * [PagingDefaults].
 *
 * Handles refresh/append/prepend load states and the empty case. The load-state presentations
 * (initial loading, refresh error, append loading/error) resolve in this order of precedence:
 *
 * 1. **Per-call slot** — a slot set in the [content] DSL ([FdkPagingSlotsBuilder.Loading],
 *    [FdkPagingSlotsBuilder.EmptyError], [FdkPagingSlotsBuilder.RefreshError],
 *    [FdkPagingSlotsBuilder.PrependLoading], [FdkPagingSlotsBuilder.PrependError],
 *    [FdkPagingSlotsBuilder.AppendLoading], [FdkPagingSlotsBuilder.AppendError])
 *    always wins for that one list.
 * 2. **App-wide default** — otherwise the current [LocalPagingDefaults] is used.
 * 3. **Material3 fallback** — [LocalPagingDefaults] itself defaults to [Material3PagingDefaults].
 *
 * The fallback delegates every slot to the shared `LocalLoadingDefaults` — the loaders and the
 * error presentation alike — so an app that supplies only a `LoadingDefaults` already words a paged
 * failure the way it words a non-paged one. Provide `ProvideLoadingDefaults` instead of a full
 * [PagingDefaults] when the layout of the paged states is fine and only their look is not.
 *
 * ### Providing app-wide defaults
 *
 * To skin *every* paged list with your design system's loaders instead of the Material3 fallback,
 * implement [PagingDefaults] and install it once, high in the composition (e.g. at the app/theme
 * root), via [ProvidePagingDefaults]:
 *
 * ```
 * object AppPagingDefaults : PagingDefaults {
 *     @Composable override fun FdkPagingSlotScope.Loading() { /* your full-viewport loader */ }
 *     @Composable override fun FdkPagingSlotScope.EmptyError(e: Throwable, retry: () -> Unit) { /* your empty-state error + retry */ }
 *     @Composable override fun FdkPagingSlotScope.RefreshError(e: Throwable, retry: () -> Unit) { /* your banner over loaded items */ }
 *     @Composable override fun FdkPagingSlotScope.AppendLoading() { /* your footer loader */ }
 *     @Composable override fun FdkPagingSlotScope.AppendError(e: Throwable, retry: () -> Unit) { /* your footer error */ }
 * }
 *
 * // App root — applies to all PagingContent below it:
 * ProvidePagingDefaults(AppPagingDefaults) {
 *     AppContent()
 * }
 * ```
 *
 * The slots receive [FdkPagingSlotScope], not `LazyItemScope`, so that one instance serves lists and
 * grids alike; it carries the `fillParentMax*` and `animateItem` a slot needs.
 *
 * ### Loaded items are never replaced
 *
 * [FdkPagingSlotsBuilder.Loading] and [FdkPagingSlotsBuilder.EmptyError] are the *empty-state*
 * presentations: once items have loaded they stay on screen. A [androidx.paging.PagingSource] over a
 * local store is invalidated by writes this screen never asked about, and each invalidation drives
 * refresh back through `Loading` — a full-viewport loader on every one of them would make the list
 * flicker. A refresh that *fails* over loaded items is reported by [FdkPagingSlotsBuilder.RefreshError]
 * instead: a banner emitted ahead of the items, defaulting to the append error's presentation. When
 * it appears while the list rests at its top, the list moves to show it ([KeepRefreshErrorInView]);
 * a list scrolled into its content stays where it is.
 *
 * The load-state slot animation resolves separately:
 * [FdkPagingSlotsBuilder.transition] > [LocalContentTransitions] > no animation. Loaded items are
 * never animated — only the load-state slots.
 *
 * Every [PagingContent] beneath the provider now uses those loaders by default; a call site can
 * still override a single state via its DSL slot. [FdkPagingScopeBuilder.Item],
 * [FdkPagingSlotsBuilder.Empty] and [FdkPagingScopeBuilder.Header] are not part of [PagingDefaults] —
 * `Item` is required per call, `Empty`/`Header` default to nothing.
 *
 * ### Programmatic refresh
 *
 * Beyond pull-to-refresh, a call site can drive [LazyPagingItems.refresh]/[LazyPagingItems.retry]
 * itself — e.g. from a toolbar button or an incoming event. Hoist a [FdkPagingController] with
 * [rememberPagingController], pass it in, and call it:
 *
 * ```
 * val paging = rememberPagingController(showsRefreshIndicator = true)
 * Button(onClick = paging::refresh) { Text("Reload") }
 * flow.PagingContent(itemKey = { it.id.toString() }, controller = paging) { Item { _, x -> Row(x) } }
 * ```
 *
 * The pull-to-refresh indicator is drawn by the current
 * [LocalRefreshDefaults][grmv.android.fdk.screen.refresh.LocalRefreshDefaults], so it matches the
 * `FdKitRefresh*` containers and [PagingGridContent].
 *
 * ### Driving the pull from the screen
 *
 * By default a pull calls [LazyPagingItems.refresh] and the indicator retracts when that reload
 * settles. A screen whose pull reloads more than the paged content — a header, a summary, anything
 * fetched beside the list — hoists both halves instead:
 *
 * ```
 * val paging = rememberPagingController()
 * flow.PagingContent(
 *     itemKey = { it.id.toString() },
 *     controller = paging,
 *     isRefreshing = state.isRefreshing,
 *     onRefresh = { viewModel.reload(paging::refresh) },
 * ) { Item { _, x -> Row(x) } }
 * ```
 *
 * The indicator then follows the screen's own flag, and the built-in reload steps aside entirely —
 * [onRefresh] is responsible for reloading the paged content too. This replaces dropping down to
 * [pagingItems] and a hand-rolled `PullToRefreshBox`.
 *
 * @param itemKey stable key for each item, used for efficient list updates.
 * @param contentPadding padding around the list content; defaults to the current
 *   [ContentPaddingDefaults][grmv.android.fdk.screen.ContentPaddingDefaults].
 * @param verticalArrangement spacing between items.
 * @param horizontalAlignment cross-axis alignment of items narrower than the list.
 * @param controller optional handle for triggering [refresh][FdkPagingController.refresh]/
 *   [retry][FdkPagingController.retry] programmatically; create it with [rememberPagingController].
 * @param state the list's scroll position; hoist it to read or drive the scroll from the screen.
 * @param isRefreshing caller-owned pull-to-refresh flag; `null` (the default) lets this composable
 *   raise and lower its own. Pass one when the pull reloads more than the paged content and the
 *   indicator must stay up until all of it has settled; it requires [onRefresh], since nothing else
 *   would raise it. Decide once: a value that switches between `null` and non-null while a reload is
 *   in flight leaves the indicator showing the wrong thing, because neither flag knows what the
 *   other one started.
 * @param onRefresh called instead of [LazyPagingItems.refresh] when the user pulls; `null` (the
 *   default) keeps the built-in reload. Whoever sets it must reload the paged content from it
 *   (through a [FdkPagingController], say) — nothing else will. On its own it keeps the built-in
 *   flag, which still retracts the indicator once that reload settles; with [isRefreshing] it hands
 *   the whole gesture over.
 * @param content the slot DSL describing item, load-state and header presentations for this list.
 */
@Composable
fun <T : Any> Flow<PagingData<T>>.PagingContent(
    itemKey: (T) -> String,
    contentPadding: PaddingValues = LocalContentPaddingDefaults.current.contentPadding,
    verticalArrangement: Vertical = Arrangement.spacedBy(12.dp),
    horizontalAlignment: Horizontal = Alignment.CenterHorizontally,
    controller: FdkPagingController? = null,
    state: LazyListState = rememberLazyListState(),
    isRefreshing: Boolean? = null,
    onRefresh: (() -> Unit)? = null,
    content: FdkPagingScopeBuilder<T>.() -> Unit,
) {
    PagedPullToRefresh(controller, isRefreshing, onRefresh) { items, refreshing ->
        KeepRefreshErrorInView(state, items)
        LazyColumn(
            state = state,
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
        ) {
            pagingItems(
                items = items,
                itemKey = itemKey,
                isRefreshing = refreshing,
                content = content,
            )
        }
    }
}

/**
 * Emits [items] and their load-state slots into a list the call site owns.
 *
 * The shape [PagingContent] is built on, and the one a paged *section* needs: a screen whose header,
 * summary card and paged content share a single [LazyColumn] cannot hand that content to a
 * composable owning its own list, but it can call this from inside the list it already has. The load
 * states are resolved through the same [PagingDefaults] either way.
 *
 * [LazyGridScope.pagingItems][grmv.android.fdk.screen.paging.pagingItems] is the grid twin.
 *
 * ```
 * LazyColumn(state = listState) {
 *     item { Header(summary) }
 *     pagingItems(items, itemKey = { it.id.toString() }) {
 *         Item { _, element -> Row(element) }
 *     }
 * }
 * ```
 *
 * Pull-to-refresh is the caller's: this emits items, it does not wrap them in a gesture. A screen
 * that pulls to refresh its whole content passes its own flag as [isRefreshing] so the full-viewport
 * loading slot does not flash on top of the pull indicator.
 *
 * The scroll state is the caller's as well: call [KeepRefreshErrorInView] beside the list with its
 * state and these [items], so a refresh-error banner appearing over a list at its top is not
 * inserted above the viewport.
 *
 * @param items the collected paging items — `flow.collectAsLazyPagingItems()` in the caller, since
 *   this is not a composable.
 * @param itemKey stable key for each item, used for efficient updates and for `animateItem`.
 * @param isRefreshing `true` while the caller's own pull-to-refresh is in flight; suppresses the
 *   full-viewport loading slot for its duration.
 * @param content the slot DSL describing item, load-state and header presentations.
 */
fun <T : Any> LazyListScope.pagingItems(
    items: LazyPagingItems<T>,
    itemKey: (T) -> String,
    isRefreshing: Boolean = false,
    content: FdkPagingScopeBuilder<T>.() -> Unit,
) {
    val scope = PagingScopeBuilderImpl<T>().apply(content).build()
    scope.header?.invoke(this)
    emitPagingSlots(
        items = items,
        slots = scope.slots,
        isRefreshing = isRefreshing,
        slot = { key, fadeOut, slotContent ->
            item(key) {
                PagingSlot(ListPagingSlotScope(this), scope.slots, fadeOut, slotContent)
            }
        },
        loadedItems = {
            items(
                count = items.itemCount,
                key = items.itemKey(itemKey),
            ) { index ->
                items[index]?.let { element ->
                    scope.item(this, index, element)
                }
            }
        },
    )
}

/**
 * DSL receiver for configuring the slot composables of a single [PagingContent] call.
 *
 * [Item] is the only required slot. The load-state slots ([FdkPagingSlotsBuilder.Loading],
 * [FdkPagingSlotsBuilder.EmptyError], [FdkPagingSlotsBuilder.AppendLoading], [FdkPagingSlotsBuilder.AppendError])
 * fall back to [LocalPagingDefaults], which itself defaults to [Material3PagingDefaults];
 * [FdkPagingSlotsBuilder.Empty] and [Header] default to rendering nothing;
 * [FdkPagingSlotsBuilder.transition] falls back to [LocalContentTransitions], which itself defaults to
 * no animation.
 *
 * [FdkPagingGridScopeBuilder] is the same DSL for a grid: the load-state slots are shared verbatim,
 * and only [Item] and [Header] — the layout-specific halves — differ.
 *
 * @param T the item type held in the [androidx.paging.PagingData] stream.
 */
interface FdkPagingScopeBuilder<T : Any> : FdkPagingSlotsBuilder {
    /** Content for each loaded item, given its index and value. */
    fun Item(content: @Composable LazyItemScope.(Int, T) -> Unit)

    /** Optional static content emitted above the paged items. Unrelated to
     * [FdkPagingSlotsBuilder.PrependLoading], which is the paging load state at that end. */
    fun Header(content: LazyListScope.() -> Unit)
}

private class PagingScopeBuilderImpl<T : Any> : PagingSlotsBuilderImpl(), FdkPagingScopeBuilder<T> {
    private var item: @Composable LazyItemScope.(Int, T) -> Unit = { _, _ -> }
    private var header: (LazyListScope.() -> Unit)? = null

    override fun Item(content: @Composable LazyItemScope.(Int, T) -> Unit) { item = content }
    override fun Header(content: LazyListScope.() -> Unit) { header = content }

    fun build() = PagingScopeImpl(item, header, buildSlots())
}

/**
 * Hoistable handle for driving a [PagingContent] list programmatically.
 *
 * Create one with [rememberPagingController], pass it to [PagingContent] or [PagingGridContent],
 * then call [refresh] or [retry] from anywhere in the composition (buttons, event effects, etc.).
 * Calls are no-ops until the controller is bound to a live list, and after it leaves the
 * composition.
 */
@Stable
class FdkPagingController internal constructor(
    private val showsRefreshIndicator: Boolean,
) {
    private var items: LazyPagingItems<*>? = null
    private var announceRefresh: (() -> Unit)? = null

    internal fun bind(items: LazyPagingItems<*>, announceRefresh: () -> Unit) {
        this.items = items
        this.announceRefresh = announceRefresh
    }

    internal fun unbind(items: LazyPagingItems<*>) {
        if (this.items === items) {
            this.items = null
            this.announceRefresh = null
        }
    }

    /**
     * Reload the list from the first page. Mirrors [LazyPagingItems.refresh].
     *
     * Raises the container's pull-to-refresh indicator for the duration when the controller was
     * remembered with `showsRefreshIndicator = true`, and nothing else has taken that flag over.
     */
    fun refresh() {
        if (showsRefreshIndicator) announceRefresh?.invoke()
        items?.refresh()
    }

    /** Retry the failed load. Mirrors [LazyPagingItems.retry]. */
    fun retry() { items?.retry() }
}

/**
 * Remembers a [FdkPagingController] for hoisting refresh/retry control out of [PagingContent].
 *
 * @param showsRefreshIndicator whether [FdkPagingController.refresh] raises the container's
 *   pull-to-refresh indicator. Off by default: a reload that the user did not ask for has no business
 *   animating, and loaded items stay on screen throughout either way. Turn it on for a refresh the
 *   user *did* ask for — a toolbar button — which would otherwise look like it did nothing. It has no
 *   effect where the call site owns the gesture through `isRefreshing`, or on a reload the SDK never
 *   sees, such as a [PagingSource][androidx.paging.PagingSource] invalidating itself.
 */
@Composable
fun rememberPagingController(showsRefreshIndicator: Boolean = false): FdkPagingController =
    remember(showsRefreshIndicator) { FdkPagingController(showsRefreshIndicator) }

private class PagingScopeImpl<T : Any>(
    val item: @Composable LazyItemScope.(Int, T) -> Unit,
    val header: (LazyListScope.() -> Unit)?,
    val slots: PagingSlots,
)

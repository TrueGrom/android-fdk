package grmv.android.fdk.screen.paging

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isFinite
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import grmv.android.fdk.screen.LocalContentPaddingDefaults
import kotlinx.coroutines.flow.Flow

/**
 * Renders this [Flow] of [PagingData] as a pull-to-refresh [LazyVerticalGrid], driven by the
 * [content] DSL — [PagingContent]'s twin for content laid out as tiles.
 *
 * Everything but the layout is shared with the list: the same [PagingDefaults] wording the load
 * states, the same per-call slot overrides, the same pull-to-refresh indicator from
 * [LocalRefreshDefaults][grmv.android.fdk.screen.refresh.LocalRefreshDefaults], and the same slot
 * transitions. The load-state slots are emitted as **full-span** items, so a loader or an error
 * covers the grid's width rather than one cell.
 *
 * Unlike [pagingItems], this measures its own viewport, so a slot's `fillParentMaxSize()` really
 * does fill the screen — which is also why a `Prepend` header combined with a full-viewport slot
 * overflows: the header is pushed off screen while that slot is up. Either keep the header out of
 * the loading and empty states, or size those slots to wrap their content.
 *
 * This composable owns its grid. A paged section *inside* a grid the screen already owns — one
 * below a header and a summary card, say — cannot use it: see the `LazyGridScope.pagingItems`
 * extension below, which this is a thin wrapper over.
 *
 * ```
 * itemsFlow.PagingGridContent(
 *     columns = GridCells.Adaptive(minSize = 104.dp),
 *     itemKey = { it.id.toString() },
 *     horizontalArrangement = Arrangement.spacedBy(8.dp),
 * ) {
 *     Item { _, element -> Tile(element, modifier = Modifier.animateItem()) }
 *     Empty { EmptyState(modifier = Modifier.fillParentMaxSize()) }
 * }
 * ```
 *
 * @param columns how the grid splits its width into cells; passed straight to [LazyVerticalGrid].
 * @param itemKey stable key for each item, used for efficient updates and for `animateItem`.
 * @param contentPadding padding around the grid content; defaults to the current
 *   [ContentPaddingDefaults][grmv.android.fdk.screen.ContentPaddingDefaults], and overridable per
 *   call: a tile that carries its own inset subtracts it back out here, so its content still lines
 *   up with the rest of the screen.
 * @param verticalArrangement spacing between rows.
 * @param horizontalArrangement spacing between columns.
 * @param controller optional handle for triggering [refresh][FdkPagingController.refresh]/
 *   [retry][FdkPagingController.retry] programmatically; create it with [rememberPagingController].
 * @param state the grid's scroll position; hoist it to read or drive the scroll from the screen.
 * @param content the slot DSL describing item, load-state and header presentations for this grid.
 */
@Composable
fun <T : Any> Flow<PagingData<T>>.PagingGridContent(
    columns: GridCells,
    itemKey: (T) -> String,
    contentPadding: PaddingValues = LocalContentPaddingDefaults.current.contentPadding,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(12.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(12.dp),
    controller: FdkPagingController? = null,
    state: LazyGridState = rememberLazyGridState(),
    content: FdkPagingGridScopeBuilder<T>.() -> Unit,
) {
    PagedPullToRefresh(controller) { items, isRefreshing ->
        // A lazy grid gives its items no viewport height (they are measured with an infinite main
        // axis), so a full-viewport slot has nothing to fill. Measuring the grid's own box here is
        // what lets `fillParentMaxSize()` in a slot mean the same thing it means in a list.
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val slotViewport = pagingSlotViewport(contentPadding)
            LazyVerticalGrid(
                columns = columns,
                state = state,
                contentPadding = contentPadding,
                verticalArrangement = verticalArrangement,
                horizontalArrangement = horizontalArrangement,
            ) {
                pagingItems(
                    items = items,
                    itemKey = itemKey,
                    isRefreshing = isRefreshing,
                    slotViewport = slotViewport,
                    content = content,
                )
            }
        }
    }
}

/**
 * Emits [items] and their load-state slots into a grid the call site owns.
 *
 * The shape [PagingGridContent] is built on, and the one a paged *section* needs: a screen whose
 * header, summary card and paged content share a single [LazyVerticalGrid] cannot hand that content
 * to a composable owning its own grid, but it can call this from inside the grid it already has.
 * The load states are resolved through the same [PagingDefaults] either way — that is the whole
 * point of the extension existing rather than each such screen branching on `loadState` by hand.
 *
 * ```
 * LazyVerticalGrid(columns = GridCells.Fixed(3), state = gridState) {
 *     item(span = { GridItemSpan(maxLineSpan) }) { Header(summary) }
 *     pagingItems(items, itemKey = { it.id.toString() }) {
 *         Item { _, element -> Tile(element, modifier = Modifier.animateItem()) }
 *     }
 * }
 * ```
 *
 * The load-state slots are emitted full-span; loaded items take one cell each.
 *
 * Pull-to-refresh is the caller's: this emits items, it does not wrap them in a gesture. A screen
 * that pulls to refresh its whole content passes its own flag as [isRefreshing] so the full-viewport
 * loader does not flash on top of the pull indicator.
 *
 * @param items the collected paging items — `flow.collectAsLazyPagingItems()` in the caller, since
 *   this is not a composable.
 * @param itemKey stable key for each item, used for efficient updates and for `animateItem`.
 * @param isRefreshing `true` while the caller's own pull-to-refresh is in flight; suppresses the
 *   full-viewport loading slot for its duration.
 * @param slotViewport the grid's content area (its viewport minus its content padding), which
 *   `fillParentMaxSize()`/`fillParentMaxHeight()` in a slot fill. Unspecified by default: a slot in
 *   a *section* normally wraps its content — a loader sized to the whole viewport would push
 *   whatever sits above it off screen. Pass [pagingSlotViewport] (from a `BoxWithConstraints`
 *   around the grid) when the paged content really is the screen. Width needs no such help: a full-span item is
 *   already measured against the grid's finite width.
 * @param content the slot DSL describing item, load-state and header presentations.
 */
fun <T : Any> LazyGridScope.pagingItems(
    items: LazyPagingItems<T>,
    itemKey: (T) -> String,
    isRefreshing: Boolean = false,
    slotViewport: DpSize = DpSize.Unspecified,
    content: FdkPagingGridScopeBuilder<T>.() -> Unit,
) {
    val scope = PagingGridScopeBuilderImpl<T>().apply(content).build()
    scope.prepend?.invoke(this)
    emitPagingSlots(
        items = items,
        slots = scope.slots,
        isRefreshing = isRefreshing,
        slot = { key, fadeOut, slotContent ->
            item(key = key, span = { GridItemSpan(maxLineSpan) }) {
                PagingSlot(
                    GridPagingSlotScope(this, slotViewport),
                    scope.slots,
                    fadeOut,
                    slotContent,
                )
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
 * The content area of a grid measured by the `BoxWithConstraints` around it, for [pagingItems]'
 * `slotViewport`.
 *
 * ```
 * BoxWithConstraints {
 *     val viewport = pagingSlotViewport(contentPadding)
 *     LazyVerticalGrid(columns = columns, contentPadding = contentPadding) {
 *         pagingItems(items, itemKey = …, slotViewport = viewport) { … }
 *     }
 * }
 * ```
 *
 * An extension on the measuring scope rather than a function taking two `Dp`s: the width and the
 * height would be silently swappable, and a slot sized to the wrong axis is a subtle failure.
 *
 * @param contentPadding the same padding passed to the grid; subtracted out, as a lazy list's own
 *   `fillParentMaxSize` does.
 */
@Composable
fun BoxWithConstraintsScope.pagingSlotViewport(contentPadding: PaddingValues): DpSize {
    val direction = LocalLayoutDirection.current
    return DpSize(
        width = maxWidth.minusPadding(
            contentPadding.calculateStartPadding(direction) +
                contentPadding.calculateEndPadding(direction),
        ),
        height = maxHeight.minusPadding(
            contentPadding.calculateTopPadding() + contentPadding.calculateBottomPadding(),
        ),
    )
}

// An unbounded parent reports an infinite constraint; there is no content area to speak of then, and
// Dp.Unspecified is how FdkPagingSlotScope hears "fall back to wrapping the content".
private fun Dp.minusPadding(padding: Dp): Dp =
    if (isFinite) (this - padding).coerceAtLeast(0.dp) else Dp.Unspecified

/**
 * DSL receiver for configuring the slot composables of a single [PagingGridContent] or [pagingItems]
 * call.
 *
 * The load-state half is [FdkPagingSlotsBuilder], shared verbatim with the list DSL
 * ([FdkPagingScopeBuilder]); only [Item] and [Prepend] differ, since they are the two slots that
 * genuinely speak the layout — a grid item scope is not a list item scope.
 *
 * @param T the item type held in the [androidx.paging.PagingData] stream.
 */
interface FdkPagingGridScopeBuilder<T : Any> : FdkPagingSlotsBuilder {
    /**
     * Content for each loaded item, given its index and value. Occupies one cell, and keeps
     * `LazyGridItemScope` — so `Modifier.animateItem()` is available, and a tile leaving the grid
     * lets the ones behind it close up rather than jumping.
     */
    fun Item(content: @Composable LazyGridItemScope.(Int, T) -> Unit)

    /**
     * Optional header items emitted before the paged content. Receives the grid scope, so a header
     * spanning the full width is `item(span = { GridItemSpan(maxLineSpan) }) { … }`.
     */
    fun Prepend(content: LazyGridScope.() -> Unit)
}

private class PagingGridScopeBuilderImpl<T : Any> :
    PagingSlotsBuilderImpl(), FdkPagingGridScopeBuilder<T> {
    private var item: @Composable LazyGridItemScope.(Int, T) -> Unit = { _, _ -> }
    private var prepend: (LazyGridScope.() -> Unit)? = null

    override fun Item(content: @Composable LazyGridItemScope.(Int, T) -> Unit) { item = content }
    override fun Prepend(content: LazyGridScope.() -> Unit) { prepend = content }

    fun build() = PagingGridScopeImpl(item, prepend, buildSlots())
}

private class PagingGridScopeImpl<T : Any>(
    val item: @Composable LazyGridItemScope.(Int, T) -> Unit,
    val prepend: (LazyGridScope.() -> Unit)?,
    val slots: PagingSlots,
)

package grmv.android.fdk.screen.paging

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.paging.compose.LazyPagingItems

/**
 * Brings the [FdkPagingSlotsBuilder.RefreshError] banner into view when it appears over a list
 * resting at its top.
 *
 * A refresh that fails over loaded items is reported by a banner emitted *ahead of* them. A lazy
 * layout keeps its scroll position anchored to the key of the first visible item, so inserting
 * anything before that item keeps the item where it was — and a banner inserted above the first row
 * of a list at its top lands just above the viewport, where nobody sees it. This moves the list to
 * the banner instead, in the same measure pass that inserts it, so nothing visibly jumps.
 *
 * It acts only when the list was resting at its very top as the banner appeared: a list scrolled
 * into its content is left exactly where the user put it. It acts once per appearance — not again
 * while the same failure stays up, and not on a reload that settles without failing.
 *
 * [PagingContent] already applies this to the list it owns. Call it beside [pagingItems] in a list
 * the screen owns, with the same state and items:
 *
 * ```
 * val listState = rememberLazyListState()
 * KeepRefreshErrorInView(listState, items)
 * LazyColumn(state = listState) {
 *     pagingItems(items, itemKey = { it.id.toString() }) { Item { _, element -> Row(element) } }
 * }
 * ```
 *
 * With a header above the paged items it is harmless, and has nothing to do while the header is on
 * screen: the header is the anchor then, and the banner is inserted below it.
 *
 * @param state the scroll state of the list the banner is emitted into.
 * @param items the same collected items passed to [pagingItems].
 */
@Composable
fun KeepRefreshErrorInView(state: LazyListState, items: LazyPagingItems<*>) {
    KeepRefreshErrorInView(
        items = items,
        isAtTop = { state.firstVisibleItemIndex == 0 && state.firstVisibleItemScrollOffset == 0 },
        scrollToTop = { state.requestScrollToItem(0) },
    )
}

/**
 * [KeepRefreshErrorInView] for a grid: [PagingGridContent] applies it to the grid it owns, and a
 * grid the screen owns calls it beside `LazyGridScope.pagingItems`.
 *
 * @param state the scroll state of the grid the banner is emitted into.
 * @param items the same collected items passed to `pagingItems`.
 */
@Composable
fun KeepRefreshErrorInView(state: LazyGridState, items: LazyPagingItems<*>) {
    KeepRefreshErrorInView(
        items = items,
        isAtTop = { state.firstVisibleItemIndex == 0 && state.firstVisibleItemScrollOffset == 0 },
        scrollToTop = { state.requestScrollToItem(0) },
    )
}

@Composable
private fun KeepRefreshErrorInView(
    items: LazyPagingItems<*>,
    isAtTop: () -> Boolean,
    scrollToTop: () -> Unit,
) {
    // Read in composition, so this recomposes when the banner comes or goes.
    val bannerShown = items.showsRefreshErrorBanner()
    val tracker = remember { AppearanceTracker() }
    // A side effect runs after the composition that saw the failure and before the layout measures
    // the inserted banner, so the scroll position read here is still the pre-insertion one. It is
    // also read here rather than in composition, which would recompose on every scrolled pixel.
    // Should a measure ever slip in between, the first row is no longer at index 0 and this stays
    // put — the banner is then missed, but the list is never moved against the user.
    SideEffect {
        if (tracker.appeared(bannerShown) && isAtTop()) scrollToTop()
    }
}

/**
 * Edge detector over a boolean fed once per composition: [appeared] is `true` only for the call
 * where the value turns from `false` to `true`.
 */
private class AppearanceTracker {
    private var shown = false

    fun appeared(shown: Boolean): Boolean {
        val appeared = shown && !this.shown
        this.shown = shown
        return appeared
    }
}

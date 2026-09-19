package grmv.android.fdk.screen.paging

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.paging.compose.LazyPagingItems

/**
 * Keeps the list steady around the [FdkPagingSlotsBuilder.RefreshError] banner: brings it into view
 * when it appears over a list resting at its top, and keeps the first row from sliding under the
 * list's top edge when it goes away while partly scrolled out.
 *
 * A refresh that fails over loaded items is reported by a banner emitted *ahead of* them. A lazy
 * layout keeps its scroll position anchored to the key of the first visible item, so inserting
 * anything before that item keeps the item where it was — and a banner inserted above the first row
 * of a list at its top lands just above the viewport, where nobody sees it. This moves the list to
 * the banner instead, in the same measure pass that inserts it, so nothing visibly jumps.
 *
 * Removal has the opposite problem. While the banner is the first visible item it is the anchor, and
 * once it is gone the layout falls back to its index and keeps the offset — so the row that followed
 * it takes its place shifted up by however much of the banner was scrolled out. This puts that row
 * at the top instead, in the pass that removes the banner. When it was partly scrolled out, the banner
 * then goes at once rather than fading: the jump resets the layout's item animations.
 *
 * It acts only when the list was resting at its very top as the banner appeared — or, after this put
 * a row in the banner's place, still resting on that row, so a retry that fails again shows its
 * banner below a header too — or when the banner was the first visible item as it went away: a list
 * scrolled into its content is left exactly where the user put it. It acts once per change — not
 * again while the same failure stays up, and not on a reload that settles without failing. Passing
 * other [items] starts over, so a pager swapped in with a failure of its own is an appearance too.
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
 * screen: the header is the anchor then, and the banner is inserted below it and removed from below
 * it.
 *
 * @param state the scroll state of the list the banner is emitted into.
 * @param items the same collected items passed to [pagingItems].
 */
@Composable
fun KeepRefreshErrorInView(state: LazyListState, items: LazyPagingItems<*>) {
    KeepRefreshErrorInView(
        items = items,
        firstVisibleItem = {
            val index = state.firstVisibleItemIndex
            FirstVisibleItem(
                index = index,
                offset = state.firstVisibleItemScrollOffset,
                key = state.layoutInfo.visibleItemsInfo.find { it.index == index }?.key,
            )
        },
        scrollToItem = { index -> state.requestScrollToItem(index) },
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
        firstVisibleItem = {
            val index = state.firstVisibleItemIndex
            FirstVisibleItem(
                index = index,
                offset = state.firstVisibleItemScrollOffset,
                key = state.layoutInfo.visibleItemsInfo.find { it.index == index }?.key,
            )
        },
        scrollToItem = { index -> state.requestScrollToItem(index) },
    )
}

@Composable
private fun KeepRefreshErrorInView(
    items: LazyPagingItems<*>,
    firstVisibleItem: () -> FirstVisibleItem,
    scrollToItem: (index: Int) -> Unit,
) {
    // Read in composition, so this recomposes when the banner comes or goes.
    val bannerShown = items.showsRefreshErrorBanner()
    // Keyed on the items: a swapped-in pager must not inherit the previous one's banner.
    val tracker = remember(items) { BannerTracker() }
    // A side effect runs after the composition that saw the change and before the layout measures
    // it, so the scroll position read here is still the one with the banner inserted or not yet
    // removed. It is also read here rather than in composition, which would recompose on every
    // scrolled pixel. Should a measure ever slip in between, the conditions below no longer hold and
    // this stays put — the banner is then missed, but the list is never moved against the user.
    SideEffect {
        if (!tracker.changed(bannerShown)) return@SideEffect
        val first = firstVisibleItem()
        if (bannerShown) {
            val resting = first.index == 0 || first.index == tracker.bannerIndex
            if (resting && first.offset == 0) scrollToItem(first.index)
        } else if (first.key == REFRESH_ERROR_SLOT_KEY) {
            // Matched by key, not by index 0: with a header above the paged items the banner sits
            // further down. Once it is removed, its index is the row that followed it — and where
            // the next banner is inserted, should the retry fail again.
            tracker.bannerIndex = first.index
            scrollToItem(first.index)
        }
    }
}

/** The first visible item of a lazy layout: its index, scroll offset and key. */
private class FirstVisibleItem(val index: Int, val offset: Int, val key: Any?)

/**
 * Edge detector over a boolean fed once per composition: [changed] is `true` only for the call where
 * the value differs from the previous one, which starts as `false`.
 */
private class BannerTracker {
    private var shown = false

    /** Index of the last banner removed while it was the first visible item; `0` until then. */
    var bannerIndex = 0

    fun changed(shown: Boolean): Boolean {
        val changed = shown != this.shown
        this.shown = shown
        return changed
    }
}

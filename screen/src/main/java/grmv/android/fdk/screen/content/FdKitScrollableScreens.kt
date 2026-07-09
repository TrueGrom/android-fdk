package grmv.android.fdk.screen.content

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.Vertical
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import grmv.android.fdk.screen.LocalContentPaddingDefaults
import grmv.android.fdk.screen.ScaffoldScope
import grmv.android.fdk.screen.ContentPaddingDefaults


/**
 * A full-size vertically scrolling [Column] scoped to a [ScaffoldScope].
 *
 * All children are composed upfront — prefer [FdKitLazyScreen] when the item count is large
 * or unknown. The column fills its parent, enables vertical scroll via [scrollState], and applies
 * [contentPadding] as inner padding so scroll stops at the content edge, not the container edge.
 *
 * @param modifier Applied to the column before fill, scroll, and padding modifiers.
 * @param scrollState Scroll position state; hoist it to animate scroll or react to position changes.
 * @param contentPadding Inner padding; defaults to the current [ContentPaddingDefaults].
 * @param content Column body, receives [ColumnScope].
 */
@Composable
fun ScaffoldScope.FdKitScrollableScreen(
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    contentPadding: PaddingValues = LocalContentPaddingDefaults.current.contentPadding,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(contentPadding),
        content = content,
    )
}

/**
 * A full-size [LazyColumn] scoped to a [ScaffoldScope], for lazily-composed scrolling bodies.
 *
 * Items are composed on demand as they scroll into view. Prefer this over [FdKitScrollableScreen] when
 * the list is long or dynamically sized. [contentPadding] is passed as `LazyColumn.contentPadding`
 * so the first/last items are inset while the scroll range extends to the container edges.
 *
 * @param lazyListState Scroll and item position state; hoist to programmatically scroll or observe.
 * @param contentPadding Padding applied around the list content area; defaults to the current [ContentPaddingDefaults].
 * @param verticalArrangement Spacing between items; defaults to [Arrangement.Top].
 * @param content Lazy list body via [LazyListScope].
 */
@Composable
fun ScaffoldScope.FdKitLazyScreen(
    lazyListState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = LocalContentPaddingDefaults.current.contentPadding,
    verticalArrangement: Vertical = Arrangement.Top,
    content: LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = lazyListState,
        contentPadding = contentPadding,
        verticalArrangement = verticalArrangement,
        content = content,
    )
}

package grmv.android.fdk.screen.refresh

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.Vertical
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import grmv.android.fdk.screen.ContentPaddingDefaults
import grmv.android.fdk.screen.LocalContentPaddingDefaults
import grmv.android.fdk.state.RefreshOwner

/**
 * Shared pull-to-refresh wrapper: Material3 [PullToRefreshBox] with the indicator supplied by the
 * current [RefreshDefaults].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val state = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        state = state,
        indicator = { with(LocalRefreshDefaults.current) { Indicator(isRefreshing, state) } },
        content = content,
    )
}

/**
 * A pull-to-refresh [Box] for a single scrolling child or a short body.
 *
 * The pull gesture requires a scrollable child; with [isScrollable] (default) the box itself
 * scrolls vertically via [scrollState] — disable it when [content] hosts its own scrollable.
 * Prefer [FdKitRefreshColumn] / [FdKitRefreshLazyColumn] for list bodies.
 *
 * This primitive overload serves screens that manage the refreshing flag themselves (e.g. inside
 * their state); ViewModels mixing in [RefreshOwner] use the receiver overload instead.
 *
 * @param isRefreshing whether the refresh indicator is shown.
 * @param onRefresh invoked when the user pulls past the threshold.
 * @param modifier applied to the refresh container.
 * @param isScrollable whether the box scrolls vertically itself.
 * @param scrollState scroll position state used when [isScrollable] is true.
 * @param content box body, receives [BoxScope].
 */
@Composable
fun FdKitRefreshContainer(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    isScrollable: Boolean = true,
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable BoxScope.() -> Unit,
) {
    RefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh, modifier = modifier) {
        Box(
            modifier = if (isScrollable) {
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            } else {
                Modifier.fillMaxSize()
            },
            content = content,
        )
    }
}

/**
 * [FdKitRefreshContainer] bound to a [RefreshOwner]: collects [RefreshOwner.refreshing]
 * lifecycle-aware and triggers [RefreshOwner.refresh] on pull.
 *
 * @see RefreshOwner
 */
@Composable
fun RefreshOwner.FdKitRefreshContainer(
    modifier: Modifier = Modifier,
    isScrollable: Boolean = true,
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable BoxScope.() -> Unit,
) {
    val isRefreshing by refreshing.collectAsStateWithLifecycle()
    FdKitRefreshContainer(
        isRefreshing = isRefreshing,
        onRefresh = { refresh() },
        modifier = modifier,
        isScrollable = isScrollable,
        scrollState = scrollState,
        content = content,
    )
}

/**
 * A pull-to-refresh vertically scrolling [Column].
 *
 * All children are composed upfront — prefer [FdKitRefreshLazyColumn] when the item count is large
 * or unknown. [contentPadding] is applied as inner padding so scroll stops at the content edge.
 *
 * @param isRefreshing whether the refresh indicator is shown.
 * @param onRefresh invoked when the user pulls past the threshold.
 * @param modifier applied to the refresh container.
 * @param scrollState scroll position state; hoist it to animate scroll or react to position changes.
 * @param contentPadding inner padding; defaults to the current [ContentPaddingDefaults].
 * @param content column body, receives [ColumnScope].
 */
@Composable
fun FdKitRefreshColumn(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    contentPadding: PaddingValues = LocalContentPaddingDefaults.current.contentPadding,
    content: @Composable ColumnScope.() -> Unit,
) {
    RefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(contentPadding),
            content = content,
        )
    }
}

/**
 * [FdKitRefreshColumn] bound to a [RefreshOwner]: collects [RefreshOwner.refreshing]
 * lifecycle-aware and triggers [RefreshOwner.refresh] on pull.
 *
 * @see RefreshOwner
 */
@Composable
fun RefreshOwner.FdKitRefreshColumn(
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    contentPadding: PaddingValues = LocalContentPaddingDefaults.current.contentPadding,
    content: @Composable ColumnScope.() -> Unit,
) {
    val isRefreshing by refreshing.collectAsStateWithLifecycle()
    FdKitRefreshColumn(
        isRefreshing = isRefreshing,
        onRefresh = { refresh() },
        modifier = modifier,
        scrollState = scrollState,
        contentPadding = contentPadding,
        content = content,
    )
}

/**
 * A pull-to-refresh [LazyColumn] for lazily-composed list bodies.
 *
 * [contentPadding] is passed as `LazyColumn.contentPadding` so the first/last items are inset while
 * the scroll range extends to the container edges.
 *
 * @param isRefreshing whether the refresh indicator is shown.
 * @param onRefresh invoked when the user pulls past the threshold.
 * @param modifier applied to the refresh container.
 * @param lazyListState scroll and item position state; hoist to programmatically scroll or observe.
 * @param contentPadding padding around the list content area; defaults to the current [ContentPaddingDefaults].
 * @param verticalArrangement spacing between items; defaults to [Arrangement.Top].
 * @param content lazy list body via [LazyListScope].
 */
@Composable
fun FdKitRefreshLazyColumn(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = LocalContentPaddingDefaults.current.contentPadding,
    verticalArrangement: Vertical = Arrangement.Top,
    content: LazyListScope.() -> Unit,
) {
    RefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh, modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = lazyListState,
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement,
            content = content,
        )
    }
}

/**
 * [FdKitRefreshLazyColumn] bound to a [RefreshOwner]: collects [RefreshOwner.refreshing]
 * lifecycle-aware and triggers [RefreshOwner.refresh] on pull.
 *
 * @see RefreshOwner
 */
@Composable
fun RefreshOwner.FdKitRefreshLazyColumn(
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = LocalContentPaddingDefaults.current.contentPadding,
    verticalArrangement: Vertical = Arrangement.Top,
    content: LazyListScope.() -> Unit,
) {
    val isRefreshing by refreshing.collectAsStateWithLifecycle()
    FdKitRefreshLazyColumn(
        isRefreshing = isRefreshing,
        onRefresh = { refresh() },
        modifier = modifier,
        lazyListState = lazyListState,
        contentPadding = contentPadding,
        verticalArrangement = verticalArrangement,
        content = content,
    )
}

package grmv.android.fdk.screen.paging

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.paging.LoadState.Error
import androidx.paging.LoadState.Loading
import androidx.paging.LoadState.NotLoading
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import grmv.android.fdk.screen.refresh.LocalRefreshDefaults
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

/**
 * How long [EndRefreshOnLoadingEvent] waits for the requested reload to reach [Loading] before
 * giving up on ever seeing it. Only the start of the load is bounded — once it begins, the wait for
 * it to finish is open-ended.
 */
private val LOADING_START_TIMEOUT = 1.seconds

/** Invokes [block] when the append load state is [Loading]. */
internal inline fun <T : Any> LazyPagingItems<T>.ifAppendPageLoading(block: () -> Unit) {
    if (loadState.append is Loading) {
        block.invoke()
    }
}

/** Invokes [block] when the prepend load state is [Loading]. */
internal inline fun <T : Any> LazyPagingItems<T>.ifPrependPageLoading(block: () -> Unit) {
    if (loadState.prepend is Loading) {
        block.invoke()
    }
}

/** Invokes [block] when the refresh load state is [Error]. */
internal inline fun <T : Any> LazyPagingItems<T>.ifRefreshError(block: () -> Unit) {
    if (loadState.refresh is Error) {
        block.invoke()
    }
}

/**
 * Invokes [block] when the append load state is [Error] and [unless] is `false`.
 *
 * @param unless suppresses the footer when the failure is already being reported elsewhere.
 */
internal inline fun <T : Any> LazyPagingItems<T>.ifAppendPageError(
    unless: Boolean = false,
    block: () -> Unit,
) {
    if (!unless && loadState.append is Error) {
        block.invoke()
    }
}

/**
 * The throwable of the current refresh [Error], or `null` when the refresh is not failed.
 *
 * Read from inside the slot's composition rather than captured when the slot is emitted, so a second
 * failure of a different kind is worded as itself instead of keeping the first one's message.
 */
internal fun <T : Any> LazyPagingItems<T>.refreshErrorOrNull(): Throwable? =
    (loadState.refresh as? Error)?.error

/**
 * Invokes [block] when the prepend load state is [Error] and [unless] is `false`.
 *
 * @param unless suppresses the header when the failure is already being reported elsewhere.
 */
internal inline fun <T : Any> LazyPagingItems<T>.ifPrependPageError(
    unless: Boolean = false,
    block: () -> Unit,
) {
    if (!unless && loadState.prepend is Error) {
        block.invoke()
    }
}

/** The throwable of the current append [Error], or `null` when the append is not failed. */
internal fun <T : Any> LazyPagingItems<T>.appendErrorOrNull(): Throwable? =
    (loadState.append as? Error)?.error

/** The throwable of the current prepend [Error], or `null` when the prepend is not failed. */
internal fun <T : Any> LazyPagingItems<T>.prependErrorOrNull(): Throwable? =
    (loadState.prepend as? Error)?.error

/** Invokes [block] when the item count is zero and all load states are [NotLoading]. */
internal inline fun <T : Any> LazyPagingItems<T>.ifEmpty(block: () -> Unit) {
    if (itemCount == 0 &&
        loadState.refresh is NotLoading &&
        loadState.append is NotLoading &&
        loadState.prepend is NotLoading
    ) {
        block.invoke()
    }
}

/**
 * Waits for [items] to transition through [Loading] and back to [NotLoading] or [Error], then
 * calls [endRefresh]. No-ops when [isRefreshing] is `false`.
 *
 * A reload that resolves before this collector starts never emits the [Loading] it waits for, so the
 * wait is bounded — [endRefresh] still runs and the refreshing flag cannot stay raised for the rest
 * of the composition.
 */
@Composable
internal fun <T : Any> EndRefreshOnLoadingEvent(
    isRefreshing: Boolean,
    items: LazyPagingItems<T>,
    endRefresh: () -> Unit,
) {
    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) return@LaunchedEffect
        val started = withTimeoutOrNull(LOADING_START_TIMEOUT) {
            snapshotFlow { items.loadState.refresh }.first { it is Loading }
        }
        if (started != null) {
            snapshotFlow { items.loadState.refresh }.first { it is NotLoading || it is Error }
        }
        endRefresh()
    }
}

/**
 * Dispatches to [loading], [error], or [ready] based on the refresh load state — but only while
 * there is nothing to show.
 *
 * **Loaded items always win.** A [PagingSource][androidx.paging.PagingSource] over a local store is
 * invalidated by writes the screen never asked about, and every invalidation drives refresh back
 * through [Loading]; a programmatic [LazyPagingItems.refresh] does the same. Branching on the refresh
 * state alone would tear the loaded content off the screen each time and put a full-viewport loader
 * in its place. So [loading] and [error] are the *empty-state* presentations, and a refresh that
 * fails over loaded items is reported by the banner [ready] emits instead
 * (`FdkPagingSlotsBuilder.RefreshError`).
 *
 * [loading] is additionally skipped while a pull-to-refresh is in progress ([isRefreshing] `true`),
 * which covers the first load: a pull on a still-empty list must not flash a spinner on top of the
 * pull indicator.
 */
internal inline fun <T : Any> LazyPagingItems<T>.expand(
    isRefreshing: Boolean,
    loading: () -> Unit,
    error: () -> Unit,
    ready: () -> Unit,
) {
    when {
        itemCount > 0 -> ready.invoke()
        loadState.refresh is Error -> error.invoke()
        loadState.refresh is Loading && !isRefreshing -> loading.invoke()
        else -> ready.invoke()
    }
}

/**
 * Collects this [Flow] of [PagingData] and wraps [content] in the shared pull-to-refresh container.
 *
 * The one copy of the refresh handshake behind both [PagingContent] and [PagingGridContent]:
 * collecting the items, binding an optional [controller], raising the refreshing flag on a pull and
 * lowering it again through [EndRefreshOnLoadingEvent], and drawing the indicator from the current
 * [LocalRefreshDefaults] so a paged list and a paged grid pull the same way. Only the layout inside
 * differs between the two.
 *
 * Both halves of the gesture can be taken over by the caller — see the container KDoc for when that
 * is worth doing. Which side owns it is fixed for the life of this composition: the two flags have no
 * shared memory of a load being in flight, so handing ownership over mid-load would drop the
 * indicator on a running reload, or raise it on nothing.
 *
 * @param controller optional handle bound to the collected items for the life of this composition.
 * @param isRefreshing caller-owned refreshing flag, or `null` to keep the built-in one.
 * @param onRefresh caller-owned pull handler, or `null` to keep the built-in [LazyPagingItems.refresh].
 * @param content the layout, given the collected items and whether a pull-to-refresh is in flight.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T : Any> Flow<PagingData<T>>.PagedPullToRefresh(
    controller: FdkPagingController?,
    isRefreshing: Boolean?,
    onRefresh: (() -> Unit)?,
    content: @Composable BoxScope.(items: LazyPagingItems<T>, isRefreshing: Boolean) -> Unit,
) {
    // Half a hoisted gesture is a silent one: the pull would run the built-in reload while the flag
    // it is supposed to follow never rises, so the indicator retracts on the spot.
    require(isRefreshing == null || onRefresh != null) {
        "isRefreshing without onRefresh: pass both to own the pull gesture, or neither."
    }
    val items = collectAsLazyPagingItems()
    var selfRefreshing by rememberSaveable { mutableStateOf(false) }
    val refreshing = isRefreshing ?: selfRefreshing
    // Read at call time, not at bind time: the controller outlives any one recomposition.
    val ownsFlag by rememberUpdatedState(isRefreshing == null)
    controller?.let { c ->
        DisposableEffect(c, items) {
            c.bind(items) { if (ownsFlag) selfRefreshing = true }
            onDispose { c.unbind(items) }
        }
    }
    val refreshState = rememberPullToRefreshState()
    // Only the flag we own is ours to lower; a caller-supplied one is lowered by whoever raised it.
    // This gate is also why ownership may not change mid-load: it would stop watching a live one.
    EndRefreshOnLoadingEvent(isRefreshing == null && selfRefreshing, items) {
        selfRefreshing = false
    }

    PullToRefreshBox(
        isRefreshing = refreshing,
        state = refreshState,
        indicator = { with(LocalRefreshDefaults.current) { Indicator(refreshing, refreshState) } },
        onRefresh = {
            if (isRefreshing == null) selfRefreshing = true
            if (onRefresh != null) onRefresh() else items.refresh()
        },
    ) {
        content(items, refreshing)
    }
}

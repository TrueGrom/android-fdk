package grmv.android.fdk.screen.paging

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.paging.LoadState.Error
import androidx.paging.LoadState.Loading
import androidx.paging.LoadState.NotLoading
import androidx.paging.compose.LazyPagingItems
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
internal inline fun <T : Any> LazyPagingItems<T>.ifPageLoading(block: () -> Unit) {
    if (loadState.append is Loading) {
        block.invoke()
    }
}

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

/** `true` when any of refresh, append, or prepend load states is [Loading]. */
internal fun <T : Any> LazyPagingItems<T>.isLoading(): Boolean {
    return loadState.refresh is Loading ||
        loadState.append is Loading ||
        loadState.prepend is Loading
}

/** Invokes [block] when the refresh load state is [Error]. */
internal inline fun <T : Any> LazyPagingItems<T>.ifError(block: () -> Unit) {
    if (loadState.refresh is Error) {
        block.invoke()
    }
}

/** Invokes [block] when the refresh load state is [Error]. */
internal inline fun <T : Any> LazyPagingItems<T>.ifRefreshError(block: () -> Unit) {
    if (loadState.refresh is Error) {
        block.invoke()
    }
}

/** Invokes [block] when the append load state is [Error]. */
internal inline fun <T : Any> LazyPagingItems<T>.ifAppendPageError(block: () -> Unit) {
    if (loadState.append is Error) {
        block.invoke()
    }
}

/** Invokes [block] when the prepend load state is [Error]. */
internal inline fun <T : Any> LazyPagingItems<T>.ifPrependPageError(block: () -> Unit) {
    if (loadState.prepend is Error) {
        block.invoke()
    }
}

/** `true` when no items have loaded yet and the refresh state is [Loading]. */
internal fun <T : Any> LazyPagingItems<T>.isInitialLoading(): Boolean {
    return itemCount == 0 && loadState.refresh is Loading
}

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
 * Dispatches to [loading], [error], or [ready] based on the refresh load state.
 * [loading] is skipped while a pull-to-refresh is in progress ([isRefreshing] `true`) so
 * the full-screen spinner does not flash on top of the pull indicator.
 */
internal inline fun <T : Any> LazyPagingItems<T>.expand(
    isRefreshing: Boolean,
    loading: () -> Unit,
    error: () -> Unit,
    ready: () -> Unit,
) {
    when (loadState.refresh) {
        is Error -> error.invoke()
        is Loading if !isRefreshing -> loading.invoke()
        else -> ready.invoke()
    }
}

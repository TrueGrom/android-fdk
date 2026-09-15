package grmv.android.fdk.screen.paging

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import grmv.android.fdk.screen.content.LocalLoadingDefaults

/**
 * App-wide default presentations for the load-state slots of paged content.
 *
 * One instance serves every layout: [PagingContent] (list), [PagingGridContent] (grid) and the
 * `pagingItems` extensions (a list or grid the call site owns) all resolve their slots here, so the same failure is
 * worded the same way whichever one renders it. That is why the slots hang off
 * [FdkPagingSlotScope] rather than off `LazyItemScope`, which a grid item cannot provide.
 *
 * Precedence: per-call DSL slot > [LocalPagingDefaults] > [Material3PagingDefaults].
 *
 * Provide a custom instance once via [ProvidePagingDefaults] (or through
 * [grmv.android.fdk.screen.FdkScreenDefaults]) to skin every paged list and grid with your design
 * system's loaders. Individual call sites still override any slot through the [FdkPagingScopeBuilder]
 * / [FdkPagingGridScopeBuilder] DSL; an unset slot falls back to the current [PagingDefaults].
 *
 * Note: this type is `@Immutable` — implementations MUST be truly immutable, or recomposition may
 * be skipped.
 */
@Immutable
interface PagingDefaults {
    /** Initial loading, filling the viewport where one is known (see [FdkPagingSlotScope]). */
    @Composable
    fun FdkPagingSlotScope.Loading()

    /**
     * Refresh error with a [retry] action, filling the viewport where one is known.
     *
     * [e] is the failure the refresh ended on. Word it through
     * [ErrorEffectsDefaults.errorMessage][grmv.android.fdk.screen.error.ErrorEffectsDefaults] — the
     * mapper behind the error dialogs and snackbars — and word
     * [LoadingDefaults.Error][grmv.android.fdk.screen.content.LoadingDefaults] from the same place,
     * so that one failure reads the same on a paged screen and on the screen next to it.
     *
     * [retry] takes no argument, unlike the non-paged
     * [LoadingDefaults][grmv.android.fdk.screen.content.LoadingDefaults] slot's: paging retries through
     * `LazyPagingItems.retry()`, which needs nothing from the caller.
     */
    @Composable
    fun FdkPagingSlotScope.Error(e: Throwable, retry: () -> Unit)

    /** Append (next page) loading footer. */
    @Composable
    fun FdkPagingSlotScope.AppendLoading()

    /** Append (next page) error footer for [e], with a [retry] action. Worded like [Error]'s. */
    @Composable
    fun FdkPagingSlotScope.AppendError(e: Throwable, retry: () -> Unit)
}

/**
 * Material3 fallback used until a consumer provides its own [PagingDefaults].
 *
 * Every slot delegates to the shared [LocalLoadingDefaults] — the loaders *and* the error
 * presentation — so an app that supplies only a
 * [LoadingDefaults][grmv.android.fdk.screen.content.LoadingDefaults] already words a paged failure
 * exactly as it words a non-paged one. This fallback contributes the layout (full-viewport for the
 * refresh states, a padded footer for the append ones) and nothing else.
 */
internal object Material3PagingDefaults : PagingDefaults {
    @Composable
    override fun FdkPagingSlotScope.Loading() {
        Box(
            modifier = Modifier.fillParentMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            with(LocalLoadingDefaults.current) { Loading() }
        }
    }

    @Composable
    override fun FdkPagingSlotScope.Error(e: Throwable, retry: () -> Unit) {
        Box(
            modifier = Modifier.fillParentMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            with(LocalLoadingDefaults.current) { Error(e) { retry() } }
        }
    }

    @Composable
    override fun FdkPagingSlotScope.AppendLoading() {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            with(LocalLoadingDefaults.current) { Loading() }
        }
    }

    @Composable
    override fun FdkPagingSlotScope.AppendError(e: Throwable, retry: () -> Unit) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            with(LocalLoadingDefaults.current) { Error(e) { retry() } }
        }
    }
}

/** App-wide [PagingDefaults] for paged lists and grids; defaults to [Material3PagingDefaults]. */
val LocalPagingDefaults = staticCompositionLocalOf<PagingDefaults> { Material3PagingDefaults }

/** Sets the app-wide paging load-state [defaults] for [content]. */
@Composable
fun ProvidePagingDefaults(defaults: PagingDefaults, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalPagingDefaults provides defaults, content = content)
}

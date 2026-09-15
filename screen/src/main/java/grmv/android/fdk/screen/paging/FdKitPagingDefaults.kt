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
     * Refresh error for the empty layout, with a [retry] action, filling the viewport where one is
     * known.
     *
     * Named for *when* it renders, not for what failed: both this and [RefreshError] are driven by
     * the same failed refresh, and only the presence of loaded items tells them apart. This one is
     * shown while there is nothing else to show — the first load that failed, or a refresh that
     * emptied the layout. Once items are on screen they stay there, and [RefreshError] reports the
     * failure over them.
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
    fun FdkPagingSlotScope.EmptyError(e: Throwable, retry: () -> Unit)

    /**
     * Refresh error shown **above loaded items**, with a [retry] action.
     *
     * The counterpart of [EmptyError] for a populated layout: loaded content is never torn off
     * the screen by a failed refresh, so the failure is reported as a banner emitted first — where
     * it is actually seen, unlike a footer under a long list. It wraps its content rather than filling the
     * viewport, and in a grid it spans the full width.
     *
     * Defaults to [AppendError], whose shape it shares — a message with a retry action, sized to its
     * content — so an app that has already skinned its append failures gets a matching banner for
     * free. Override it to distinguish the two.
     *
     * [e] is the failure the refresh ended on, worded like [EmptyError]'s. [retry] dismisses the
     * banner — on the configured transition, like any other slot — and leaves the loaded items in
     * place. The reload it starts is *not* announced: the only way to announce it over loaded
     * content would be a loader on every refresh, which is exactly what this slot exists to avoid.
     * The banner comes back if the reload fails again.
     */
    @Composable
    fun FdkPagingSlotScope.RefreshError(e: Throwable, retry: () -> Unit) {
        AppendError(e, retry)
    }

    /**
     * Previous-page loading header, above the loaded items.
     *
     * Only a [PagingSource][androidx.paging.PagingSource] that can page backwards ever reaches this
     * — one entered at an anchor in the middle of the data rather than at its start. Defaults to
     * [AppendLoading]: the same indicator, at the other end.
     */
    @Composable
    fun FdkPagingSlotScope.PrependLoading() {
        AppendLoading()
    }

    /**
     * Previous-page error header for [e], with a [retry] action. Defaults to [AppendError], and
     * reached under the same conditions as [PrependLoading].
     */
    @Composable
    fun FdkPagingSlotScope.PrependError(e: Throwable, retry: () -> Unit) {
        AppendError(e, retry)
    }

    /** Append (next page) loading footer. */
    @Composable
    fun FdkPagingSlotScope.AppendLoading()

    /** Append (next page) error footer for [e], with a [retry] action. Worded like [EmptyError]'s. */
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
 * empty-state refresh slots, a padded band for the append ones and for
 * [RefreshError][PagingDefaults.RefreshError], which keeps its inherited default here) and nothing
 * else.
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
    override fun FdkPagingSlotScope.EmptyError(e: Throwable, retry: () -> Unit) {
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

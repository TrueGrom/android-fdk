package grmv.android.fdk.screen.paging

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import grmv.android.fdk.screen.R
import grmv.android.fdk.screen.content.FdKitErrorContent
import grmv.android.fdk.screen.content.LocalLoadingDefaults

/**
 * App-wide default presentations for [PagingContent]'s load-state slots.
 *
 * Provide a custom instance once via [ProvidePagingDefaults] to skin every paged list with your
 * design system's loaders. Individual call sites still override any slot through the
 * [PagingScopeBuilder] DSL; an unset slot falls back to the current [PagingDefaults].
 */
@Immutable
interface PagingDefaults {
    /** Full-list initial loading. */
    @Composable
    fun LazyItemScope.Loading()

    /** Full-list refresh error with a [retry] action. */
    @Composable
    fun LazyItemScope.Error(retry: () -> Unit)

    /** Append (next page) loading footer. */
    @Composable
    fun LazyItemScope.AppendLoading()

    /** Append (next page) error footer with a [retry] action. */
    @Composable
    fun LazyItemScope.AppendError(retry: () -> Unit)
}

/** Material3 fallback used until a consumer provides its own [PagingDefaults]. */
internal object Material3PagingDefaults : PagingDefaults {
    @Composable
    override fun LazyItemScope.Loading() {
        Box(
            modifier = Modifier.fillParentMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            with(LocalLoadingDefaults.current) { Loading() }
        }
    }

    @Composable
    override fun LazyItemScope.Error(retry: () -> Unit) {
        FdKitErrorContent(
            message = stringResource(R.string.fdk_error_something_went_wrong),
            onRetry = retry,
            modifier = Modifier.fillParentMaxSize(),
        )
    }

    @Composable
    override fun LazyItemScope.AppendLoading() {
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
    override fun LazyItemScope.AppendError(retry: () -> Unit) {
        FdKitErrorContent(
            message = stringResource(R.string.fdk_error_something_went_wrong),
            onRetry = retry,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
    }
}

/** App-wide [PagingDefaults] for [PagingContent]; defaults to [Material3PagingDefaults]. */
val LocalPagingDefaults = staticCompositionLocalOf<PagingDefaults> { Material3PagingDefaults }

/** Sets the app-wide [PagingContent] load-state [defaults] for [content]. */
@Composable
fun ProvidePagingDefaults(defaults: PagingDefaults, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalPagingDefaults provides defaults, content = content)
}

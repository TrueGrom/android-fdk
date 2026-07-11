package grmv.android.fdk.screen.refresh

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import grmv.android.fdk.screen.FdkScreenDefaults

/**
 * App-wide default presentation for the pull-to-refresh indicator.
 *
 * Consumed by every `FdKitRefresh*` container ([FdKitRefreshContainer], [FdKitRefreshColumn],
 * [FdKitRefreshLazyColumn]). Provide a custom instance once high in the composition via
 * [ProvideRefreshDefaults] (or through [FdkScreenDefaults]) to skin the indicator with your design
 * system.
 *
 * Precedence: [LocalRefreshDefaults] > [Material3RefreshDefaults].
 *
 * Note: this type is `@Immutable` — implementations MUST be truly immutable, or recomposition may be skipped.
 */
@Immutable
@OptIn(ExperimentalMaterial3Api::class)
interface RefreshDefaults {
    /**
     * Pull indicator drawn over the refreshable content; align yourself within the [BoxScope]
     * (typically [Alignment.TopCenter]).
     *
     * @param isRefreshing whether refresh work is in flight.
     * @param state pull gesture state driving the indicator's position and progress.
     */
    @Composable
    fun BoxScope.Indicator(isRefreshing: Boolean, state: PullToRefreshState)
}

/** Material3 fallback used until a consumer provides its own [RefreshDefaults]. */
@OptIn(ExperimentalMaterial3Api::class)
internal object Material3RefreshDefaults : RefreshDefaults {
    @Composable
    override fun BoxScope.Indicator(isRefreshing: Boolean, state: PullToRefreshState) {
        PullToRefreshDefaults.Indicator(
            isRefreshing = isRefreshing,
            state = state,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

/** App-wide [RefreshDefaults]; defaults to [Material3RefreshDefaults]. */
val LocalRefreshDefaults = staticCompositionLocalOf<RefreshDefaults> { Material3RefreshDefaults }

/** Sets the app-wide pull-to-refresh [defaults] for [content]. */
@Composable
fun ProvideRefreshDefaults(defaults: RefreshDefaults, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalRefreshDefaults provides defaults, content = content)
}

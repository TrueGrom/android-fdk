package grmv.android.fdk.screen

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.FabPosition
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import grmv.android.fdk.screen.content.LoadingDefaults
import grmv.android.fdk.screen.content.FdkContentTransitions
import grmv.android.fdk.screen.content.LocalContentTransitions
import grmv.android.fdk.screen.content.LocalLoadingDefaults
import grmv.android.fdk.screen.content.containerColor
import grmv.android.fdk.screen.content.fabPosition
import grmv.android.fdk.screen.content.screenDefaultInsets
import grmv.android.fdk.screen.error.ErrorEffectsDefaults
import grmv.android.fdk.screen.error.LocalErrorEffectsDefaults
import grmv.android.fdk.screen.paging.LocalPagingDefaults
import grmv.android.fdk.screen.paging.PagingDefaults
import grmv.android.fdk.screen.refresh.LocalRefreshDefaults
import grmv.android.fdk.screen.refresh.RefreshDefaults
import grmv.android.fdk.screen.topbars.LocalTopBarDefaults
import grmv.android.fdk.screen.topbars.TopBarDefaults

/**
 * App-wide default slot values for [FdKitBaseScaffold], provided via [LocalFdKitBaseScaffoldDefaults].
 *
 * Each theme-derived default is a [Composable] producer so it resolves in the caller's composition
 * (correct theme colors). Override a subset by providing a copy to [LocalFdKitBaseScaffoldDefaults]
 * (or through [FdkScreenDefaults]); each [FdKitBaseScaffold] call site still overrides via its own
 * parameters.
 */
@Immutable
class FdKitBaseScaffoldDefaults(
    val floatingActionButtonPosition: FabPosition = ScaffoldDefaults.fabPosition,
    val containerColor: @Composable () -> Color = { ScaffoldDefaults.containerColor() },
    val contentColor: @Composable (containerColor: Color) -> Color = { contentColorFor(it) },
    val contentWindowInsets: @Composable () -> WindowInsets = { ScaffoldDefaults.screenDefaultInsets() },
)

/** App-wide [FdKitBaseScaffoldDefaults] consulted by [FdKitBaseScaffold]'s slot defaults. */
val LocalFdKitBaseScaffoldDefaults = staticCompositionLocalOf { FdKitBaseScaffoldDefaults() }

/** Sets the app-wide scaffold [defaults] for [content]. */
@Composable
fun ProvideFdKitBaseScaffoldDefaults(
    defaults: FdKitBaseScaffoldDefaults,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalFdKitBaseScaffoldDefaults provides defaults, content = content)
}

/**
 * Configures every FdKit screen default in one place and provides them to [content].
 *
 * A single entry point that wires the app-wide theming contracts consumed across the screen
 * building blocks: [ContentPaddingDefaults] (screen spacing), [LoadingDefaults] (shared loaders),
 * [ErrorEffectsDefaults] (error presentation), [PagingDefaults] ([PagingContent] load states),
 * [TopBarDefaults] (`FdKit*TopBar` presets), [RefreshDefaults] (`FdKitRefresh*` pull indicator),
 * and [FdkContentTransitions] (state-slot animations). Place it once, high in the composition
 * (typically just inside your theme).
 *
 * Each parameter defaults to the current value from its `Local*Defaults`, so this may be called
 * with no arguments (all Material3 fallbacks), with a subset overridden, or nested — an unset slot
 * preserves whatever an outer provider (or the fallback) already supplies. For finer-grained scoping
 * the per-contract `Provide*Defaults` composables remain available.
 *
 * @param contentPaddingDefaults screen-edge spacing (content padding).
 * @param loadingDefaults shared loading-indicator presentation.
 * @param errorEffectsDefaults error-message mapping and dialog presentation.
 * @param pagingDefaults paged-list load-state slots.
 * @param topBarDefaults theming for the `FdKit*TopBar` presets.
 * @param refreshDefaults pull-to-refresh indicator for the `FdKitRefresh*` containers.
 * @param baseScaffoldDefaults slot defaults for [FdKitBaseScaffold].
 * @param contentTransitions state-slot animations for `Fetchable` and `PagingContent`; defaults to
 *   no animation.
 * @param content composition scoped to the provided defaults.
 */
@Composable
fun FdkScreenDefaults(
    contentPaddingDefaults: ContentPaddingDefaults = LocalContentPaddingDefaults.current,
    loadingDefaults: LoadingDefaults = LocalLoadingDefaults.current,
    errorEffectsDefaults: ErrorEffectsDefaults = LocalErrorEffectsDefaults.current,
    pagingDefaults: PagingDefaults = LocalPagingDefaults.current,
    topBarDefaults: TopBarDefaults = LocalTopBarDefaults.current,
    refreshDefaults: RefreshDefaults = LocalRefreshDefaults.current,
    baseScaffoldDefaults: FdKitBaseScaffoldDefaults = LocalFdKitBaseScaffoldDefaults.current,
    contentTransitions: FdkContentTransitions = LocalContentTransitions.current,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalContentPaddingDefaults provides contentPaddingDefaults,
        LocalLoadingDefaults provides loadingDefaults,
        LocalErrorEffectsDefaults provides errorEffectsDefaults,
        LocalPagingDefaults provides pagingDefaults,
        LocalTopBarDefaults provides topBarDefaults,
        LocalRefreshDefaults provides refreshDefaults,
        LocalFdKitBaseScaffoldDefaults provides baseScaffoldDefaults,
        LocalContentTransitions provides contentTransitions,
        content = content,
    )
}

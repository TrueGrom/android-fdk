package grmv.android.fdk.screen.topbars

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import grmv.android.fdk.screen.ScaffoldSettings

/**
 * [MediumTopAppBar] preset (collapsing headline style) wired to [ScaffoldSettings.scrollBehavior].
 *
 * The title collapses into the toolbar as the user scrolls. Pass [onNavigateBack] for the common
 * case of a back arrow; supply [navigationIcon] explicitly to replace the default entirely.
 *
 * @param title Text shown as the headline when expanded and in the bar when collapsed.
 * @param onNavigateBack When non-null, the default [navigationIcon] renders a [BackNavigationIcon]
 * that invokes this lambda. Ignored when [navigationIcon] is overridden.
 * @param navigationIcon Leading slot. Defaults to [LocalTopBarDefaults]'s back icon driven by
 * [onNavigateBack]; pass your own composable to replace it.
 * @param actions Trailing icon row; empty by default.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaffoldSettings.FdKitTopBarHeadlineTitle(
    title: String,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
    navigationIcon: @Composable () -> Unit = { LocalTopBarDefaults.current.NavigationIcon(onNavigateBack) },
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: WindowInsets = LocalTopBarDefaults.current.windowInsets,
    colors: TopAppBarColors = LocalTopBarDefaults.current.colors(),
) {
    MediumTopAppBar(
        title = { Text(text = title) },
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        colors = colors,
        scrollBehavior = scrollBehavior,
        windowInsets = windowInsets,
    )
}

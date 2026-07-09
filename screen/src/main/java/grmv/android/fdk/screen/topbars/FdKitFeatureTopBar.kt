package grmv.android.fdk.screen.topbars

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import grmv.android.fdk.screen.ScaffoldSettings

/**
 * Small [TopAppBar] preset wired to [ScaffoldSettings.scrollBehavior].
 *
 * Unlike [FdKitTopBarTextTitle] and [FdKitTopBarHeadlineTitle], the leading slot has no built-in back-arrow
 * default — it is typically a profile avatar or hamburger.
 *
 * @param title Text displayed in the bar.
 * @param maxLines Maximum lines for [title] before [overflow] applies. Defaults to `1`.
 * @param overflow Truncation strategy when [title] exceeds [maxLines]. Defaults to [TextOverflow.Ellipsis].
 * @param navigationIcon Leading slot; typically a profile, menu, or branded icon. Defaults to
 * [LocalTopBarDefaults]'s [TopBarDefaults.FeatureNavigationIcon] (renders nothing until a consumer provides one).
 * @param actions Trailing icon row; empty by default.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaffoldSettings.FdKitFeatureTopBar(
    title: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    navigationIcon: @Composable () -> Unit = { LocalTopBarDefaults.current.FeatureNavigationIcon() },
    actions: @Composable RowScope.() -> Unit = {},
    expandedHeight: Dp = LocalTopBarDefaults.current.expandedHeight,
    windowInsets: WindowInsets = LocalTopBarDefaults.current.windowInsets,
    colors: TopAppBarColors = LocalTopBarDefaults.current.colors(),
) {
    TopAppBar(
        title = {
            Text(text = title, maxLines = maxLines, overflow = overflow)
        },
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        colors = colors,
        scrollBehavior = scrollBehavior,
        windowInsets = windowInsets,
        expandedHeight = expandedHeight,
    )
}

package grmv.android.fdk.screen.topbars

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import grmv.android.fdk.screen.ScaffoldSettings

/**
 * [CenterAlignedTopAppBar] preset wired to [ScaffoldSettings.scrollBehavior].
 *
 * The title is centered horizontally. Pass [onNavigateBack] for the common case of a back arrow;
 * supply [navigationIcon] explicitly to replace the default entirely.
 *
 * @param title Text centered in the bar.
 * @param maxLines Maximum lines for [title] before [overflow] applies. Defaults to `1`.
 * @param overflow Truncation strategy when [title] exceeds [maxLines]. Defaults to [TextOverflow.Ellipsis].
 * @param onNavigateBack When non-null, the default [navigationIcon] renders a [BackNavigationIcon]
 * that invokes this lambda. Ignored when [navigationIcon] is overridden.
 * @param navigationIcon Leading slot. Defaults to [LocalTopBarDefaults]'s back icon driven by
 * [onNavigateBack]; pass your own composable to replace it.
 * @param actions Trailing icon row; empty by default.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaffoldSettings.FdKitTopBarTextTitle(
    title: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    onNavigateBack: (() -> Unit)? = null,
    navigationIcon: @Composable () -> Unit = { LocalTopBarDefaults.current.NavigationIcon(onNavigateBack) },
    actions: @Composable RowScope.() -> Unit = {},
    expandedHeight: Dp = LocalTopBarDefaults.current.expandedHeight,
    windowInsets: WindowInsets = LocalTopBarDefaults.current.windowInsets,
    colors: TopAppBarColors = LocalTopBarDefaults.current.colors(),
) {
    CenterAlignedTopAppBar(
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

/** Default back-navigation icon: an [Icons.AutoMirrored.Filled.ArrowBack] button, or nothing when [onClick] is null. */
@Composable
internal fun BackNavigationIcon(onClick: (() -> Unit)?) {
    if (onClick != null) {
        IconButton(onClick = onClick) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
        }
    }
}

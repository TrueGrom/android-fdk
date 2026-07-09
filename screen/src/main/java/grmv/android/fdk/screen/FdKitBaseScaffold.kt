package grmv.android.fdk.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll

/**
 * Content receiver for [FdKitBaseScaffold]. Extends [BoxScope] so the body may use box alignment APIs
 * and call the content helpers ([FdKitScreenColumn], [FdKitScrollableScreen], …) that target this scope.
 */
@Stable
interface ScaffoldScope : BoxScope

/** Exposes the scaffold's [scrollBehavior] to slot composables (top bars, etc.). */
@OptIn(ExperimentalMaterial3Api::class)
@Stable
interface ScaffoldSettings {
    /** Scroll behavior shared with collapsing top bars; `null` disables nested-scroll coupling. */
    val scrollBehavior: TopAppBarScrollBehavior?
}

/**
 * Material3 [Scaffold] wrapper with [ScaffoldSettings]-scoped slots and a [ScaffoldScope] body.
 *
 * Entry point for building a screen: call it on a [ScreenScope] (e.g. inside a [ViewModelScreen]
 * body or from a [rememberScreenScope]). The top/bottom bar, snackbar host and FAB slots receive
 * [ScaffoldSettings] so they can wire into [scrollBehavior] — pass the `FdKit*TopBar` presets and
 * [ScreenSnackbarHost] there. The body slot receives a [ScaffoldScope] (also a [BoxScope]) on which
 * the content helpers ([FdKitScreenColumn], [FdKitScrollableScreen], …) are callable.
 *
 * @param topBar top app bar slot; receives [ScaffoldSettings]. Empty by default.
 * @param bottomBar bottom bar slot; receives [ScaffoldSettings]. Empty by default.
 * @param snackbarHost snackbar host slot; place a [ScreenSnackbarHost] here. Empty by default.
 * @param floatingActionButton FAB slot; receives [ScaffoldSettings]. Empty by default.
 * @param scrollBehavior coupled to the content via nested scroll and shared with collapsing top bars
 *   through [ScaffoldSettings]; defaults to enter-always. Pass `null` to disable nested-scroll coupling.
 * @param contentWindowInsets insets applied to the body.
 * @param content screen body, invoked with the [ScaffoldScope] receiver.
 *
 * Slot defaults (FAB position, colors, insets) come from [LocalFdKitBaseScaffoldDefaults]; override
 * them app-wide via [FdkScreenDefaults]. [scrollBehavior] is not part of those defaults — it is
 * created per call site (enter-always) and passed explicitly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScope.FdKitBaseScaffold(
    topBar: @Composable ScaffoldSettings.() -> Unit = {},
    bottomBar: @Composable ScaffoldSettings.() -> Unit = {},
    snackbarHost: @Composable ScaffoldSettings.() -> Unit = {},
    floatingActionButton: @Composable ScaffoldSettings.() -> Unit = {},
    floatingActionButtonPosition: FabPosition =
        LocalFdKitBaseScaffoldDefaults.current.floatingActionButtonPosition,
    containerColor: Color = LocalFdKitBaseScaffoldDefaults.current.containerColor(),
    contentColor: Color = LocalFdKitBaseScaffoldDefaults.current.contentColor(containerColor),
    contentWindowInsets: WindowInsets = LocalFdKitBaseScaffoldDefaults.current.contentWindowInsets(),
    scrollBehavior: TopAppBarScrollBehavior? =
        TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState()),
    content: @Composable ScaffoldScope.() -> Unit,
) {
    val scaffoldSettings = remember(scrollBehavior) {
        object : ScaffoldSettings {
            override val scrollBehavior: TopAppBarScrollBehavior? = scrollBehavior
        }
    }
    Scaffold(
        modifier = scrollBehavior?.let { Modifier.nestedScroll(it.nestedScrollConnection) }
            ?: Modifier,
        topBar = { scaffoldSettings.topBar() },
        bottomBar = { scaffoldSettings.bottomBar() },
        snackbarHost = { scaffoldSettings.snackbarHost() },
        floatingActionButton = { scaffoldSettings.floatingActionButton() },
        floatingActionButtonPosition = floatingActionButtonPosition,
        containerColor = containerColor,
        contentColor = contentColor,
        contentWindowInsets = contentWindowInsets,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            val scope = remember {
                object : ScaffoldScope, BoxScope by this {}
            }
            scope.content()
        }
    }
}

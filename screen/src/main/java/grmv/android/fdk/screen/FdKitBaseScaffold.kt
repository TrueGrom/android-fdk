package grmv.android.fdk.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
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
 * @param contentWindowInsets insets applied to the body. Note that passing this replaces whatever
 *   the app configured app-wide, not just the SDK default; to build on the app's value, start from
 *   `LocalFdKitBaseScaffoldDefaults.current.contentWindowInsets()`.
 * @param avoidKeyboard pads the **body** by the ime inset, keeping it clear of the keyboard;
 *   defaults to the app-wide [LocalFdKitBaseScaffoldDefaults]. Pass `false` for a screen that draws
 *   under the keyboard (map, camera, full-bleed background) or handles the inset itself. Its scope
 *   is the body alone: the `bottomBar` and the FAB stay behind the keyboard, and [ScreenSnackbarHost]
 *   rides above it through its own always-on padding, not through this flag. It shrinks the body's
 *   viewport rather than scrolling it, so it serves a scrolling or bottom-anchored body — a static
 *   one taller than what is left still overflows, which is why a form belongs in
 *   [FdKitScrollableScreen]. A body that also applies its own `Modifier.imePadding()` does not
 *   double-pad: that modifier excludes insets a parent consumed. For the FAB, see
 *   [hideFabWhenImeVisible].
 * @param hideFabWhenImeVisible removes the FAB from the composition while the keyboard is up;
 *   defaults to the app-wide [LocalFdKitBaseScaffoldDefaults] (off). The FAB is never *lifted* —
 *   Material3 derives the snackbar's offset from the FAB's measured height, so padding that slot
 *   would count the keyboard inset twice on a screen with both. The same coupling means a snackbar
 *   visible when the FAB goes away drops by the FAB's height for a frame. Wrap the slot in your own
 *   `AnimatedVisibility` if it should fade rather than vanish.
 *
 *   Note also that a full-bleed layer inside the body (a background drawn with `matchParentSize`)
 *   now lays out into the shrunk box, so it moves with the keyboard.
 * @param content screen body, invoked with the [ScaffoldScope] receiver.
 *
 * Slot defaults (FAB position, colors, insets) come from [LocalFdKitBaseScaffoldDefaults]; override
 * them app-wide via [FdkScreenDefaults]. [scrollBehavior] is not part of those defaults — it is
 * created per call site (enter-always) and passed explicitly.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    avoidKeyboard: Boolean = LocalFdKitBaseScaffoldDefaults.current.avoidKeyboard,
    hideFabWhenImeVisible: Boolean =
        LocalFdKitBaseScaffoldDefaults.current.hideFabWhenImeVisible,
    scrollBehavior: TopAppBarScrollBehavior? =
        TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState()),
    content: @Composable ScaffoldScope.() -> Unit,
) {
    WarnOnConflictingSoftInputMode(avoidKeyboard)
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
        // Deliberately not *lifted* by `avoidKeyboard`: Scaffold derives the snackbar's offset from
        // the FAB's measured height, so padding this slot would count the ime inset twice for a
        // screen that has both — and the slot cannot know what the scaffold already reserved.
        floatingActionButton = {
            if (!hideFabWhenImeVisible || !WindowInsets.isImeVisible) {
                scaffoldSettings.floatingActionButton()
            }
        },
        floatingActionButtonPosition = floatingActionButtonPosition,
        containerColor = containerColor,
        contentColor = contentColor,
        contentWindowInsets = contentWindowInsets,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                // After consuming the scaffold padding this adds only the excess,
                // `max(0, ime - innerPadding.bottom)`, so the body's bottom always lands at
                // `max(innerPadding.bottom, ime)` whatever the bottom bar contributed.
                .then(if (avoidKeyboard) Modifier.imePadding() else Modifier),
        ) {
            val scope = remember {
                object : ScaffoldScope, BoxScope by this {}
            }
            scope.content()
        }
    }
}

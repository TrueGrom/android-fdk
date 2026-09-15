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
 * (correct theme colors). Use `copy` to replace a single slot while keeping whatever an outer
 * provider already supplies.
 *
 * Precedence: [FdKitBaseScaffold] call-site parameter > [LocalFdKitBaseScaffoldDefaults] > the slot
 * defaults below. Provide once via [ProvideFdKitBaseScaffoldDefaults] (or through
 * [FdkScreenDefaults]); implementations MUST be truly immutable, or recomposition may be skipped.
 *
 * [avoidKeyboard] is on by default, so an input screen cannot end up with its submit button behind
 * the keyboard. To opt out app-wide:
 *
 * ```
 * FdkScreenDefaults(
 *     baseScaffoldDefaults = LocalFdKitBaseScaffoldDefaults.current.copy(avoidKeyboard = false),
 * ) { AppContent() }
 * ```
 *
 * @param avoidKeyboard pads the scaffold **body** by the ime inset, keeping it clear of the
 *   keyboard. Under `enableEdgeToEdge()` the window is never resized for the keyboard, so
 *   without this the bottom of a form sits behind it. Turn it off for a screen that means to draw
 *   under the keyboard (map, camera, full-bleed background) or that handles the inset itself.
 *   The padding lands on the body, not on [contentWindowInsets], so it applies equally with and
 *   without a `bottomBar` — Material3 takes the body's bottom padding from the bar height when that
 *   slot is filled, and would ignore an inset put there. Its scope is the body alone: the bottom
 *   bar and the FAB stay behind the keyboard, and `ScreenSnackbarHost` rides above it through its
 *   own always-on padding, not through this flag. It shrinks the body's viewport rather than
 *   scrolling it, so it
 *   serves a *scrolling* or bottom-anchored body; a static body taller than what is left still
 *   overflows. The inset animates in step with the keyboard from API 30; on API 26-29 it is reported
 *   only under a resizing window, so `android:windowSoftInputMode="adjustResize"` is still needed
 *   there, and `adjustPan` breaks this everywhere — which the SDK cannot enforce, only report: a
 *   debuggable build logs a warning through `FdkLog` when it finds a declared `adjustPan`.
 * @param hideFabWhenImeVisible removes the FAB from the composition while the keyboard is up.
 *   Off by default: a keyboard already occludes a bottom-anchored FAB, so this changes little
 *   visually and is a presentation choice rather than a correctness fix — it does, however, keep an
 *   unreachable control out of the semantics tree, and spares a FAB peeking over a short or floating
 *   keyboard. The FAB is not *lifted* instead, at any setting: Material3 derives the snackbar's
 *   offset from the FAB's measured height, so padding that slot would count the keyboard inset twice
 *   on a screen with both. That same coupling is why hiding is not free either — a snackbar visible
 *   at the moment the FAB goes away drops by the FAB's height plus its spacing in one frame, then
 *   rides back up with the keyboard, since the ime is reported visible before the inset has grown.
 *   A screen wanting the FAB to fade rather than vanish wraps its own `AnimatedVisibility` in the
 *   slot.
 */
@Immutable
class FdKitBaseScaffoldDefaults(
    val floatingActionButtonPosition: FabPosition = ScaffoldDefaults.fabPosition,
    val containerColor: @Composable () -> Color = { ScaffoldDefaults.containerColor() },
    val contentColor: @Composable (containerColor: Color) -> Color = { contentColorFor(it) },
    val contentWindowInsets: @Composable () -> WindowInsets = { ScaffoldDefaults.screenDefaultInsets() },
    val avoidKeyboard: Boolean = true,
    val hideFabWhenImeVisible: Boolean = false,
) {
    // Hand-written rather than `data`: to keep `componentN` out of a published artifact's API, and
    // because `equals`/`hashCode` below are load-bearing rather than incidental. Binary
    // compatibility is not the reason — this `copy`'s own `copy$default` descriptor changes on an
    // added slot exactly as the generated one would. Keep all three in step with the constructor.
    /** Returns a copy of these defaults with the given slots replaced; unset slots are preserved. */
    fun copy(
        floatingActionButtonPosition: FabPosition = this.floatingActionButtonPosition,
        containerColor: @Composable () -> Color = this.containerColor,
        contentColor: @Composable (containerColor: Color) -> Color = this.contentColor,
        contentWindowInsets: @Composable () -> WindowInsets = this.contentWindowInsets,
        avoidKeyboard: Boolean = this.avoidKeyboard,
        hideFabWhenImeVisible: Boolean = this.hideFabWhenImeVisible,
    ): FdKitBaseScaffoldDefaults = FdKitBaseScaffoldDefaults(
        floatingActionButtonPosition = floatingActionButtonPosition,
        containerColor = containerColor,
        contentColor = contentColor,
        contentWindowInsets = contentWindowInsets,
        avoidKeyboard = avoidKeyboard,
        hideFabWhenImeVisible = hideFabWhenImeVisible,
    )

    // Structural equality matters: `LocalFdKitBaseScaffoldDefaults` is a static composition local,
    // and its provider compares the provided value (`StaticValueHolder` is a data class), so
    // identity equality here would invalidate the whole subtree on every `copy`.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FdKitBaseScaffoldDefaults) return false
        return floatingActionButtonPosition == other.floatingActionButtonPosition &&
            containerColor == other.containerColor &&
            contentColor == other.contentColor &&
            contentWindowInsets == other.contentWindowInsets &&
            avoidKeyboard == other.avoidKeyboard &&
            hideFabWhenImeVisible == other.hideFabWhenImeVisible
    }

    override fun hashCode(): Int {
        var result = floatingActionButtonPosition.hashCode()
        result = 31 * result + containerColor.hashCode()
        result = 31 * result + contentColor.hashCode()
        result = 31 * result + contentWindowInsets.hashCode()
        result = 31 * result + avoidKeyboard.hashCode()
        result = 31 * result + hideFabWhenImeVisible.hashCode()
        return result
    }
}

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
 * [ErrorEffectsDefaults] (error presentation), [PagingDefaults] (load states for paged lists and
 * grids alike),
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
 * @param pagingDefaults load-state slots for paged content, list and grid alike.
 * @param topBarDefaults theming for the `FdKit*TopBar` presets.
 * @param refreshDefaults pull-to-refresh indicator for the `FdKitRefresh*` containers.
 * @param baseScaffoldDefaults slot defaults for [FdKitBaseScaffold].
 * @param contentTransitions state-slot animations for `Fetchable` and paged content; defaults to
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

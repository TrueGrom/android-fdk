package grmv.android.fdk.screen.content

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.union
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable

/**
 * Scaffold content insets used by every screen that does not pass `contentWindowInsets` explicitly:
 * Material3's [ScaffoldDefaults.contentWindowInsets] (`systemBars union displayCutout`), with the
 * cutout re-stated as a guard should that default ever change.
 *
 * Nothing is stripped. `Scaffold` reads either a bar's height or the inset, never both, so a screen
 * *with* a top/bottom bar is unaffected — the bar consumes its own system-bar inset
 * (`TopAppBarDefaults.windowInsets`/`BottomAppBarDefaults.windowInsets`) and the body is offset by
 * the bar's height. A screen *without* one of the bars is the case this covers: previously the
 * vertical component was filtered out and only the cutout put a top inset back, so a no-top-bar
 * screen on a device with no cutout ran under the status bar, and a no-bottom-bar screen put its
 * body, FAB and snackbar under the gesture/navigation bar.
 *
 * The keyboard is deliberately not part of this: it is handled by `FdKitBaseScaffold`'s `avoidKeyboard`
 * flag, which pads the body rather than the scaffold, so that it also works on a screen with a
 * bottom bar.
 */
@Composable
fun ScaffoldDefaults.screenDefaultInsets(): WindowInsets =
    // `union` (per-side maximum), not `add` (per-side sum): the base already contains the cutout,
    // and on a side where both land summing would reserve that side twice.
    contentWindowInsets.union(WindowInsets.displayCutout)

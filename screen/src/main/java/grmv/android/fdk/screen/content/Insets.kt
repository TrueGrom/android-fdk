package grmv.android.fdk.screen.content

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.union
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable

/**
 * Horizontal-only scaffold content insets, combined with [WindowInsets.displayCutout].
 *
 * Vertical system-bar insets are stripped so the scaffold's top/bottom bars own them. The cutout,
 * however, survives that stripping on all four sides: Material3's
 * [ScaffoldDefaults.contentWindowInsets] is already `systemBars union displayCutout`, so on the
 * horizontal sides the cutout term below is redundant (kept as a guard, should that default ever
 * change), and its real effect is to re-add the cutout's *vertical* component — which is the only
 * top inset a screen with no top bar gets.
 *
 * The keyboard is deliberately not part of this: it is handled by `FdKitBaseScaffold`'s `avoidKeyboard`
 * flag, which pads the body rather than the scaffold, so that it also works on a screen with a
 * bottom bar.
 */
@Composable
fun ScaffoldDefaults.screenDefaultInsets(): WindowInsets =
    // `union` (per-side maximum), not `add` (per-side sum): the base already contains the cutout,
    // and on a side where both land summing would reserve that side twice.
    contentWindowInsets.only(WindowInsetsSides.Horizontal).union(WindowInsets.displayCutout)

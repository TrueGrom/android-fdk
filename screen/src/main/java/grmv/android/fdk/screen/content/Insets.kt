package grmv.android.fdk.screen.content

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable

/**
 * Horizontal-only scaffold content insets combined with [WindowInsets.displayCutout].
 *
 * Strips vertical (status-bar / nav-bar) insets so the scaffold's top/bottom bars handle those;
 * the cutout is added to prevent content from being obscured by camera notches on the sides.
 */
@Composable
fun ScaffoldDefaults.screenDefaultInsets(): WindowInsets {
    return contentWindowInsets.only(WindowInsetsSides.Horizontal).add(WindowInsets.displayCutout)
}

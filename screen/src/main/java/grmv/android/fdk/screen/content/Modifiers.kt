package grmv.android.fdk.screen.content

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import grmv.android.fdk.screen.LocalContentPaddingDefaults
import grmv.android.fdk.screen.ContentPaddingDefaults


/**
 * Applies the standard screen content padding from the current [ContentPaddingDefaults].
 *
 * Use on any composable that should respect the module-wide horizontal and vertical screen margins
 * without being wrapped in a [FdKitScreenColumn] or scrollable container.
 */
@Composable
@ReadOnlyComposable
fun Modifier.screenDefaultPadding(): Modifier =
    then(Modifier.padding(LocalContentPaddingDefaults.current.contentPadding))

package grmv.android.fdk.screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

/**
 * App-wide default spacing applied by the screen content helpers ([FdKitScreenColumn],
 * [FdKitScrollableScreen], [PagingContent], …).
 *
 * Provide a custom instance once high in the composition via [ProvideContentPaddingDefaults] (or
 * through [FdkScreenDefaults]) to retune screen margins app-wide; each call site still overrides via
 * its own `contentPadding` parameter, which falls back to the current [ContentPaddingDefaults].
 */
@Immutable
interface ContentPaddingDefaults {
    /** Content padding applied to screen containers. */
    val contentPadding: PaddingValues
}

/** Material3 fallback used until a consumer provides its own [ContentPaddingDefaults]. */
internal object Material3ContentPaddingDefaults : ContentPaddingDefaults {
    override val contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
}

/** App-wide [ContentPaddingDefaults]; defaults to [Material3ContentPaddingDefaults]. */
val LocalContentPaddingDefaults =
    staticCompositionLocalOf<ContentPaddingDefaults> { Material3ContentPaddingDefaults }

/** Sets the app-wide screen spacing [defaults] for [content]. */
@Composable
fun ProvideContentPaddingDefaults(defaults: ContentPaddingDefaults, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalContentPaddingDefaults provides defaults, content = content)
}

package grmv.android.fdk.uikit.locale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalLocale
import java.util.Locale

/**
 * The current [Locale] from the ambient composition, read from `LocalLocale`. This is the locale
 * source used by every formatter composable in this module, so reading it directly is only needed
 * for custom formatting; prefer the `localizedFormat` helpers for date/time output.
 *
 * Reads at composition time and triggers recomposition of the caller when the ambient locale
 * changes (e.g. the user switches the device language).
 *
 * @return the platform [Locale] backing the current composition.
 */
val currentLocale: Locale
    @Composable
    @ReadOnlyComposable
    get() = LocalLocale.current.platformLocale

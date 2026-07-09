package grmv.android.fdk.screen.error

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.res.stringResource
import grmv.android.fdk.screen.R

/**
 * App-wide default presentations for [ErrorEffects].
 *
 * Provide a custom instance once high in the composition via [ProvideErrorEffectsDefaults] to skin
 * every [ErrorEffects] call with your design system's error-message mapping and dialog. Each call
 * site still overrides any slot through its own parameter; an unset parameter falls back to the
 * current [ErrorEffectsDefaults].
 *
 * Precedence: per-call parameter > [LocalErrorEffectsDefaults] > [Material3ErrorEffectsDefaults].
 */
@Immutable
interface ErrorEffectsDefaults {
    /** Maps a [Throwable] to the [ErrorMessage] shown for it. */
    @Composable
    fun errorMessage(error: Throwable): ErrorMessage

    /** Dialog presentation for dialog error reactions; [onClose] consumes the error. */
    @Composable
    fun Dialog(message: ErrorMessage, onClose: () -> Unit)
}

/** Material3 fallback used until a consumer provides its own [ErrorEffectsDefaults]. */
internal object Material3ErrorEffectsDefaults : ErrorEffectsDefaults {
    @Composable
    override fun errorMessage(error: Throwable): ErrorMessage =
        ErrorMessage(
            title = stringResource(R.string.fdk_error_unexpected),
            text = error.message ?: stringResource(R.string.fdk_error_something_went_wrong),
        )

    @Composable
    override fun Dialog(message: ErrorMessage, onClose: () -> Unit) {
        AlertDialog(
            onDismissRequest = onClose,
            confirmButton = {
                TextButton(onClick = onClose) { Text(stringResource(android.R.string.ok)) }
            },
            title = message.title?.let { title -> { Text(title) } },
            text = { Text(message.text) },
        )
    }
}

/** App-wide [ErrorEffectsDefaults] for [ErrorEffects]; defaults to [Material3ErrorEffectsDefaults]. */
val LocalErrorEffectsDefaults = staticCompositionLocalOf<ErrorEffectsDefaults> { Material3ErrorEffectsDefaults }

/** Sets the app-wide [ErrorEffects] [defaults] for [content]. */
@Composable
fun ProvideErrorEffectsDefaults(defaults: ErrorEffectsDefaults, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalErrorEffectsDefaults provides defaults, content = content)
}

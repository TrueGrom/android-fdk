package grmv.android.fdk.screen.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import grmv.android.fdk.screen.R
import grmv.android.fdk.state.RemoteData

/**
 * App-wide default presentation for shared content states.
 *
 * Carries the [Loading] indicator and [Error] presentation reused by every screen building block
 * that renders an indeterminate load or a failed fetch — `Fetchable` ([RemoteData] content) and,
 * via `PagingDefaults`, `PagingContent` and the paged grids. Provide a custom instance once (high in
 * the composition) via [ProvideLoadingDefaults] to skin them with your design system; individual
 * call-site slots still override per usage.
 *
 * Both slots are unscoped: callers place them in whatever layout they need (full-screen box, lazy
 * item, …).
 */
@Immutable
interface LoadingDefaults {
    /** Indeterminate loading indicator, laid out by the caller. */
    @Composable
    fun Loading()

    /**
     * Presentation for a failed fetch, laid out by the caller.
     *
     * @param e the failure surfaced by [RemoteData.Error].
     * @param retry invoked with [e] when the user requests a retry.
     */
    @Composable
    fun Error(e: Throwable, retry: (Throwable) -> Unit)
}

/** Material3 fallback used until a consumer provides its own [LoadingDefaults]. */
internal object Material3LoadingDefaults : LoadingDefaults {
    @Composable
    override fun Loading() {
        CircularProgressIndicator()
    }

    @Composable
    override fun Error(e: Throwable, retry: (Throwable) -> Unit) {
        FdKitErrorContent(
            message = e.message ?: stringResource(R.string.fdk_error_something_went_wrong),
            onRetry = { retry(e) },
        )
    }
}

/**
 * Default failed-fetch presentation: a centered [message] above a retry [TextButton].
 *
 * Reused by [Material3LoadingDefaults] and the paging defaults so every default error surface
 * shares one look.
 *
 * @param message the error text to display.
 * @param onRetry invoked when the user taps the retry action.
 * @param modifier applied to the enclosing [Column].
 */
@Composable
fun FdKitErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = message)
        TextButton(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) {
            Text(text = stringResource(R.string.fdk_action_retry))
        }
    }
}

/** App-wide [LoadingDefaults]; defaults to [Material3LoadingDefaults]. */
val LocalLoadingDefaults = staticCompositionLocalOf<LoadingDefaults> { Material3LoadingDefaults }

/** Sets the app-wide shared content [defaults] for [content]. */
@Composable
fun ProvideLoadingDefaults(defaults: LoadingDefaults, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLoadingDefaults provides defaults, content = content)
}

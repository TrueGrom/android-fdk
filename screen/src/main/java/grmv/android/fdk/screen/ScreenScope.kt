package grmv.android.fdk.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember

/**
 * Root marker for screen-level composition scopes.
 *
 * Receiver for [FdKitBaseScaffold] and the entry point that [ViewModelScope] extends. Construct a bare
 * scope with [rememberScreenScope] for screens that do not expose a ViewModel.
 */
@Immutable
interface ScreenScope

/** Returns a remembered [ScreenScope] for screens that do not own a ViewModel. */
@Composable
internal fun rememberScreenScope(): ScreenScope = remember { object : ScreenScope {} }

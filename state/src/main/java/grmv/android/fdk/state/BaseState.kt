package grmv.android.fdk.state

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.StateFlow

/** Marker interface for all immutable UI state types. Implement to signal Compose stability. */
@Immutable
interface BaseState

/**
 * Read-only contract for components that expose a [StateFlow] of UI state.
 *
 * @param T concrete state type; must be [BaseState] to guarantee Compose stability.
 */
@Immutable
interface StateOwner<T : BaseState> {
    /** Hot [StateFlow] of the current state. Never completes; always holds the latest value. */
    val state: StateFlow<T>
}
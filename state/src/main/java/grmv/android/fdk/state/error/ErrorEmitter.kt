package grmv.android.fdk.state.error

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.StateFlow

/**
 * Read-only contract for components that expose a stream of [ErrorReaction]s.
 *
 * @param Action type of the optional follow-up action a user can trigger from an error UI.
 */
@Immutable
interface ErrorEmitter<Action : Any> {
    /** Hot [StateFlow] of the current error reaction. Holds [ErrorReaction.None] when idle. */
    val errors: StateFlow<ErrorReaction<Action>>

    /**
     * Clears [event] from [errors] if it is still the current value.
     * No-op when [errors] has already advanced to a different reaction.
     */
    fun consumeError(event: ErrorReaction<Action>)
}
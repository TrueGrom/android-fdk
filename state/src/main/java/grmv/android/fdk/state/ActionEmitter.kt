package grmv.android.fdk.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Read-only contract for components that expose a stream of one-shot screen actions.
 *
 * @param T sealed class or enum representing the possible screen-level actions.
 */
interface ActionEmitter<T : Any> {
    /** Hot [StateFlow] of the pending action. `null` when no action is pending. */
    val screenActions: StateFlow<T?>

    /**
     * Clears [event] from [screenActions] if it is still the current value.
     * No-op when [screenActions] has already advanced to a different action.
     */
    fun consumeAction(event: T)
}

/**
 * Extends [ActionEmitter] with the ability to emit new actions.
 *
 * @param T sealed class or enum representing the possible screen-level actions.
 */
interface ActionManager<T : Any> : ActionEmitter<T> {
    /** Emits [event] as the current pending action, replacing any unprocessed one. */
    fun sendAction(event: T)
}

/**
 * Default [ActionManager] implementation. Subclass in ViewModels or presenters.
 *
 * Expose only [ActionEmitter] to the UI layer; keep the [ActionManager] reference internal.
 */
open class MutableActionManager<T : Any>() : ActionManager<T> {

    private val _actions = MutableStateFlow<T?>(null)
    override val screenActions: StateFlow<T?> = _actions.asStateFlow()

    override fun sendAction(event: T) {
        _actions.value = event
    }

    override fun consumeAction(event: T) {
        _actions.update { current ->
            if (current == event) {
                null
            } else {
                current
            }
        }
    }
}
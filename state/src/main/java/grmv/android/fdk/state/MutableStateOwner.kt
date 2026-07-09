package grmv.android.fdk.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet

/**
 * Base class for components that own and mutate UI state.
 *
 * Subclass in ViewModels or presenters that need full read/write access to state while
 * exposing only the read-only [StateOwner] contract to the UI layer.
 *
 * @param T concrete state type.
 * @param initial factory invoked once during construction to produce the initial state.
 */
open class MutableStateOwner<T : BaseState>(initial: () -> T): StateOwner<T> {
    private val _state: MutableStateFlow<T> = MutableStateFlow(initial())

    override val state: StateFlow<T> = _state.asStateFlow()

    /** Replaces the current state with [value]. */
    fun setState(value: T) {
        _state.value = value
    }

    /**
     * Atomically applies [updater] to the current state and emits the result.
     *
     * @return the new state after [updater] was applied.
     */
    fun updateState(updater: (T) -> T): T {
        return _state.updateAndGet(updater)
    }
}
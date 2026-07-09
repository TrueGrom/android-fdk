package grmv.android.fdk.state

import grmv.android.fdk.viewmodel.BaseViewModel
import kotlinx.coroutines.CoroutineScope


/**
 * Abstract ViewModel base that couples a [MutableStateOwner] with a builder-pattern DSL for
 * producing immutable state transitions.
 *
 * Subclasses own the state type [T] and its builder [B]. State is mutated exclusively through
 * [state], which creates a fresh builder from the current snapshot via [builderFactory], applies
 * the caller's mutations, and atomically emits the result. Direct field assignment to the state
 * object is intentionally impossible.
 *
 * Reusable mutation surface is added by mixing trait interfaces (see [BuilderOps]) into the
 * builder type. Attach one or more [StateViewModelDelegate]s when you need a stateful
 * collaborator (own fields, coroutines, lifecycle) rather than additional mutation methods.
 *
 * ### Example
 *
 * ```
 * data class CounterState(val count: Int = 0) : BaseState
 *
 * class CounterStateBuilder(override val initial: CounterState) :
 *     BaseStateBuilder<CounterState>() {
 *     fun increment() = accumulate { it.copy(count = it.count + 1) }
 * }
 *
 * class CounterViewModel(
 *     owner: MutableStateOwner<CounterState> = MutableStateOwner { CounterState() },
 * ) : StateViewModel<CounterState, CounterStateBuilder>(owner, ::CounterStateBuilder) {
 *     fun bumpTwice() = state {
 *         increment()
 *         increment()
 *     }
 * }
 * ```
 *
 * @param T concrete [BaseState] type held by this ViewModel.
 * @param B builder that produces [T]; produced by [builderFactory] on every [state] call.
 * @param builderFactory factory invoked on each [state] call to create a fresh [B] seeded with
 *  the current state snapshot. Typically, a constructor reference, e.g. `::FeedStateBuilder`.
 */
abstract class StateViewModel<T : BaseState, B : BaseStateBuilder<T>>(
    private val stateOwner: MutableStateOwner<T>,
    private val builderFactory: (T) -> B,
) : BaseViewModel(), StateOwner<T> by stateOwner {

    /**
     * Returns the current state snapshot synchronously.
     *
     * Prefer collecting [state] as a Flow in the UI layer; use this only where a suspend-free
     * read of the latest value is required (e.g., inside a [state] updater block).
     */
    protected fun getActualState(): T = stateOwner.state.value

    /**
     * Applies [updater] to a fresh builder seeded from the current state and emits the result.
     *
     * The builder is created via [builderFactory] on every call, so mutations inside [updater]
     * never affect a shared object. The produced state is emitted atomically.
     *
     * @return the new state after [updater] was applied.
     */
    fun state(updater: B.() -> Unit): T {
        return stateOwner.updateState { current ->
            builderFactory(current).apply(updater).build()
        }
    }

    /**
     * Forwards a coroutine block to this ViewModel's task launcher.
     *
     * Intended for [StateViewModelDelegate] instances that need to launch coroutines scoped to
     * this ViewModel's lifecycle without holding a direct reference to [BaseViewModel.task].
     */
    internal fun delegatedTask(taskJob: suspend CoroutineScope.() -> Unit) {
        task(taskJob)
    }
}

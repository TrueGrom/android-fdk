package grmv.android.fdk.state

/**
 * Minimal contract a state builder must satisfy so reusable mutation traits can be mixed in.
 *
 * Trait interfaces (e.g. `PaginationOps<S>`, `LoadingOps<S>`) extend this and declare default
 * methods that call [accumulate]. Any [BaseStateBuilder] subclass automatically satisfies the
 * contract, so adding traits is a matter of additional `: SomeOps<S>` supertypes.
 *
 * ### Example — a reusable trait built on [BuilderOps]
 *
 * ```
 * interface LoadableState<S : LoadableState<S>> : BaseState {
 *     val loading: Boolean
 *     fun withLoading(loading: Boolean): S
 * }
 *
 * interface LoadingOps<S : LoadableState<S>> : BuilderOps<S> {
 *     fun showLoading() = accumulate { it.withLoading(true) }
 *     fun hideLoading() = accumulate { it.withLoading(false) }
 * }
 * ```
 *
 * @param S concrete [BaseState] type the builder produces.
 */
interface BuilderOps<S : BaseState> {
    /**
     * Applies [updater] to the current accumulated state and stores the result so subsequent
     * mutations see the new value.
     */
    fun accumulate(updater: (S) -> S)
}

/**
 * Accumulator for incremental state construction.
 *
 * Mutations are expressed as [accumulate] transforms: each call appends a full-state transform to
 * an ordered list. Nothing is executed until [build], which folds every transform over [initial]
 * in call order, each seeing the prior result. Suitable for reusable mutation traits.
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
 * ```
 *
 * ### Threading
 *
 * **Not thread-safe.** Each instance is meant to be confined to a single build pass — created,
 * mutated, and consumed on one thread (as done by `StateViewModel.state`, which allocates a fresh
 * builder per call). Do not share an instance across threads or call [accumulate] concurrently.
 *
 * @param S concrete [BaseState] type produced by this builder.
 */
abstract class BaseStateBuilder<S : BaseState> : BuilderOps<S> {
    /**
     * The state snapshot this builder was seeded with. Subclasses must override it, typically as a
     * constructor parameter, e.g. `class FooStateBuilder(override val initial: FooState)`.
     */
    protected abstract val initial: S

    /** Deferred [accumulate] transforms, applied in order at [build] time. */
    private val updaters = mutableListOf<(S) -> S>()

    override fun accumulate(updater: (S) -> S) {
        updaters += updater
    }

    /**
     * Produces the final state by folding every [accumulate] transform over [initial] in call
     * order. Any modification must be expressed as an [accumulate] transform, not applied here.
     */
    fun build(): S {
        return updaters.fold(initial) { acc, updater -> updater(acc) }
    }
}

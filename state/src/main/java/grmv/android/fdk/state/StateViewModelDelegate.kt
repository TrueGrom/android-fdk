package grmv.android.fdk.state

import kotlinx.coroutines.CoroutineScope

/**
 * Companion to a [StateViewModel] that owns its own state (fields, coroutines, lifecycle)
 * and forwards mutations into the owner's builder.
 *
 * Use a delegate when a concern is a *stateful collaborator* — for example a paginator that
 * holds the current cursor, a websocket controller, or a background sync worker. The delegate
 * launches coroutines via [task] (scoped to the owner) and mutates state via [state], which
 * runs the caller's block against the owner's builder [B].
 *
 * Use a **trait interface** (see [BuilderOps]) instead of a delegate when the concern is a
 * *reusable set of mutations* over a particular state shape (pagination, loading, error). Traits
 * compose freely on the builder type and all become available inside a single [state] block,
 * yielding one atomic emission.
 *
 * After construction, call [initialize] exactly once before invoking any other method; all calls
 * before initialization will throw [UninitializedPropertyAccessException].
 *
 * ### Example — a delegate that fetches and writes via the builder's traits
 *
 * ```
 * class ProfileLoaderDelegate(
 *     private val fetch: suspend () -> Profile,
 * ) : StateViewModelDelegate<ProfileState, ProfileStateBuilder>() {
 *
 *     fun refresh() = task {
 *         state { loading() }
 *         runCatching { fetch() }
 *             .onSuccess { profile -> state { fetched(profile) } }
 *             .onFailure { error -> state { failed(error) } }
 *     }
 * }
 *
 * class ProfileViewModel(...) :
 *     StateViewModel<ProfileState, ProfileStateBuilder>(owner, ::ProfileStateBuilder) {
 *
 *     val loader = ProfileLoaderDelegate(fetch = ::fetchProfile).also { it.initialize(this) }
 * }
 * ```
 *
 * @param T concrete [BaseState] shared with the owning [StateViewModel].
 * @param B the owner's builder type; mutated directly inside [state] blocks.
 */
abstract class StateViewModelDelegate<T : BaseState, B : BaseStateBuilder<T>> {
    private lateinit var owner: StateViewModel<T, B>

    /**
     * Binds this delegate to its [StateViewModel] owner.
     *
     * Must be called once before [task], [state], or [getActualState]. Calling a second time
     * silently replaces the owner.
     */
    fun initialize(owner: StateViewModel<T, B>) {
        this.owner = owner
    }

    /**
     * Launches [block] as a coroutine scoped to the owning ViewModel's lifecycle.
     *
     * Behaves identically to calling `task` on the ViewModel directly.
     */
    fun task(block: suspend CoroutineScope.() -> Unit) {
        owner.delegatedTask(block)
    }

    /**
     * Applies [updater] to the owner's builder and emits the result atomically.
     *
     * Equivalent to calling [StateViewModel.state] on the owner directly; exposed here so
     * delegates can mutate state without holding a reference to the owner's public API.
     *
     * @return the new state after [updater] was applied.
     */
    fun state(updater: B.() -> Unit): T = owner.state(updater)

    /**
     * Returns the current state snapshot synchronously.
     *
     * Prefer collecting [StateViewModel.state] as a Flow in the UI layer; use this only where a
     * suspend-free read of the latest value is required.
     */
    fun getActualState(): T {
        return owner.state.value
    }
}

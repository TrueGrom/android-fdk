package grmv.android.fdk.state.traits

import androidx.compose.runtime.Immutable
import grmv.android.fdk.state.BaseState
import grmv.android.fdk.state.BuilderOps

/**
 * State marker: a flag that, when true, signals the UI to disable interaction (clicks, edits)
 * for the duration the state remains locked.
 *
 * Implement on a state data class to opt in to [LockOps]:
 * ```
 * data class FormState(
 *     override val locked: Boolean = false,
 *     val name: String = "",
 * ) : LockableState<FormState> {
 *     override fun withLocked(locked: Boolean) = copy(locked = locked)
 * }
 * ```
 *
 * @param S concrete state type (F-bounded self type so [withLocked] returns [S], not the marker).
 */
@Immutable
interface LockableState<S : LockableState<S>> : BaseState {
    val locked: Boolean
    fun withLocked(locked: Boolean): S
}

/**
 * Reusable mutation surface for any [LockableState].
 *
 * Mix into a builder to gain `lock()` and `unlock()`:
 * ```
 * class FormStateBuilder(override val initial: FormState) :
 *     BaseStateBuilder<FormState>(),
 *     LockOps<FormState>
 * ```
 */
interface LockOps<S : LockableState<S>> : BuilderOps<S> {
    /** Sets [LockableState.locked] to `true`. */
    fun lock() = accumulate { it.withLocked(true) }

    /** Sets [LockableState.locked] to `false`. */
    fun unlock() = accumulate { it.withLocked(false) }
}

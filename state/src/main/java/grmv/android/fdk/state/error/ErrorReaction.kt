package grmv.android.fdk.state.error

import androidx.compose.runtime.Immutable

/**
 * Describes how an error should be presented to the user.
 *
 * The flow starts at [None], transitions to a [VisibleError] subtype when an error occurs,
 * and returns to [None] once the UI calls [ErrorEmitter.consumeError].
 *
 * @param Action type of the optional follow-up action available from error UI.
 */
@Immutable
sealed class ErrorReaction<out Action : Any> {

    /** No active error. Default/idle state. */
    @Immutable
    data object None : ErrorReaction<Nothing>()

    /** Implemented by reactions that carry an optional user-triggered follow-up [action]. */
    interface Actionable<out Action : Any> {
        /** Action to execute when the user taps the action button; `null` means no button shown. */
        val action: Action?
    }

    /** Base for all reactions that show visible error UI. */
    sealed class VisibleError<Action : Any> : ErrorReaction<Action>() {
        /** The original exception that triggered this reaction. */
        abstract val error: Throwable
    }

    /** Show the error in a modal dialog, optionally with an [action] button. */
    @Immutable
    data class Dialog<Action : Any>(
        override val error: Throwable,
        override val action: Action? = null,
    ) : VisibleError<Action>(), Actionable<Action>

    /** Show the error in a snackbar, optionally with an [action] button. */
    @Immutable
    data class SnackBar<Action : Any>(
        override val error: Throwable,
        override val action: Action? = null
    ) : VisibleError<Action>(), Actionable<Action>

    /** Show the error as a toast. No action supported. */
    @Immutable
    data class Toast(
        override val error: Throwable,
    ) : VisibleError<Nothing>()

}
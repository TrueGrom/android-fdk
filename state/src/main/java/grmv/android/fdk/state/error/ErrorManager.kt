package grmv.android.fdk.state.error

import grmv.android.fdk.coroutines.onError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Selects the presentation style for an error inside an [ErrorBuilder] lambda.
 *
 * [WithAction] subtypes support an optional follow-up action via [WithAction.withAction].
 * [Toast] carries no action.
 */
sealed class ErrorDisplay {

    /** Presentation styles that support an optional follow-up action. */
    sealed class WithAction<Action : Any> : ErrorDisplay() {
        /** The attached follow-up action, or `null` when no action button should be shown. */
        abstract val action: Action?

        /** Returns a copy of this display with [action] attached. */
        abstract fun withAction(action: Action): WithAction<Action>

        /** Presents the error as a snackbar with an optional action button. */
        data class SnackBar<Action : Any>(override val action: Action? = null) : WithAction<Action>() {
            override fun withAction(action: Action) = copy(action = action)
        }

        /** Presents the error as a modal dialog with an optional action button. */
        data class Dialog<Action : Any>(override val action: Action? = null) : WithAction<Action>() {
            override fun withAction(action: Action) = copy(action = action)
        }
    }

    /** Presents the error as a toast. No action supported. */
    data object Toast : ErrorDisplay()
}

/** DSL receiver for constructing an [ErrorReaction] inside [ErrorManager.showError]. */
interface ErrorBuilder<Action : Any> {
    /** Presents the error as a snackbar; chain [ErrorDisplay.WithAction.withAction] to attach an action. */
    fun snackbar(): ErrorDisplay.WithAction<Action>
    /** Presents the error as a dialog; chain [ErrorDisplay.WithAction.withAction] to attach an action. */
    fun dialog(): ErrorDisplay.WithAction<Action>
    /** Presents the error as a toast. No action supported. */
    fun toast(): ErrorDisplay.Toast
}

/** Convenience alias for [ErrorManager] when no actionable errors are needed. */
typealias Errors = ErrorManager<Nothing>

/**
 * Extends [ErrorEmitter] with the ability to push new error reactions into the UI.
 *
 * The typical pattern is to call [showError] (or the [visualError] infix extension) from a
 * ViewModel's coroutine scope, then let the UI layer observe [ErrorEmitter.errors] and call
 * [ErrorEmitter.consumeError] to reset back to [ErrorReaction.None].
 *
 * @param Action type of the optional follow-up action available from error UI. Use [Nothing] (via
 * the [Errors] alias) when no actionable errors are needed.
 */
interface ErrorManager<Action : Any> : ErrorEmitter<Action> {
    /**
     * Infix extension for `Result` chains: on failure, invokes [errorBuilder] with the exception
     * and emits the resulting [ErrorReaction]. On success, returns the [Result] unchanged.
     *
     * ```kotlin
     * repository.fetchData() visualError { snackbar() }
     * ```
     */
    infix fun <T> Result<T>.visualError(errorBuilder: ErrorBuilder<Action>.() -> ErrorDisplay): Result<T>

    /**
     * Immediately emits an [ErrorReaction] constructed from [error] and [builder].
     *
     * Safe to call from any coroutine context; the backing [MutableStateFlow] serialises updates.
     */
    fun showError(error: Throwable, builder: ErrorBuilder<Action>.() -> ErrorDisplay)
}

/**
 * Default [ErrorManager] implementation backed by a [MutableStateFlow].
 *
 * Subclass in ViewModels or presenters. Expose only the [ErrorEmitter] interface to the UI layer
 * so callers can observe and consume errors but cannot emit them directly.
 */
open class MutableErrorManager<Action : Any> : ErrorManager<Action> {
    private val _errors = MutableStateFlow<ErrorReaction<Action>>(ErrorReaction.None)
    override val errors = _errors.asStateFlow()

    override fun consumeError(event: ErrorReaction<Action>) {
        _errors.update { current ->
            if (current == event) {
                ErrorReaction.None
            } else {
                current
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun showError(error: Throwable, builder: ErrorBuilder<Action>.() -> ErrorDisplay) {
        _errors.value = when (val display = builder(ErrorBuilderImpl())) {
            is ErrorDisplay.Toast -> ErrorReaction.Toast(error)
            is ErrorDisplay.WithAction.SnackBar<*> -> ErrorReaction.SnackBar(error, display.action as Action?)
            is ErrorDisplay.WithAction.Dialog<*> -> ErrorReaction.Dialog(error, display.action as Action?)
        }
    }

    override fun <T> Result<T>.visualError(errorBuilder: ErrorBuilder<Action>.() -> ErrorDisplay): Result<T> {
        return onError { e ->
            showError(e, errorBuilder)
        }
    }
}

private class ErrorBuilderImpl<Action : Any> : ErrorBuilder<Action> {
    override fun snackbar() = ErrorDisplay.WithAction.SnackBar<Action>()
    override fun dialog() = ErrorDisplay.WithAction.Dialog<Action>()
    override fun toast() = ErrorDisplay.Toast
}
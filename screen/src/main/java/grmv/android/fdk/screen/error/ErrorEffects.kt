package grmv.android.fdk.screen.error

import android.widget.Toast
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import grmv.android.fdk.screen.ViewModelScope
import grmv.android.fdk.state.error.ErrorEmitter
import grmv.android.fdk.state.error.ErrorReaction
import grmv.android.fdk.viewmodel.BaseViewModel

/**
 * Text shown for an error: an optional [title] and a [text] body.
 */
@Immutable
data class ErrorMessage(
    val title: String?,
    val text: String,
)

/**
 * Renders the current [ErrorReaction] from this scope's [ViewModelScope.viewModel] as a dialog,
 * snackbar, or toast, then consumes it.
 *
 * Delegates to the [ErrorEmitter] overload; all parameters have identical semantics.
 * Prefer this overload inside a [ViewModelScreen] body where the ViewModel is already in scope.
 *
 * @param errorAction invoked with the reaction's follow-up action when the user triggers it.
 * @param snackbarHostState target host for snackbar reactions; falls back to a toast when `null`.
 * @param snackbarActionLabel label for the snackbar action button; only shown when an action exists.
 * @param errorMessage maps a [Throwable] to display text; defaults to [LocalErrorEffectsDefaults].
 * @param dialog slot for dialog presentation; defaults to [LocalErrorEffectsDefaults].
 */
@Composable
fun <VM, ErrorAction : Any> ViewModelScope<VM>.ErrorEffects(
    errorAction: ((ErrorAction) -> Unit)? = null,
    snackbarHostState: SnackbarHostState? = null,
    snackbarActionLabel: String? = null,
    errorMessage: @Composable (Throwable) -> ErrorMessage = { LocalErrorEffectsDefaults.current.errorMessage(it) },
    dialog: @Composable (message: ErrorMessage, onClose: () -> Unit) -> Unit = { message, onClose ->
        LocalErrorEffectsDefaults.current.Dialog(message, onClose)
    },
) where VM : BaseViewModel, VM : ErrorEmitter<ErrorAction> {
    viewModel.ErrorEffects(
        errorAction = errorAction,
        snackbarHostState = snackbarHostState,
        snackbarActionLabel = snackbarActionLabel,
        errorMessage = errorMessage,
        dialog = dialog,
    )
}

/**
 * Observes [ErrorEmitter.errors] and presents each [ErrorReaction] as UI, consuming it once shown.
 *
 * - [ErrorReaction.Dialog] is rendered with [dialog].
 * - [ErrorReaction.SnackBar] uses [snackbarHostState] when provided, otherwise falls back to a toast.
 * - [ErrorReaction.Toast] is shown as a system toast.
 *
 * @param errorAction invoked with the reaction's follow-up action when the user triggers it.
 * @param snackbarActionLabel label for the snackbar action button; only shown when an action exists.
 * @param errorMessage maps a [Throwable] to display text; defaults to [LocalErrorEffectsDefaults].
 * @param dialog slot for the dialog presentation; defaults to [LocalErrorEffectsDefaults].
 */
@Composable
fun <ErrorAction : Any> ErrorEmitter<ErrorAction>.ErrorEffects(
    errorAction: ((ErrorAction) -> Unit)? = null,
    snackbarHostState: SnackbarHostState? = null,
    snackbarActionLabel: String? = null,
    errorMessage: @Composable (Throwable) -> ErrorMessage = { LocalErrorEffectsDefaults.current.errorMessage(it) },
    dialog: @Composable (message: ErrorMessage, onClose: () -> Unit) -> Unit = { message, onClose ->
        LocalErrorEffectsDefaults.current.Dialog(message, onClose)
    },
) {
    val errorIntent by errors.collectAsStateWithLifecycle()
    when (val error = errorIntent) {
        ErrorReaction.None -> {}

        is ErrorReaction.Dialog<ErrorAction> -> {
            val message = errorMessage(error.error)
            dialog(message) {
                error.action?.let { errorAction?.invoke(it) }
                consumeError(errorIntent)
            }
        }

        is ErrorReaction.SnackBar<*> -> {
            val message = errorMessage(error.error)
            if (snackbarHostState != null) {
                LaunchedEffect(error) {
                    val actionLabel = if (error.action != null) snackbarActionLabel else null
                    val result = snackbarHostState.showSnackbar(
                        message = message.text,
                        actionLabel = actionLabel,
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        @Suppress("UNCHECKED_CAST")
                        (error as? ErrorReaction.SnackBar<ErrorAction>)?.action
                            ?.let { errorAction?.invoke(it) }
                    }
                    consumeError(errorIntent)
                }
            } else {
                ToastEffect(message = message.text, onShown = { consumeError(errorIntent) })
            }
        }

        is ErrorReaction.Toast -> {
            val message = errorMessage(error.error)
            ToastEffect(message = message.text, onShown = { consumeError(errorIntent) })
        }
    }
}

/** Shows [message] as a system toast exactly once and invokes [onShown]. */
@Composable
private fun ToastEffect(message: String, onShown: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        onShown()
    }
}

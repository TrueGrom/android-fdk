package grmv.android.fdk.screen.event

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import grmv.android.fdk.screen.ViewModelScope
import grmv.android.fdk.state.ActionEmitter
import grmv.android.fdk.viewmodel.BaseViewModel
import kotlinx.coroutines.CoroutineScope

/**
 * Collects one-shot screen actions from this scope's [ViewModelScope.viewModel] and dispatches
 * each to [onEvent], consuming it afterward.
 *
 * The generic consumer for custom actions. Standard events are handled by their own target
 * extension instead (e.g. `SnackbarManager.consumeEvents` for snackbar events); when layering both
 * on one screen, leave those events unhandled here. Delegates to the [ActionEmitter] overload;
 * prefer this one inside a [ViewModelScreen] body where the ViewModel is already in scope.
 *
 * @param VM concrete [BaseViewModel] that also implements [ActionEmitter].
 * @param Event action type emitted by the ViewModel.
 * @param onEvent handler invoked with each action; runs in the collecting [CoroutineScope].
 */
@Composable
fun <VM, Event : Any> ViewModelScope<VM>.EventEffects(
    onEvent: CoroutineScope.(Event) -> Unit,
) where VM : BaseViewModel, VM : ActionEmitter<Event> {
    viewModel.EventEffects(onEvent = onEvent)
}

/**
 * Collects one-shot screen actions from [this] emitter and dispatches each to [onEvent],
 * consuming it afterward.
 *
 * Collection is lifecycle-aware via [collectAsStateWithLifecycle]: it pauses below `STARTED` and
 * resumes on return; because `screenActions` is a conflated `StateFlow`, the latest pending action is
 * re-observed on resume, so nothing fires while the screen is backgrounded and no action is lost.
 * Each non-null action is dispatched from a [LaunchedEffect] keyed on it, then consumed — which
 * clears the flow and ends the effect. The [onEvent] lambda is wrapped in [rememberUpdatedState] so
 * recompositions with a new lambda are picked up without restarting the current dispatch.
 *
 * @param Event action type emitted by the [ActionEmitter].
 * @param onEvent handler invoked with each action; runs in the collecting [CoroutineScope].
 */
@Composable
fun <Event : Any> ActionEmitter<Event>.EventEffects(
    onEvent: CoroutineScope.(Event) -> Unit,
) {
    val handler by rememberUpdatedState(onEvent)
    val event by screenActions.collectAsStateWithLifecycle()
    event?.let { current ->
        LaunchedEffect(current) {
            handler(this, current)
            consumeAction(current)
        }
    }
}

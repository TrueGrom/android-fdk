package grmv.android.fdk.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import grmv.android.fdk.viewmodel.BaseViewModel

/**
 * [ScreenScope] that exposes the screen's [viewModel].
 *
 * Receiver for the effect composables ([ErrorEffects], [EventEffects]) so they can observe the
 * ViewModel without it being passed explicitly. [EventEffects] consumes custom actions; standard
 * events are consumed by their own target (e.g. `SnackbarManager.consumeEvents`).
 *
 * @param VM concrete [BaseViewModel] backing the screen.
 */
@Immutable
interface ViewModelScope<VM> : ScreenScope where VM : BaseViewModel {
    /** The ViewModel backing the current screen. */
    val viewModel: VM
}

/**
 * Builds a [ViewModelScope] for [content], retrieving [viewModel] from Hilt by default.
 *
 * @param viewModel the screen's ViewModel; defaults to [hiltViewModel].
 * @param content screen body invoked with the [ViewModelScope] receiver.
 */
@Composable
inline fun <reified VM : BaseViewModel> ViewModelScreen(
    viewModel: VM = hiltViewModel(),
    crossinline content: @Composable ViewModelScope<VM>.() -> Unit,
) {
    val scope = remember(viewModel) {
        object : ViewModelScope<VM> {
            override val viewModel: VM = viewModel
        }
    }
    content.invoke(scope)
}

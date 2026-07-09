package grmv.android.fdk.screen.snackbar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import grmv.android.fdk.state.ActionEmitter

/**
 * Marks a one-shot screen event that should surface as a snackbar.
 *
 * Implement it on your event type to declare — through the [SnackbarBuilder] DSL — the text,
 * action label, duration, and result callbacks of the snackbar shown when the event is emitted.
 * `EventEffects` (given a [SnackbarManager]) renders it automatically; events that do not
 * implement this interface fall through to your own handler, so a screen's action stream may
 * freely mix snackbar events with navigation and custom events.
 *
 * ```
 * sealed interface HomeEvent {
 *     data class Failed(val reason: String) : HomeEvent, SnackbarEvent {
 *         override fun SnackbarBuilder.snackbar() {
 *             message(reason)
 *             actionLabel(R.string.fdk_action_retry)
 *             onAction { /* retry */ }
 *         }
 *     }
 * }
 * ```
 */
interface SnackbarEvent {
    /** Configures the snackbar shown for this event using the [SnackbarBuilder] DSL. */
    fun SnackbarBuilder.snackbar()
}

/**
 * Shows the snackbar declared by [event] through its [SnackbarEvent.snackbar] configuration.
 *
 * Convenience over [SnackbarManager.showSnackbar]; no-ops when the event sets no message text.
 */
fun SnackbarManager.showSnackbar(event: SnackbarEvent) =
    showSnackbar { with(event) { snackbar() } }

/**
 * Observes [emitter]'s one-shot actions and shows every [SnackbarEvent] on this manager.
 *
 * A snackbar-target consumer: it reacts to — and consumes — only [SnackbarEvent]s, leaving all
 * other actions pending for the generic `EventEffects` (or another target consumer). In a
 * `ViewModelScreen`, pass the ViewModel as [emitter].
 *
 * @param Event action type emitted by [emitter].
 * @param emitter source of one-shot screen actions.
 */
@Composable
fun <Event : Any> SnackbarManager.ConsumeEvents(emitter: ActionEmitter<Event>) {
    LaunchedEffect(this, emitter) {
        emitter.screenActions.collect { event ->
            if (event is SnackbarEvent) {
                showSnackbar(event)
                emitter.consumeAction(event)
            }
        }
    }
}

package grmv.android.fdk.screen.snackbar

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Immutable description of a snackbar to display.
 *
 * All fields are internal; construct instances only via the [SnackbarBuilder] DSL exposed by
 * [SnackbarManager.showSnackbar].
 */
@ConsistentCopyVisibility
data class SnackbarMessage internal constructor(
    internal val message: String,
    internal val actionLabel: String? = null,
    internal val withDismissAction: Boolean = false,
    internal val duration: SnackbarDuration,
    internal val onAction: (() -> Unit)? = null,
    internal val onDismiss: (() -> Unit)? = null,
)

/** DSL receiver for assembling a [SnackbarMessage] inside [SnackbarManager.showSnackbar]. */
interface SnackbarBuilder {
    /** Sets the snackbar text. */
    fun message(text: String)

    /** Sets the snackbar text from a string resource. */
    fun message(@StringRes resId: Int)

    /** Sets the action button label. */
    fun actionLabel(text: String)

    /** Sets the action button label from a string resource. */
    fun actionLabel(@StringRes resId: Int)

    /**
     * Sets how long the snackbar stays visible.
     *
     * Wins over the action-derived default: without this call a snackbar shows for
     * [SnackbarDuration.Short] when it has no [actionLabel] and [SnackbarDuration.Long] when it
     * has one. A snackbar is the only place its action exists, so one that offers something to
     * tap has to outlive one that merely reports. Material3 draws the same line and goes further,
     * to `Indefinite`; [SnackbarDuration.Long] stops short of that so an ignored undo still leaves
     * on its own. [withDismissAction] does not enter into it — a dismiss affordance is a way out
     * of the snackbar, not a reason to keep it around.
     */
    fun duration(duration: SnackbarDuration)

    /** Shows a dismiss affordance on the snackbar. */
    fun withDismissAction()

    /** Callback invoked when the action button is tapped. */
    fun onAction(action: () -> Unit)

    /** Callback invoked when the snackbar is dismissed. */
    fun onDismiss(action: () -> Unit)
}

/**
 * Creates and remembers a [SnackbarHostState] for the current composition.
 *
 * Pass the returned state to [rememberSnackbarManager] and [ScreenSnackbarHost].
 */
@Composable
fun rememberSnackbarHostState(): SnackbarHostState = remember { SnackbarHostState() }

/**
 * UI-only gateway for showing snack bars.
 *
 * Created and remembered in the composition via [rememberSnackbarManager]; it owns a
 * [SnackbarHostState] (render it with [ScreenSnackbarHost]) and shows messages on its own
 * [CoroutineScope]. Snack bars live entirely in the UI layer — ViewModels never hold a manager;
 * they emit one-shot [SnackbarEvent]s instead, which [ConsumeEvents] turns into snack bars.
 */
@Stable
class SnackbarManager internal constructor(
    val hostState: SnackbarHostState,
    private val scope: CoroutineScope,
    private val context: Context,
) {
    /**
     * Builds a [SnackbarMessage] via the [SnackbarBuilder] DSL and shows it on [hostState].
     *
     * No-ops when [builder] sets no [SnackbarBuilder.message] text. Shows on the manager's
     * [CoroutineScope] and dispatches [SnackbarBuilder.onAction] / [SnackbarBuilder.onDismiss]
     * according to the result.
     *
     * @param builder configures the message text, action, duration, and result callbacks.
     */
    fun showSnackbar(builder: SnackbarBuilder.() -> Unit) {
        val message = SnackbarBuilderImpl(context).apply(builder).build()
        if (message.message.isEmpty()) return
        scope.launch {
            val result = hostState.showSnackbar(
                message = message.message,
                actionLabel = message.actionLabel,
                withDismissAction = message.withDismissAction,
                duration = message.duration,
            )
            when (result) {
                SnackbarResult.ActionPerformed -> message.onAction?.invoke()
                SnackbarResult.Dismissed -> message.onDismiss?.invoke()
            }
        }
    }
}

/**
 * Creates and remembers a UI-only [SnackbarManager] for the current composition.
 *
 * @param hostState backing host state; defaults to a remembered [rememberSnackbarHostState].
 */
@Composable
fun rememberSnackbarManager(
    hostState: SnackbarHostState = rememberSnackbarHostState(),
): SnackbarManager {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return remember(hostState, scope, context) { SnackbarManager(hostState, scope, context) }
}

/**
 * Material3 [SnackbarHost] styled for screen-level use.
 *
 * Drop this into [FdKitBaseScaffold]'s `snackbarHost` slot alongside a [rememberSnackbarHostState].
 * Internally renders each snackbar with the default Material3 [Snackbar] visuals.
 *
 * Rides above the keyboard: the slot is composed outside the scaffold body, so nothing has consumed
 * the ime inset there and [Modifier.imePadding] applies it in full. Without this a form's own
 * validation message — the snackbar most likely to be shown while the keyboard is open — would
 * render behind it. Deliberately always on, independent of `FdKitBaseScaffold`'s `avoidKeyboard`: that
 * flag governs where the *body* ends, and a message that cannot be read is not a layout preference.
 * Pass a `modifier` that consumes the inset to override.
 *
 * @param hostState state object that controls which snackbar is visible.
 * @param modifier applied before the keyboard padding.
 */
@Composable
fun ScreenSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(hostState = hostState, modifier = modifier.imePadding()) { data ->
        Snackbar(snackbarData = data)
    }
}

/**
 * [ScreenSnackbarHost] backed by [manager]'s [SnackbarManager.hostState].
 *
 * @param manager UI gateway whose host state controls which snackbar is visible.
 */
@Composable
fun ScreenSnackbarHost(
    manager: SnackbarManager,
    modifier: Modifier = Modifier,
) {
    ScreenSnackbarHost(hostState = manager.hostState, modifier = modifier)
}

internal class SnackbarBuilderImpl(private val context: Context) : SnackbarBuilder {
    private var message: String = ""
    private var actionLabel: String? = null
    private var withDismissAction: Boolean = false
    private var duration: SnackbarDuration? = null
    private var onAction: (() -> Unit)? = null
    private var onDismiss: (() -> Unit)? = null

    override fun message(text: String) { message = text }
    override fun message(@StringRes resId: Int) { message = context.getString(resId) }
    override fun actionLabel(text: String) { actionLabel = text }
    override fun actionLabel(@StringRes resId: Int) { actionLabel = context.getString(resId) }
    override fun duration(duration: SnackbarDuration) { this.duration = duration }
    override fun withDismissAction() { withDismissAction = true }
    override fun onAction(action: () -> Unit) { onAction = action }
    override fun onDismiss(action: () -> Unit) { onDismiss = action }

    fun build() = SnackbarMessage(
        message = message,
        actionLabel = actionLabel,
        withDismissAction = withDismissAction,
        duration = duration ?: defaultDuration(),
        onAction = onAction,
        onDismiss = onDismiss,
    )

    /** Duration used when the caller set none; the rule is documented on [SnackbarBuilder.duration]. */
    private fun defaultDuration(): SnackbarDuration =
        if (actionLabel == null) SnackbarDuration.Short else SnackbarDuration.Long
}

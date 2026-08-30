package grmv.android.fdk.screen

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ApplicationInfo
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import grmv.android.fdk.logging.FdkLog

/**
 * Warns once per process, in a debuggable build, when the host window declares a soft-input mode
 * that fights the keyboard padding `FdKitBaseScaffold` applies.
 *
 * `adjustPan` makes the framework shift the whole window up for the keyboard. On API 30+ the ime
 * inset is dispatched as well, so the content moves twice; below that the inset is never reported
 * under a panning window, so [FdKitBaseScaffold]'s `avoidKeyboard` silently does nothing. Either way
 * the symptom shows only on a device with a focused field, which is the class of bug that padding
 * exists to remove — and the SDK cannot fix it, because `windowSoftInputMode` lives in the
 * consumer's manifest.
 *
 * Scope is narrow on purpose: this sees only what the manifest (or an explicit `setSoftInputMode`)
 * declared. An app that declares nothing reports `SOFT_INPUT_ADJUST_UNSPECIFIED`, which
 * `ViewRootImpl` resolves at traversal time — to panning, for a hierarchy with no scroll container,
 * which a pure Compose window is — into a copy of the attributes that `Window.getAttributes` never
 * sees. That case is invisible here, and warning on `UNSPECIFIED` outright would fire on every app
 * that never had a problem, so it does not.
 *
 * No-op in a non-debuggable build, when [avoidKeyboard] is off (nothing to conflict with), and for
 * an app that installed no [FdkLog] sink.
 */
@Composable
internal fun WarnOnConflictingSoftInputMode(avoidKeyboard: Boolean) {
    val context = LocalContext.current
    LaunchedEffect(context, avoidKeyboard) {
        if (!avoidKeyboard || warned) return@LaunchedEffect
        val activity = context.findActivity() ?: return@LaunchedEffect
        if (!activity.isDebuggable()) return@LaunchedEffect
        val adjust = activity.window?.attributes?.softInputMode
            ?.and(WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST)
            ?: return@LaunchedEffect
        if (adjust == WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN) {
            warned = true
            FdkLog.tagged(TAG).w(
                "%s declares windowSoftInputMode=adjustPan, which conflicts with avoidKeyboard: the " +
                    "window pans for the keyboard, so content either shifts twice (API 30+) or the " +
                    "ime inset is never reported and the padding does nothing. Use adjustResize, or " +
                    "pass avoidKeyboard = false.",
                activity::class.java.simpleName,
            )
        }
    }
}

private const val TAG = "FdKitBaseScaffold"

/** Warn only on the first offending scaffold: the mode is a property of the window, not the screen. */
@Volatile
private var warned = false

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

private fun Activity.isDebuggable(): Boolean =
    applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

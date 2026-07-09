package grmv.android.fdk.state.error

import java.util.concurrent.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ErrorManagerTest {

    private sealed class Action {
        data object Retry : Action()
        data object Dismiss : Action()
    }

    // --- initial state ---

    @Test
    fun `errors - new manager - is None`() {
        val manager = MutableErrorManager<Action>()
        assertEquals(ErrorReaction.None, manager.errors.value)
    }

    // --- showError display selection ---

    @Test
    fun `showError - snackbar - emits SnackBar with the throwable`() {
        val manager = MutableErrorManager<Action>()
        val cause = RuntimeException("net")
        manager.showError(cause) { snackbar() }
        val current = manager.errors.value
        assertTrue(current is ErrorReaction.SnackBar)
        assertSame(cause, (current as ErrorReaction.SnackBar).error)
        assertNull(current.action)
    }

    @Test
    fun `showError - dialog - emits Dialog with the throwable`() {
        val manager = MutableErrorManager<Action>()
        val cause = IllegalStateException("bad")
        manager.showError(cause) { dialog() }
        val current = manager.errors.value
        assertTrue(current is ErrorReaction.Dialog)
        assertSame(cause, (current as ErrorReaction.Dialog).error)
        assertNull(current.action)
    }

    // --- withAction ---

    @Test
    fun `showError - snackbar with action - attaches action`() {
        val manager = MutableErrorManager<Action>()
        manager.showError(RuntimeException()) { snackbar().withAction(Action.Retry) }
        val current = manager.errors.value as ErrorReaction.SnackBar
        assertEquals(Action.Retry, current.action)
    }

    @Test
    fun `showError - dialog with action - attaches action`() {
        val manager = MutableErrorManager<Action>()
        manager.showError(RuntimeException()) { dialog().withAction(Action.Dismiss) }
        val current = manager.errors.value as ErrorReaction.Dialog
        assertEquals(Action.Dismiss, current.action)
    }

    @Test
    fun `showError - toast - emits Toast with the throwable`() {
        val manager = MutableErrorManager<Action>()
        val cause = RuntimeException("toast")
        manager.showError(cause) { toast() }
        val current = manager.errors.value
        assertTrue(current is ErrorReaction.Toast)
        assertSame(cause, (current as ErrorReaction.Toast).error)
    }

    // --- consumeError ---

    @Test
    fun `consumeError - matches current reaction - resets to None`() {
        val manager = MutableErrorManager<Action>()
        manager.showError(RuntimeException("e")) { snackbar() }
        val current = manager.errors.value
        manager.consumeError(current)
        assertEquals(ErrorReaction.None, manager.errors.value)
    }

    @Test
    fun `consumeError - does not match current - leaves state unchanged`() {
        val manager = MutableErrorManager<Action>()
        manager.showError(RuntimeException("a")) { snackbar() }
        val before = manager.errors.value
        manager.consumeError(ErrorReaction.Toast(RuntimeException("b")))
        assertSame(before, manager.errors.value)
    }

    @Test
    fun `consumeError - while None - stays None`() {
        val manager = MutableErrorManager<Action>()
        manager.consumeError(ErrorReaction.Toast(RuntimeException()))
        assertEquals(ErrorReaction.None, manager.errors.value)
    }

    @Test
    fun `consumeError - then new error - emits new reaction`() {
        val manager = MutableErrorManager<Action>()
        manager.showError(RuntimeException("one")) { snackbar() }
        manager.consumeError(manager.errors.value)
        val second = IllegalStateException("two")
        manager.showError(second) { dialog() }
        val current = manager.errors.value as ErrorReaction.Dialog
        assertSame(second, current.error)
    }

    // --- visualError ---

    @Test
    fun `visualError - success result - returns same result and emits nothing`() {
        val manager = MutableErrorManager<Action>()
        val original = Result.success(42)
        val out = with(manager) { original.visualError { snackbar() } }
        assertTrue(out.isSuccess)
        assertEquals(42, out.getOrNull())
        assertEquals(ErrorReaction.None, manager.errors.value)
    }

    @Test
    fun `visualError - non-cancellation failure - emits reaction and returns failure unchanged`() {
        val manager = MutableErrorManager<Action>()
        val cause = RuntimeException("fail")
        val out = with(manager) {
            Result.failure<Int>(cause).visualError { snackbar().withAction(Action.Retry) }
        }
        assertTrue(out.isFailure)
        assertSame(cause, out.exceptionOrNull())
        val current = manager.errors.value as ErrorReaction.SnackBar
        assertSame(cause, current.error)
        assertEquals(Action.Retry, current.action)
    }

    @Test
    fun `visualError - cancellation failure - rethrows without emitting`() {
        val manager = MutableErrorManager<Action>()
        var builderCalled = false
        val caught = runCatching {
            with(manager) {
                Result.failure<Int>(CancellationException("cancel"))
                    .visualError {
                        builderCalled = true
                        snackbar()
                    }
            }
        }
        assertTrue(caught.isFailure)
        assertTrue(caught.exceptionOrNull() is CancellationException)
        assertFalse(builderCalled)
        assertEquals(ErrorReaction.None, manager.errors.value)
    }
}

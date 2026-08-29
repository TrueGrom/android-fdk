package grmv.android.fdk.state

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RefreshControllerTest {

    @Test
    fun `refreshing - false before any refresh`() {
        val controller = RefreshController()
        assertFalse(controller.refreshing.value)
    }

    @Test
    fun `refresh - flag true while work runs and false after`() = runTest {
        val controller = RefreshController()
        val started = CompletableDeferred<Unit>()
        val gate = CompletableDeferred<Unit>()
        controller.initialize(this) {
            started.complete(Unit)
            gate.await()
        }

        controller.refresh()
        runCurrent()
        // The flag is raised synchronously in refresh(), so assert the work is really in flight.
        assertTrue(started.isCompleted)
        assertTrue(controller.refreshing.value)

        gate.complete(Unit)
        advanceUntilIdle()
        assertFalse(controller.refreshing.value)
    }

    @Test
    fun `refresh - calls while in flight are ignored`() = runTest {
        val controller = RefreshController()
        val gate = CompletableDeferred<Unit>()
        var invocations = 0
        controller.initialize(this) {
            invocations++
            gate.await()
        }

        controller.refresh()
        runCurrent()
        controller.refresh()
        controller.refresh()
        runCurrent()

        assertEquals(1, invocations)
        assertTrue(controller.refreshing.value)

        gate.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun `refresh - runs again after completion`() = runTest {
        val controller = RefreshController()
        val firstRun = CompletableDeferred<Unit>()
        val secondRun = CompletableDeferred<Unit>()
        var invocations = 0
        controller.initialize(this) {
            invocations++
            if (invocations == 1) firstRun.await() else secondRun.await()
        }

        controller.refresh()
        runCurrent()
        firstRun.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, invocations)
        assertFalse(controller.refreshing.value)

        controller.refresh()
        runCurrent()
        assertEquals(2, invocations)
        assertTrue(controller.refreshing.value)

        secondRun.complete(Unit)
        advanceUntilIdle()
        assertFalse(controller.refreshing.value)
    }

    @Test
    fun `refresh - work that throws - resets the flag and propagates to the scope`() = runTest {
        val controller = RefreshController()
        var caught: Throwable? = null
        val scope = CoroutineScope(
            StandardTestDispatcher(testScheduler) + CoroutineExceptionHandler { _, e -> caught = e },
        )
        controller.initialize(scope) { error("boom") }

        controller.refresh()
        advanceUntilIdle()

        assertFalse(controller.refreshing.value)
        assertTrue(caught is IllegalStateException)
    }

    @Test
    fun `refresh - scope already cancelled - does not leave the flag raised`() = runTest {
        val controller = RefreshController()
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
        controller.initialize(scope) { }
        scope.cancel()

        controller.refresh()
        advanceUntilIdle()

        // The coroutine body never runs on a cancelled scope, so the reset cannot live inside it.
        assertFalse(controller.refreshing.value)
    }

    @Test
    fun `refresh - before initialize throws`() {
        val controller = RefreshController()
        assertThrows(UninitializedPropertyAccessException::class.java) {
            controller.refresh()
        }
    }
}

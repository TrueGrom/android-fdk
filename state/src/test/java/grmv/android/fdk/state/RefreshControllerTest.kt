package grmv.android.fdk.state

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
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
        val gate = CompletableDeferred<Unit>()
        controller.initialize(CoroutineScope(StandardTestDispatcher(testScheduler))) { gate.await() }

        controller.refresh()
        runCurrent()
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
        controller.initialize(CoroutineScope(StandardTestDispatcher(testScheduler))) {
            invocations++
            gate.await()
        }

        controller.refresh()
        runCurrent()
        controller.refresh()
        controller.refresh()
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, invocations)
    }

    @Test
    fun `refresh - runs again after completion`() = runTest {
        val controller = RefreshController()
        var invocations = 0
        controller.initialize(CoroutineScope(StandardTestDispatcher(testScheduler))) { invocations++ }

        controller.refresh()
        advanceUntilIdle()
        controller.refresh()
        advanceUntilIdle()

        assertEquals(2, invocations)
        assertFalse(controller.refreshing.value)
    }

    @Test
    fun `refresh - work that throws still resets the flag`() = runTest {
        val controller = RefreshController()
        val scope = CoroutineScope(
            StandardTestDispatcher(testScheduler) + SupervisorJob() + CoroutineExceptionHandler { _, _ -> },
        )
        controller.initialize(scope) { error("boom") }

        controller.refresh()
        advanceUntilIdle()

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

package grmv.android.fdk.screen.snackbar

import androidx.compose.material3.SnackbarHostState
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SnackbarManagerTest {

    private val hostState = SnackbarHostState()

    private fun TestScope.manager() = SnackbarManager(hostState, backgroundScope, mockk())

    @Test
    fun `cancel visible - dismisses it without callbacks`() = runTest {
        val calls = mutableListOf<String>()
        val job = manager().showSnackbar {
            message("A")
            onAction { calls += "action" }
            onDismiss { calls += "dismiss" }
        }
        runCurrent()
        assertEquals("A", hostState.currentSnackbarData?.visuals?.message)

        job.cancel()
        runCurrent()

        assertNull(hostState.currentSnackbarData)
        assertTrue(calls.isEmpty())
    }

    @Test
    fun `cancel queued - never shows it and fires no callbacks`() = runTest {
        val calls = mutableListOf<String>()
        val manager = manager()
        manager.showSnackbar {
            message("A")
            onDismiss { calls += "A dismiss" }
        }
        val queued = manager.showSnackbar {
            message("B")
            onAction { calls += "B action" }
            onDismiss { calls += "B dismiss" }
        }
        runCurrent()

        queued.cancel()
        hostState.currentSnackbarData!!.dismiss()
        runCurrent()

        assertNull(hostState.currentSnackbarData)
        assertEquals(listOf("A dismiss"), calls)
    }

    @Test
    fun `cancel right after the reader acts - still fires no callbacks`() = runTest {
        val calls = mutableListOf<String>()
        val job = manager().showSnackbar {
            message("A")
            actionLabel("Undo")
            onAction { calls += "action" }
            onDismiss { calls += "dismiss" }
        }
        runCurrent()

        hostState.currentSnackbarData!!.performAction()
        job.cancel()
        runCurrent()

        assertTrue(job.isCancelled)
        assertTrue(calls.isEmpty())
    }

    @Test
    fun `no message - returns a completed job`() = runTest {
        val job = manager().showSnackbar { actionLabel("Undo") }

        assertTrue(job.isCompleted)
        assertFalse(job.isCancelled)
        assertNull(hostState.currentSnackbarData)
    }
}

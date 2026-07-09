package grmv.android.fdk.coroutines

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CancellationTest {

    // --- rethrowCancellation ---

    @Test
    fun `rethrowCancellation - non-cancellation exception - does not throw`() {
        val ex = RuntimeException("boom")
        ex.rethrowCancellation() // must not propagate
    }

    @Test(expected = CancellationException::class)
    fun `rethrowCancellation - CancellationException - rethrows`() {
        val ex = CancellationException("cancelled")
        ex.rethrowCancellation()
    }

    @Test(expected = CancellationException::class)
    fun `rethrowCancellation - CancellationException subclass - rethrows`() {
        val ex = object : CancellationException("subclass") {}
        ex.rethrowCancellation()
    }

    // --- unwrapCancellation ---

    @Test
    fun `unwrapCancellation - success result - returns same value`() {
        val result = Result.success(42).unwrapCancellation()
        assertTrue(result.isSuccess)
        assertEquals(42, result.getOrNull())
    }

    @Test
    fun `unwrapCancellation - non-cancellation failure - returns failure without throwing`() {
        val cause = IllegalStateException("oops")
        val result = Result.failure<Int>(cause).unwrapCancellation()
        assertTrue(result.isFailure)
        assertEquals(cause, result.exceptionOrNull())
    }

    @Test(expected = CancellationException::class)
    fun `unwrapCancellation - CancellationException in failure - rethrows`() {
        Result.failure<Int>(CancellationException("stop")).unwrapCancellation()
    }

    // --- runCatchingRethrowCancellation ---

    @Test
    fun `runCatchingRethrowCancellation - block succeeds - returns success`() = runTest {
        val result = runCatchingRethrowCancellation { "value" }
        assertTrue(result.isSuccess)
        assertEquals("value", result.getOrNull())
    }

    @Test
    fun `runCatchingRethrowCancellation - block throws regular exception - returns failure`() = runTest {
        val cause = IllegalArgumentException("bad input")
        val result = runCatchingRethrowCancellation<String> { throw cause }
        assertTrue(result.isFailure)
        assertEquals(cause, result.exceptionOrNull())
    }

    @Test
    fun `runCatchingRethrowCancellation - block throws CancellationException - propagates`() = runTest {
        val caught = runCatching {
            runCatchingRethrowCancellation<String> { throw CancellationException("abort") }
        }
        assertTrue(caught.isFailure)
        assertTrue(caught.exceptionOrNull() is CancellationException)
    }

    // --- onError ---

    @Test
    fun `onError - success result - block is not called`() {
        var called = false
        Result.success("ok").onError { called = true }
        assertFalse(called)
    }

    @Test
    fun `onError - success result - returns original success`() {
        val result = Result.success("ok").onError { }
        assertTrue(result.isSuccess)
        assertEquals("ok", result.getOrNull())
    }

    @Test
    fun `onError - non-cancellation failure - calls block with the exception`() {
        val cause = RuntimeException("err")
        var received: Throwable? = null
        Result.failure<String>(cause).onError { received = it }
        assertEquals(cause, received)
    }

    @Test
    fun `onError - non-cancellation failure - returns original failure`() {
        val cause = RuntimeException("err")
        val result = Result.failure<String>(cause).onError { }
        assertTrue(result.isFailure)
        assertEquals(cause, result.exceptionOrNull())
    }

    @Test
    fun `onError - non-cancellation failure - block allows non-local return`() {
        fun subject(): String {
            Result.failure<String>(RuntimeException("err")).onError { return "non-local" }
            return "normal"
        }
        assertEquals("non-local", subject())
    }

    @Test
    fun `onError - CancellationException failure - rethrows without calling block`() {
        var called = false
        val caught = runCatching {
            Result.failure<String>(CancellationException("gone")).onError { called = true }
        }
        assertFalse(called)
        assertTrue(caught.isFailure)
        assertTrue(caught.exceptionOrNull() is CancellationException)
    }

    // --- onAnyResult ---

    @Test
    fun `onAnyResult - success - block invoked once and success preserved`() {
        var calls = 0
        val result = Result.success("v").onAnyResult { calls++ }
        assertEquals(1, calls)
        assertTrue(result.isSuccess)
        assertEquals("v", result.getOrNull())
    }

    @Test
    fun `onAnyResult - non-cancellation failure - block invoked once and failure preserved`() {
        val cause = RuntimeException("err")
        var calls = 0
        val result = Result.failure<String>(cause).onAnyResult { calls++ }
        assertEquals(1, calls)
        assertTrue(result.isFailure)
        assertEquals(cause, result.exceptionOrNull())
    }

    @Test
    fun `onAnyResult - CancellationException failure - rethrows without calling block`() {
        var called = false
        val caught = runCatching {
            Result.failure<String>(CancellationException("gone")).onAnyResult { called = true }
        }
        assertFalse(called)
        assertTrue(caught.isFailure)
        assertTrue(caught.exceptionOrNull() is CancellationException)
    }

    @Test
    fun `onAnyResult - block allows non-local return`() {
        fun subject(): String {
            Result.success("v").onAnyResult { return "non-local" }
            return "normal"
        }
        assertEquals("non-local", subject())
    }
}

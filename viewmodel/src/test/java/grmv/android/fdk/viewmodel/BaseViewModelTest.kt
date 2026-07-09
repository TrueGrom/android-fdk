package grmv.android.fdk.viewmodel

import java.util.concurrent.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BaseViewModelTest {

    private class TestViewModel : BaseViewModel() {
        fun runTask(block: suspend CoroutineScope.() -> Unit): Job = task(block)
        fun <T> runAsync(block: suspend CoroutineScope.() -> T): Deferred<T> = asyncTask(block)
        fun runUnique(id: Any, block: suspend CoroutineScope.() -> Unit): Job = uniqueTask(id, block)
        fun <T> runAsyncUnique(id: Any, block: suspend CoroutineScope.() -> T): Deferred<T> =
            asyncUniqueTask(id, block)

        fun <T> Result<T>.exposeHandledError(block: (Throwable) -> Unit): Result<T> =
            handledError(block)
    }

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    // --- task ---

    @Test
    fun `task - executes block and completes job`() = runTest {
        val vm = TestViewModel()
        var ran = false
        val job = vm.runTask { ran = true }
        job.join()
        assertTrue(ran)
        assertTrue(job.isCompleted)
    }

    // --- asyncTask ---

    @Test
    fun `asyncTask - returns Deferred resolving to block value`() = runTest {
        val vm = TestViewModel()
        val deferred = vm.runAsync { 42 }
        assertEquals(42, deferred.await())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `asyncTask - block throws - await rethrows`() = runTest {
        val vm = TestViewModel()
        val deferred = vm.runAsync<Int> { throw IllegalArgumentException("bad") }
        deferred.await()
    }

    // --- uniqueTask ---

    @Test
    fun `uniqueTask - same key second launch - cancels first job`() = runTest {
        val vm = TestViewModel()
        val first = vm.runUnique("key") {
            suspendCancellableCoroutine<Unit> { /* never resumes */ }
        }
        val second = vm.runUnique("key") { /* completes */ }
        second.join()
        assertTrue(first.isCancelled)
        assertTrue(second.isCompleted)
        assertFalse(second.isCancelled)
    }

    @Test
    fun `uniqueTask - different keys - both run independently`() = runTest {
        val vm = TestViewModel()
        var aRan = false
        var bRan = false
        val a = vm.runUnique("a") { aRan = true }
        val b = vm.runUnique("b") { bRan = true }
        a.join()
        b.join()
        assertTrue(aRan)
        assertTrue(bRan)
        assertFalse(a.isCancelled)
        assertFalse(b.isCancelled)
    }

    @Test
    fun `uniqueTask - same key after prior completion - new run is fresh job`() = runTest {
        val vm = TestViewModel()
        val first = vm.runUnique("key") { /* completes */ }
        first.join()
        val second = vm.runUnique("key") { /* completes */ }
        second.join()
        assertNotSame(first, second)
        assertFalse(second.isCancelled)
        assertTrue(second.isCompleted)
    }

    // --- asyncUniqueTask ---

    @Test
    fun `asyncUniqueTask - same key second launch - cancels prior Deferred`() = runTest {
        val vm = TestViewModel()
        val first = vm.runAsyncUnique("key") {
            suspendCancellableCoroutine<Int> { /* never resumes */ }
        }
        val second = vm.runAsyncUnique("key") { 7 }
        assertEquals(7, second.await())
        assertTrue(first.isCancelled)
    }

    @Test
    fun `asyncUniqueTask - keys differ - both produce results`() = runTest {
        val vm = TestViewModel()
        val a = vm.runAsyncUnique("a") { "A" }
        val b = vm.runAsyncUnique("b") { "B" }
        assertEquals("A", a.await())
        assertEquals("B", b.await())
    }

    // --- handledError ---

    @Test
    fun `handledError - success - block not invoked`() {
        val vm = TestViewModel()
        var called = false
        val result = with(vm) {
            Result.success("ok").exposeHandledError { called = true }
        }
        assertFalse(called)
        assertTrue(result.isSuccess)
        assertEquals("ok", result.getOrNull())
    }

    @Test
    fun `handledError - non-cancellation failure - block receives the exception`() {
        val vm = TestViewModel()
        val cause = IllegalStateException("err")
        var received: Throwable? = null
        val result = with(vm) {
            Result.failure<String>(cause).exposeHandledError { received = it }
        }
        assertSame(cause, received)
        assertTrue(result.isFailure)
        assertSame(cause, result.exceptionOrNull())
    }

    @Test
    fun `handledError - cancellation failure - rethrows and block is not called`() {
        val vm = TestViewModel()
        var called = false
        val caught = runCatching {
            with(vm) {
                Result.failure<String>(CancellationException("cancel"))
                    .exposeHandledError { called = true }
            }
        }
        assertTrue(caught.isFailure)
        assertTrue(caught.exceptionOrNull() is CancellationException)
        assertFalse(called)
    }
}

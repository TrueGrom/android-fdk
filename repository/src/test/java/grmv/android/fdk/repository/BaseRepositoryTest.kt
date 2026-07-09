package grmv.android.fdk.repository

import grmv.android.fdk.httperror.HttpError
import grmv.android.fdk.httperror.HttpErrorMapper
import grmv.android.fdk.utils.Either
import kotlin.coroutines.ContinuationInterceptor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BaseRepositoryTest {

    private class TestRepo(
        dispatchers: BaseDispatchers,
        errorMapper: HttpErrorMapper = { error("unexpected mapper call") }
    ) : BaseRepository(dispatchers, errorMapper) {
        suspend fun <T> exposeIo(block: suspend CoroutineScope.() -> T): T = ioContext(block)
        suspend fun <T : Any> exposeSafeCall(block: suspend () -> T): Either<HttpError, T> = httpSafeCall(block)
        suspend fun <T : Any> exposeHttp(block: suspend () -> T): T = http(block)
    }

    // --- ioContext: result propagation ---

    @Test
    fun `ioContext - returns block result`() = runTest {
        val repo = TestRepo(BaseDispatchers(io = UnconfinedTestDispatcher(testScheduler)))
        val value = repo.exposeIo { 99 }
        assertEquals(99, value)
    }

    // --- ioContext: dispatcher routing ---

    @Test
    fun `ioContext - block runs on the configured io dispatcher`() = runTest {
        val ioDispatcher = StandardTestDispatcher(testScheduler)
        val repo = TestRepo(BaseDispatchers(io = ioDispatcher))
        var capturedInterceptor: ContinuationInterceptor? = null
        launch {
            repo.exposeIo {
                capturedInterceptor = this.coroutineContext[ContinuationInterceptor]
            }
        }
        advanceUntilIdle()
        assertSame(ioDispatcher, capturedInterceptor)
    }

    // --- ioContext: exception propagation ---

    @Test(expected = IllegalStateException::class)
    fun `ioContext - block throws - exception propagates to caller`() = runTest {
        val repo = TestRepo(BaseDispatchers(io = UnconfinedTestDispatcher(testScheduler)))
        repo.exposeIo { throw IllegalStateException("io error") }
    }

    // --- ioContext: cancellation propagation ---

    @Test
    fun `ioContext - outer job cancelled - block observes CancellationException`() = runTest {
        val ioDispatcher = StandardTestDispatcher(testScheduler)
        val repo = TestRepo(BaseDispatchers(io = ioDispatcher))
        var observed: Throwable? = null
        val job = launch {
            repo.exposeIo {
                try {
                    awaitCancellation()
                } catch (t: Throwable) {
                    observed = t
                    throw t
                }
            }
        }
        runCurrent()
        job.cancel()
        advanceUntilIdle()
        assertTrue(observed is CancellationException)
        assertTrue(job.isCancelled)
    }

    // --- httpSafeCall: success ---

    @Test
    fun `httpSafeCall - success - returns Either Right with value`() = runTest {
        val repo = TestRepo(BaseDispatchers(io = UnconfinedTestDispatcher(testScheduler)))
        val result = repo.exposeSafeCall { "ok" }
        assertTrue(result is Either.Right)
        assertEquals("ok", (result as Either.Right).value)
    }

    // --- httpSafeCall: error mapping ---

    @Test
    fun `httpSafeCall - exception - returns Either Left with mapped HttpError`() = runTest {
        val mapped = HttpError.NetworkError(cause = RuntimeException("timeout"))
        val repo = TestRepo(
            BaseDispatchers(io = UnconfinedTestDispatcher(testScheduler)),
            errorMapper = { mapped }
        )
        val result = repo.exposeSafeCall<String> { throw RuntimeException("timeout") }
        assertTrue(result is Either.Left)
        assertSame(mapped, (result as Either.Left).value)
    }

    // --- httpSafeCall: CancellationException passthrough ---

    @Test
    fun `httpSafeCall - CancellationException - rethrown without calling mapper`() = runTest {
        var mapperCalled = false
        val repo = TestRepo(
            BaseDispatchers(io = UnconfinedTestDispatcher(testScheduler)),
            errorMapper = { mapperCalled = true; HttpError.UnknownError() }
        )
        var caught: Throwable? = null
        launch {
            try {
                repo.exposeSafeCall<String> { throw CancellationException("cancelled") }
            } catch (e: CancellationException) {
                caught = e
                throw e
            }
        }
        advanceUntilIdle()
        assertTrue(caught is CancellationException)
        assertFalse(mapperCalled)
    }

    // --- http: success ---

    @Test
    fun `http - success - returns unwrapped value`() = runTest {
        val repo = TestRepo(BaseDispatchers(io = UnconfinedTestDispatcher(testScheduler)))
        val result = repo.exposeHttp { 42 }
        assertEquals(42, result)
    }

    // --- http: error ---

    @Test
    fun `http - exception - throws the HttpError produced by mapper`() = runTest {
        val mapped = HttpError.NetworkError(cause = RuntimeException("net"))
        val repo = TestRepo(
            BaseDispatchers(io = UnconfinedTestDispatcher(testScheduler)),
            errorMapper = { mapped }
        )
        var thrown: Throwable? = null
        try {
            repo.exposeHttp<String> { throw RuntimeException("net") }
        } catch (e: HttpError) {
            thrown = e
        }
        assertSame(mapped, thrown)
    }
}

package grmv.android.fdk.coroutines

import kotlin.coroutines.ContinuationInterceptor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DispatchersTest {

    // --- CoroutineDispatcher.context ---

    @Test
    fun `context - block returns value - result is propagated`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val result = dispatcher.context { 99 }
        assertEquals(99, result)
    }

    @Test
    fun `context - block runs on the given dispatcher`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        var captured: ContinuationInterceptor? = null
        launch {
            dispatcher.context {
                captured = coroutineContext[ContinuationInterceptor]
            }
        }
        advanceUntilIdle()
        assertSame(dispatcher, captured)
    }

    @Test(expected = IllegalStateException::class)
    fun `context - block throws - exception propagates to caller`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        dispatcher.context<Unit> { throw IllegalStateException("block error") }
    }
}

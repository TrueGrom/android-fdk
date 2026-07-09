package grmv.android.fdk.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteDataExtTest {

    // --- unwrap ---

    @Test
    fun `unwrap - fetched - returns payload`() {
        val data: RemoteData<Int> = RemoteData.fetched(42)
        assertEquals(42, data.unwrap())
    }

    @Test(expected = ClassCastException::class)
    fun `unwrap - loading - throws`() {
        RemoteData.loading().unwrap()
    }

    @Test(expected = ClassCastException::class)
    fun `unwrap - error - throws`() {
        val data: RemoteData<Int> = RemoteData.error(RuntimeException())
        data.unwrap()
    }

    // --- fetchedOrDefault ---

    @Test
    fun `fetchedOrDefault - fetched - returns payload and skips default`() {
        var defaultCalled = false
        val data: RemoteData<String> = RemoteData.fetched("x")
        val result = data.fetchedOrDefault {
            defaultCalled = true
            "fallback"
        }
        assertEquals("x", result)
        assertFalse(defaultCalled)
    }

    @Test
    fun `fetchedOrDefault - loading - returns default`() {
        val data: RemoteData<String> = RemoteData.loading()
        assertEquals("fallback", data.fetchedOrDefault { "fallback" })
    }

    @Test
    fun `fetchedOrDefault - error - returns default`() {
        val data: RemoteData<String> = RemoteData.error(RuntimeException())
        assertEquals("fallback", data.fetchedOrDefault { "fallback" })
    }

    // --- isFetched ---

    @Test
    fun `isFetched - fetched - true`() {
        assertTrue(RemoteData.fetched(1).isFetched())
    }

    @Test
    fun `isFetched - loading - false`() {
        assertFalse(RemoteData.loading().isFetched())
    }

    @Test
    fun `isFetched - error - false`() {
        assertFalse(RemoteData.error(RuntimeException()).isFetched())
    }

    @Test
    fun `isFetched - smart-casts receiver on true`() {
        val data: RemoteData<Int> = RemoteData.fetched(7)
        // The contract lets the compiler smart-cast `data` to Fetched after the guard.
        val value = if (data.isFetched()) data.data else -1
        assertEquals(7, value)
    }

    // --- mustBeFetched ---

    @Test
    fun `mustBeFetched - fetched - returns Fetched wrapper`() {
        val data: RemoteData<Int> = RemoteData.fetched(99)
        assertEquals(99, data.mustBeFetched().data)
    }

    @Test(expected = ClassCastException::class)
    fun `mustBeFetched - loading - throws`() {
        RemoteData.loading().mustBeFetched()
    }

    // --- ifFetched ---

    @Test
    fun `ifFetched - fetched - invokes action with payload`() {
        var captured: Int? = null
        RemoteData.fetched(5).ifFetched { captured = it }
        assertEquals(5, captured)
    }

    @Test
    fun `ifFetched - loading - does not invoke`() {
        var called = false
        RemoteData.loading().ifFetched { called = true }
        assertFalse(called)
    }

    @Test
    fun `ifFetched - error - does not invoke`() {
        var called = false
        val data: RemoteData<Int> = RemoteData.error(RuntimeException())
        data.ifFetched { called = true }
        assertFalse(called)
    }
}

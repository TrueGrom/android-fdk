package grmv.android.fdk.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteDataTest {

    // --- companion factories ---

    @Test
    fun `fetched - wraps value - returns Fetched with data`() {
        val data: RemoteData<Int> = RemoteData.fetched(123)
        assertTrue(data is RemoteData.Fetched)
        assertEquals(123, (data as RemoteData.Fetched).data)
    }

    @Test
    fun `error - wraps throwable - returns Error with cause`() {
        val cause = IllegalStateException("boom")
        val data: RemoteData<Nothing> = RemoteData.error(cause)
        assertTrue(data is RemoteData.Error)
        assertSame(cause, (data as RemoteData.Error).error)
    }

    @Test
    fun `loading - factory - returns Loading singleton`() {
        assertSame(RemoteData.Loading, RemoteData.loading())
    }

    // --- contentKey ---

    @Test
    fun `Fetched - contentKey - returns fetched_remote_data`() {
        assertEquals("fetched_remote_data", RemoteData.Fetched("any").contentKey)
    }

    @Test
    fun `Error - contentKey - returns error_remote_data`() {
        assertEquals("error_remote_data", RemoteData.Error(RuntimeException()).contentKey)
    }

    @Test
    fun `Loading - contentKey - returns loading_remote_data`() {
        assertEquals("loading_remote_data", RemoteData.Loading.contentKey)
    }

    @Test
    fun `Fetched - contentKey - stable across different data values`() {
        assertEquals(RemoteData.Fetched(1).contentKey, RemoteData.Fetched(2).contentKey)
    }
}

package grmv.android.fdk.state.traits

import grmv.android.fdk.state.BuilderOps
import grmv.android.fdk.state.RemoteData
import org.junit.Assert.assertEquals
import org.junit.Test

class RemoteDataHolderTest {

    private data class S(
        override val remoteData: RemoteData<Int> = RemoteData.loading(),
    ) : RemoteDataState<S, Int> {
        override fun withRemoteData(remoteData: RemoteData<Int>) = copy(remoteData = remoteData)
    }

    private class FakeBuilder(var current: S = S()) : BuilderOps<S>, RemoteDataOps<S, Int> {
        override fun accumulate(updater: (S) -> S) { current = updater(current) }
    }

    @Test
    fun `loading - sets payload to RemoteData_Loading`() {
        val b = FakeBuilder(S(remoteData = RemoteData.fetched(42)))
        b.loading()
        assertEquals(RemoteData.Loading, b.current.remoteData)
    }

    @Test
    fun `fetched - sets payload to RemoteData_Fetched with data`() {
        val b = FakeBuilder()
        b.fetched(99)
        assertEquals(RemoteData.Fetched(99), b.current.remoteData)
    }

    @Test
    fun `failed - sets payload to RemoteData_Error with throwable`() {
        val error = IllegalStateException("boom")
        val b = FakeBuilder()
        b.failed(error)
        assertEquals(RemoteData.Error(error), b.current.remoteData)
    }

    @Test
    fun `loading then fetched - last write wins`() {
        val b = FakeBuilder()
        b.loading()
        b.fetched(7)
        assertEquals(RemoteData.Fetched(7), b.current.remoteData)
    }

    @Test
    fun `fetched then failed - last write wins`() {
        val b = FakeBuilder()
        b.fetched(7)
        b.failed(RuntimeException("x"))
        assertEquals(RemoteData.Error::class, b.current.remoteData::class)
    }
}

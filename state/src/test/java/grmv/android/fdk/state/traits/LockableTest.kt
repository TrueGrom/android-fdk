package grmv.android.fdk.state.traits

import grmv.android.fdk.state.BuilderOps
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockableTest {

    private data class S(override val locked: Boolean = false) : LockableState<S> {
        override fun withLocked(locked: Boolean) = copy(locked = locked)
    }

    private class FakeBuilder(var current: S = S()) : BuilderOps<S>, LockOps<S> {
        override fun accumulate(updater: (S) -> S) { current = updater(current) }
    }

    @Test
    fun `lock - sets locked to true`() {
        val b = FakeBuilder()
        b.lock()
        assertTrue(b.current.locked)
    }

    @Test
    fun `unlock - sets locked to false`() {
        val b = FakeBuilder(S(locked = true))
        b.unlock()
        assertFalse(b.current.locked)
    }

    @Test
    fun `lock then unlock - last write wins`() {
        val b = FakeBuilder()
        b.lock()
        b.unlock()
        assertFalse(b.current.locked)
    }
}

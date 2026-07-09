package grmv.android.fdk.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class BaseStateBuilderTest {

    private data class S(val count: Int = 0, val name: String = "init") : BaseState

    private class Builder(override val initial: S = S()) : BaseStateBuilder<S>()

    // --- build (no mutations) ---

    @Test
    fun `build - no mutations - returns initial state`() {
        val initial = S(count = 5, name = "seed")
        val out = Builder(initial).build()
        assertSame(initial, out)
    }

    // --- accumulate ---

    @Test
    fun `accumulate - first call - receives initial state`() {
        val builder = Builder(S(count = 1))
        var seen: S? = null
        builder.accumulate { current ->
            seen = current
            current.copy(count = current.count + 1)
        }
        builder.build()
        assertEquals(S(count = 1), seen)
    }

    @Test
    fun `accumulate - single call - build returns transformed state`() {
        val builder = Builder(S(count = 1, name = "a"))
        builder.accumulate { it.copy(count = 10) }
        assertEquals(S(count = 10, name = "a"), builder.build())
    }

    @Test
    fun `accumulate - chained calls - each sees previous result`() {
        val builder = Builder(S(count = 0, name = "x"))
        builder.accumulate { it.copy(count = it.count + 1) }
        builder.accumulate { it.copy(count = it.count + 1, name = "y") }
        assertEquals(S(count = 2, name = "y"), builder.build())
    }

    @Test
    fun `accumulate - many calls - all transforms compose`() {
        val builder = Builder(S(count = 0))
        repeat(5) {
            builder.accumulate { it.copy(count = it.count + 1) }
        }
        assertEquals(5, builder.build().count)
    }

    // --- laziness ---

    @Test
    fun `accumulate - transforms are not executed until build`() {
        val builder = Builder(S(count = 0))
        var executed = false
        builder.accumulate {
            executed = true
            it.copy(count = 1)
        }
        assertFalse(executed)
        builder.build()
        assertTrue(executed)
    }

    // --- build is idempotent (does not consume transforms) ---

    @Test
    fun `build - called twice - returns equal state each time`() {
        val builder = Builder(S(count = 0))
        builder.accumulate { it.copy(count = it.count + 1) }
        assertEquals(S(count = 1), builder.build())
        assertEquals(S(count = 1), builder.build())
    }
}

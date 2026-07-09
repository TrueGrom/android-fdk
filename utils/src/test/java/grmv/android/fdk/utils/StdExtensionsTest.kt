package grmv.android.fdk.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class StdExtensionsTest {

    // --- Boolean.toInt ---

    @Test
    fun `toInt - true - returns 1`() {
        assertEquals(1, true.toInt())
    }

    @Test
    fun `toInt - false - returns 0`() {
        assertEquals(0, false.toInt())
    }

    // --- Int.toBool ---

    @Test
    fun `toBool - zero - returns false`() {
        assertFalse(0.toBool())
    }

    @Test
    fun `toBool - positive one - returns true`() {
        assertTrue(1.toBool())
    }

    @Test
    fun `toBool - negative - returns true`() {
        assertTrue((-1).toBool())
    }

    // --- T.toList ---

    @Test
    fun `toList - string - wraps in single-element list`() {
        val result = "hello".toList()
        assertEquals(1, result.size)
        assertEquals("hello", result[0])
    }

    @Test
    fun `toList - integer - wraps in single-element list`() {
        val result = 99.toList()
        assertEquals(listOf(99), result)
    }

    @Test
    fun `toList - object reference - list contains same reference`() {
        val obj = object {}
        val result = obj.toList()
        assertEquals(1, result.size)
        assertSame(obj, result[0])
    }
}

package grmv.android.fdk.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class LetTest {

    // letWith: receiver is non-null (T : Any), other may be null
    @Test
    fun `letWith - other non-null - executes block with both values`() {
        val result = "hello".letWith(42) { s, n -> "$s-$n" }
        assertEquals("hello-42", result)
    }

    @Test
    fun `letWith - other null - returns null without executing block`() {
        var called = false
        val result = "hello".letWith<String, Int, String>(null) { _, _ -> called = true; "x" }
        assertNull(result)
        assertFalse(called)
    }

    @Test
    fun `letWith - block receives exact receiver and other instances`() {
        val receiver = object {}
        val other = object {}
        var capturedReceiver: Any? = null
        var capturedOther: Any? = null
        receiver.letWith(other) { r, o -> capturedReceiver = r; capturedOther = o }
        assertSame(receiver, capturedReceiver)
        assertSame(other, capturedOther)
    }

    @Test
    fun `letWith - block return value is propagated`() {
        val result = 1.letWith("x") { a, b -> a + b.length }
        assertEquals(2, result)
    }

    // letBoth: receiver is nullable (T?), other is nullable (O?)
    @Test
    fun `letBoth - both non-null - executes block`() {
        val result = "world".letBoth(99) { s, n -> "$s:$n" }
        assertEquals("world:99", result)
    }

    @Test
    fun `letBoth - receiver null - returns null`() {
        var called = false
        val receiver: String? = null
        val result = receiver.letBoth(42) { _, _ -> called = true; "x" }
        assertNull(result)
        assertFalse(called)
    }

    @Test
    fun `letBoth - other null - returns null`() {
        var called = false
        val result = "hello".letBoth<String, Int, String>(null) { _, _ -> called = true; "x" }
        assertNull(result)
        assertFalse(called)
    }

    @Test
    fun `letBoth - both null - returns null`() {
        val receiver: String? = null
        val result = receiver.letBoth<String, Int, String>(null) { _, _ -> "x" }
        assertNull(result)
    }

    @Test
    fun `letBoth - block receives exact non-null instances`() {
        val a = "a"
        val b = 7
        var capturedA: String? = null
        var capturedB: Int? = null
        a.letBoth(b) { x, y -> capturedA = x; capturedB = y }
        assertEquals("a", capturedA)
        assertEquals(7, capturedB)
    }

}

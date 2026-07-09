package grmv.android.fdk.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultTest {

    // --- success ---

    @Test
    fun `success - non-null value - wraps in Result success`() {
        val result = "hello".success()
        assertEquals("hello", result.getOrNull())
    }

    // --- successNullable ---

    @Test
    fun `successNullable - non-null value - wraps in Result success`() {
        val value: String? = "data"
        val result = value.successNullable()
        assertEquals("data", result.getOrNull())
    }

    @Test
    fun `successNullable - null value - wraps null in Result success`() {
        val value: String? = null
        val result = value.successNullable()
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    // --- failure ---

    @Test
    fun `failure - throwable - wraps in Result failure`() {
        val ex = IllegalStateException("boom")
        val result = ex.failure<String>()
        assertTrue(result.isFailure)
        assertEquals(ex, result.exceptionOrNull())
    }

    // --- successOrFailureIfNull ---

    @Test
    fun `successOrFailureIfNull - non-null value - returns success with value`() {
        val value: String? = "ok"
        val result = value.successOrFailureIfNull()
        assertTrue(result.isSuccess)
        assertEquals("ok", result.getOrNull())
    }

    @Test
    fun `successOrFailureIfNull - null value - returns failure`() {
        val value: String? = null
        val result = value.successOrFailureIfNull()
        assertTrue(result.isFailure)
    }

    @Test
    fun `successOrFailureIfNull - null value - failure cause is NullPointerException`() {
        val value: String? = null
        val result = value.successOrFailureIfNull()
        assertTrue(result.exceptionOrNull() is NullPointerException)
    }

}

package grmv.android.fdk.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ValueTest {

    @Test
    fun isPresented_some_returnsTrue() {
        assertTrue(Value.Some(1).isPresented())
    }

    @Test
    fun isPresented_none_returnsFalse() {
        assertFalse(Value.None.isPresented())
    }

    @Test
    fun notPresented_none_returnsTrue() {
        assertTrue(Value.None.notPresented())
    }

    @Test
    fun notPresented_some_returnsFalse() {
        assertFalse(Value.Some("x").notPresented())
    }

    @Test
    fun from_nonNullValue_wrapsSome() {
        assertEquals(Value.Some(42), Value.from(42))
    }

    @Test
    fun fromNullable_nonNull_wrapsSome() {
        assertEquals(Value.Some("hello"), Value.fromNullable("hello"))
    }

    @Test
    fun fromNullable_null_wrapsNullInsideSome() {
        val result = Value.fromNullable<String>(null)
        assertTrue(result.isPresented())
        assertSame(null, result.value)
    }

    @Test
    fun noneIfNull_companion_nonNull_returnsSome() {
        assertEquals(Value.Some("hi"), Value.noneIfNull("hi"))
    }

    @Test
    fun noneIfNull_companion_null_returnsNone() {
        assertSame(Value.None, Value.noneIfNull<String>(null))
    }

    @Test
    fun fromNullable_vs_noneIfNull_onNull_differentResults() {
        val fromNullableResult = Value.fromNullable<String>(null)
        val noneIfNullResult = Value.noneIfNull<String>(null)
        assertTrue(fromNullableResult.isPresented())
        assertFalse(noneIfNullResult.isPresented())
    }

    @Test
    fun someOrNone_success_returnsSome() {
        assertEquals(Value.Some(99), Result.success(99).someOrNone())
    }

    @Test
    fun someOrNone_failure_returnsNone() {
        assertSame(Value.None, Result.failure<Int>(RuntimeException()).someOrNone())
    }

    @Test
    fun resultNoneIfNull_successNonNull_returnsSome() {
        assertEquals(Value.Some("v"), Result.success<String?>("v").noneIfNull())
    }

    @Test
    fun resultNoneIfNull_successNull_returnsNone() {
        assertSame(Value.None, Result.success<String?>(null).noneIfNull())
    }

    @Test
    fun resultNoneIfNull_failure_returnsNone() {
        assertSame(Value.None, Result.failure<String?>(RuntimeException()).noneIfNull())
    }

    @Test
    fun nullableSomeOrNone_successNonNull_returnsSome() {
        assertEquals(Value.Some("v"), Result.success<String?>("v").nullableSomeOrNone())
    }

    @Test
    fun nullableSomeOrNone_successNull_returnsSomeOfNull() {
        val result = Result.success<String?>(null).nullableSomeOrNone()
        assertTrue(result.isPresented())
        assertSame(null, (result as Value.Some).value)
    }

    @Test
    fun nullableSomeOrNone_failure_returnsNone() {
        assertSame(Value.None, Result.failure<String?>(RuntimeException()).nullableSomeOrNone())
    }

    @Test
    fun nullableSomeOrNone_vs_noneIfNull_onNullSuccess_differentResults() {
        val successNull = Result.success<String?>(null)
        assertTrue(successNull.nullableSomeOrNone().isPresented())
        assertFalse(successNull.noneIfNull().isPresented())
    }
}

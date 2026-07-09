package grmv.android.fdk.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class EitherTest {

    // --- isLeft ---

    @Test
    fun `isLeft - receiver is Left - returns true`() {
        val either: Either<String, Int> = Either.Left("e")
        assertTrue(either.isLeft())
    }

    @Test
    fun `isLeft - receiver is Right - returns false`() {
        val either: Either<String, Int> = Either.Right(1)
        assertFalse(either.isLeft())
    }

    @Test
    fun `isLeft - true branch smart-casts to Left`() {
        val either: Either<String, Int> = Either.Left("e")
        // Compiles only if the contract enables smart-cast to Left.
        val captured: String = if (either.isLeft()) either.value else "fallback"
        assertEquals("e", captured)
    }

    // --- isRight ---

    @Test
    fun `isRight - receiver is Right - returns true`() {
        val either: Either<String, Int> = Either.Right(7)
        assertTrue(either.isRight())
    }

    @Test
    fun `isRight - receiver is Left - returns false`() {
        val either: Either<String, Int> = Either.Left("e")
        assertFalse(either.isRight())
    }

    @Test
    fun `isRight - true branch smart-casts to Right`() {
        val either: Either<String, Int> = Either.Right(7)
        // Compiles only if the contract enables smart-cast to Right.
        val captured: Int = if (either.isRight()) either.value else -1
        assertEquals(7, captured)
    }

    // --- fold ---

    @Test
    fun `fold on Left calls ifLeft and not ifRight`() {
        var rightCalled = false
        val result = Either.Left("error").fold(
            ifLeft = { it.length },
            ifRight = { rightCalled = true; -1 }
        )
        assertEquals(5, result)
        assertFalse(rightCalled)
    }

    @Test
    fun `fold on Right calls ifRight and not ifLeft`() {
        var leftCalled = false
        val result = Either.Right(42).fold(
            ifLeft = { leftCalled = true; -1 },
            ifRight = { it * 2 }
        )
        assertEquals(84, result)
        assertFalse(leftCalled)
    }

    // --- getOrElse ---

    @Test
    fun `getOrElse on Right returns wrapped value`() {
        val either: Either<String, Int> = Either.Right(10)
        assertEquals(10, either.getOrElse { 99 })
    }

    @Test
    fun `getOrElse on Left returns default`() {
        val either: Either<String, Int> = Either.Left("err")
        assertEquals(99, either.getOrElse { 99 })
    }

    @Test
    fun `getOrElse default lambda not evaluated for Right`() {
        var called = false
        Either.Right(1).getOrElse { called = true; 0 }
        assertFalse(called)
    }

    // --- onLeft ---

    @Test
    fun `onLeft action invoked for Left`() {
        var captured: String? = null
        Either.Left("oops").onLeft { captured = it }
        assertEquals("oops", captured)
    }

    @Test
    fun `onLeft action not invoked for Right`() {
        var called = false
        Either.Right(1).onLeft { called = true }
        assertFalse(called)
    }

    @Test
    fun `onLeft returns same Left instance`() {
        val either: Either<String, Int> = Either.Left("x")
        assertSame(either, either.onLeft { })
    }

    @Test
    fun `onLeft returns same Right instance`() {
        val either: Either<String, Int> = Either.Right(1)
        assertSame(either, either.onLeft { })
    }

    // --- onRight ---

    @Test
    fun `onRight action invoked for Right`() {
        var captured: Int? = null
        Either.Right(7).onRight { captured = it }
        assertEquals(7, captured)
    }

    @Test
    fun `onRight action not invoked for Left`() {
        var called = false
        Either.Left("err").onRight { called = true }
        assertFalse(called)
    }

    @Test
    fun `onRight returns same Right instance`() {
        val either: Either<String, Int> = Either.Right(5)
        assertSame(either, either.onRight { })
    }

    @Test
    fun `onRight returns same Left instance`() {
        val either: Either<String, Int> = Either.Left("x")
        assertSame(either, either.onRight { })
    }

    // --- chaining ---

    @Test
    fun `onLeft then onRight on Left only triggers onLeft`() {
        var leftCalled = false
        var rightCalled = false
        Either.Left("x")
            .onLeft { leftCalled = true }
            .onRight { rightCalled = true }
        assertTrue(leftCalled)
        assertFalse(rightCalled)
    }

    @Test
    fun `onRight then onLeft on Right only triggers onRight`() {
        var leftCalled = false
        var rightCalled = false
        Either.Right(1)
            .onRight { rightCalled = true }
            .onLeft { leftCalled = true }
        assertTrue(rightCalled)
        assertFalse(leftCalled)
    }

    // --- companion factory ---

    @Test
    fun `left factory produces Left with correct value`() {
        val either = Either.left("e")
        assertTrue(either is Either.Left)
        assertEquals("e", (either as Either.Left).value)
    }

    @Test
    fun `right factory produces Right with correct value`() {
        val either = Either.right(42)
        assertTrue(either is Either.Right)
        assertEquals(42, (either as Either.Right).value)
    }

    // --- runCatchingEither ---

    @Test
    fun `runCatchingEither returns Right on success`() {
        val result = runCatchingEither(factory = { it.message }) { 42 }
        assertEquals(Either.Right(42), result)
    }

    @Test
    fun `runCatchingEither returns Left on Exception`() {
        val ex = RuntimeException("boom")
        val result = runCatchingEither(factory = { it }) { throw ex }
        assertEquals(Either.Left(ex), result)
    }

    @Test
    fun `runCatchingEither returns Left on Error`() {
        val err = OutOfMemoryError("oom")
        val result = runCatchingEither(factory = { it }) { throw err }
        assertEquals(Either.Left(err), result)
    }

    @Test
    fun `runCatchingEither factory receives the thrown Throwable`() {
        var caught: Throwable? = null
        runCatchingEither(factory = { caught = it; "err" }) { throw IllegalStateException("x") }
        assertTrue(caught is IllegalStateException)
    }

    // --- extension functions ---

    @Test
    fun `toRight wraps value in Right`() {
        assertEquals(Either.Right("hello"), "hello".toRight())
    }

    @Test
    fun `toLeft wraps value in Left`() {
        assertEquals(Either.Left("err"), "err".toLeft())
    }

    // --- inline / non-local return semantics ---
    // These compile only because the receivers are `inline fun` with non-crossinline lambdas.

    @Test
    fun `fold supports non-local return from ifLeft`() {
        assertEquals("from-left", foldNonLocal(Either.Left("e")))
    }

    @Test
    fun `fold supports non-local return from ifRight`() {
        assertEquals("from-right:7", foldNonLocal(Either.Right(7)))
    }

    private fun foldNonLocal(either: Either<String, Int>): String {
        either.fold(
            ifLeft = { return "from-left" },
            ifRight = { return "from-right:$it" }
        )
        return "unreachable"
    }

    @Test
    fun `onLeft supports non-local return from action`() {
        assertEquals("short-circuited", onLeftNonLocal(Either.Left("e")))
        assertEquals("ran-through", onLeftNonLocal(Either.Right(1)))
    }

    private fun onLeftNonLocal(either: Either<String, Int>): String {
        either.onLeft { return "short-circuited" }
        return "ran-through"
    }

    @Test
    fun `onRight supports non-local return from action`() {
        assertEquals("short-circuited", onRightNonLocal(Either.Right(1)))
        assertEquals("ran-through", onRightNonLocal(Either.Left("e")))
    }

    private fun onRightNonLocal(either: Either<String, Int>): String {
        either.onRight { return "short-circuited" }
        return "ran-through"
    }

    @Test
    fun `runCatchingEither supports non-local return from block`() {
        assertEquals(99, runCatchingBlockNonLocal())
    }

    private fun runCatchingBlockNonLocal(): Int {
        runCatchingEither<String, Int>(factory = { it.message ?: "" }) {
            return 99
        }
        return -1
    }

    @Test
    fun `runCatchingEither supports non-local return from factory`() {
        assertEquals("caught", runCatchingFactoryNonLocal())
    }

    @Suppress("UNREACHABLE_CODE")
    private fun runCatchingFactoryNonLocal(): String {
        runCatchingEither<String, Int>(
            factory = { return "caught" },
            block = { throw IllegalStateException("x") }
        )
        return "unreachable"
    }
}

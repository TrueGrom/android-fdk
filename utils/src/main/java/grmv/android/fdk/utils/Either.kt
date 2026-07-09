package grmv.android.fdk.utils

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Represents a value of one of two possible types — [Left] (typically an error/alternative)
 * or [Right] (the success/primary path).
 *
 * By convention [Right] holds the happy-path value and [Left] holds the error or alternate value,
 * mirroring the Haskell `Either` convention.
 *
 * @param L The type held by [Left].
 * @param R The type held by [Right].
 */
sealed class Either<out L, out R> {
    /**
     * The alternative — typically an error or fallback value.
     *
     * @property value The wrapped left value.
     */
    data class Left<out L>(val value: L) : Either<L, Nothing>()

    /**
     * The primary success value.
     *
     * @property value The wrapped right value.
     */
    data class Right<out R>(val value: R) : Either<Nothing, R>()

    /**
     * Collapses both branches into a single value.
     *
     * Both lambdas are inlined, so non-local `return` from either branch
     * propagates to the enclosing function.
     *
     * @param ifLeft Called when this is [Left].
     * @param ifRight Called when this is [Right].
     * @return The result of whichever branch was invoked.
     */
    inline fun <T> fold(ifLeft: (L) -> T, ifRight: (R) -> T): T = when (this) {
        is Left -> ifLeft(this.value)
        is Right -> ifRight(this.value)
    }

    /**
     * Extracts the [Right] value, or falls back to [default] when this is [Left].
     *
     * @param default Invoked lazily when this is [Left].
     * @return The right value, or the result of [default].
     */
    fun getOrElse(default: () -> @UnsafeVariance R): R = when (this) {
        is Right -> this.value
        is Left -> default()
    }

    /**
     * Runs [action] if this is [Left], then returns `this` unchanged — useful for side-effects in a chain.
     *
     * The lambda is inlined; a non-local `return` from [action] propagates to the enclosing function.
     *
     * @param action Receives the left value; return value is ignored.
     * @return `this`, allowing further chaining.
     */
    inline fun onLeft(action: (L) -> Unit): Either<L, R> {
        if (this is Left) {
            action(this.value)
        }
        return this
    }

    /**
     * Runs [action] if this is [Right], then returns `this` unchanged — useful for side-effects in a chain.
     *
     * The lambda is inlined; a non-local `return` from [action] propagates to the enclosing function.
     *
     * @param action Receives the right value; return value is ignored.
     * @return `this`, allowing further chaining.
     */
    inline fun onRight(action: (R) -> Unit): Either<L, R> {
        if (this is Right) {
            action(this.value)
        }
        return this
    }

    companion object {
        /** Wraps [value] as a [Left]. */
        fun <L> left(value: L): Either<L, Nothing> = Left(value)

        /** Wraps [value] as a [Right]. */
        fun <R> right(value: R): Either<Nothing, R> = Right(value)
    }
}

/**
 * Executes [block] and wraps the result in [Either.Right], or catches any [Throwable] and
 * wraps the mapped error in [Either.Left].
 *
 * Both lambdas are inlined; a non-local `return` from [block] short-circuits this call and
 * returns from the enclosing function without producing an [Either].
 *
 * Catches every [Throwable], including `CancellationException`. In coroutine code prefer
 * `runCatchingRethrowCancellation` so cancellation is not captured as a [Left] and structured
 * concurrency is preserved.
 *
 * @param factory Converts the caught throwable to the left (error) type.
 * @param block The operation to run.
 * @return [Either.Right] on success; [Either.Left] on any thrown exception.
 */
inline fun <L, R> runCatchingEither(factory: (Throwable) -> L, block: () -> R): Either<L, R> {
    return try {
        Either.right(block())
    } catch (t: Throwable) {
        Either.left(factory(t))
    }
}

/** Wraps the receiver as [Either.Right]. */
fun <T> T.toRight(): Either<Nothing, T> = Either.right(this)

/** Wraps the receiver as [Either.Left]. */
fun <E> E.toLeft(): Either<E, Nothing> = Either.left(this)

/**
 * Returns `true` if this is [Either.Left]. Smart-casts the receiver to [Either.Left] in the `true` branch.
 */
@OptIn(ExperimentalContracts::class)
fun <L, R> Either<L, R>.isLeft(): Boolean {
    contract { returns(true) implies (this@isLeft is Either.Left<L>) }
    return this is Either.Left<L>
}

/**
 * Returns `true` if this is [Either.Right]. Smart-casts the receiver to [Either.Right] in the `true` branch.
 */
@OptIn(ExperimentalContracts::class)
fun <L, R> Either<L, R>.isRight(): Boolean {
    contract { returns(true) implies (this@isRight is Either.Right<R>) }
    return this is Either.Right<R>
}

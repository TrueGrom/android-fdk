package grmv.android.fdk.utils

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract


/**
 * An optional value type — either [Some] (present) or [None] (absent).
 *
 * Prefer over nullable types when absence must be explicit and carries no error context.
 * Use [noneIfNull] to lift a nullable `T?`, or [Result.someOrNone] to collapse a [Result] into [None] on failure.
 *
 * @param T The type of the wrapped value.
 */
sealed class Value<out T> {
    /** Absent value; typed equivalent of `null`. */
    data object None : Value<Nothing>()

    /**
     * A present value.
     *
     * @property value The wrapped non-null value.
     */
    data class Some<T>(val value: T) : Value<T>()

    /**
     * Returns `true` when this is [Some].
     *
     * Note: member-function contracts cannot smart-cast `this` in K2.
     * Use an explicit `is Some<T>` check for narrowing.
     */
    @OptIn(ExperimentalContracts::class)
    fun isPresented(): Boolean {
        contract { returns(true) implies (this@Value is Some<T>) }
        return this is Some<T>
    }

    /** Returns `true` when this is [None]. */
    fun notPresented() = !isPresented()

    companion object {
        /** Wraps a non-null [value] in [Some]. */
        fun <T : Any> from(value: T) = Some(value)

        /** Wraps a nullable [value] in [Some] as-is. Prefer [noneIfNull] when absence should map to [None]. */
        fun <T : Any> fromNullable(value: T?) = Some(value)

        /** Returns [Some] if [value] is non-null, [None] otherwise. */
        fun <T : Any> noneIfNull(value: T?) = if (value != null) Some(value) else None
    }
}

/** Maps a successful [Result] to [Value.Some], or [Value.None] on failure. */
fun <T : Any> Result<T>.someOrNone() = fold(onSuccess = { Value.from(it) }, onFailure = { Value.None })

/** Maps a successful non-null [Result] to [Value.Some], or [Value.None] on null or failure. */
fun <T : Any> Result<T?>.noneIfNull() = fold(onSuccess = { Value.noneIfNull(it) }, onFailure = { Value.None })

/**
 * Maps a successful [Result] to [Value.Some] preserving the nullable value inside, or [Value.None] on failure.
 *
 * Unlike [noneIfNull], a `null` success produces `Some(null)` rather than `None`.
 */
fun <T : Any> Result<T?>.nullableSomeOrNone() = fold(onSuccess = { Value.fromNullable(it) }, onFailure = { Value.None })
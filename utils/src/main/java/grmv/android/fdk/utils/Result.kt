package grmv.android.fdk.utils


/** Convenience alias for `Result<Unit>` — use when the outcome carries no data. */
typealias SomeResult = Result<Unit>

/**
 * Wraps the receiver in a successful [Result].
 *
 * @return `Result.success(this)`
 */
fun <T : Any> T.success(): Result<T> {
    return Result.success(this)
}

/**
 * Wraps a nullable receiver in a successful [Result], preserving `null` as a valid value.
 *
 * @return `Result.success(this)` — the result is always successful; `null` is not treated as failure.
 */
fun <T : Any> T?.successNullable(): Result<T?> {
    return Result.success(this)
}

/**
 * Wraps the receiver throwable in a failed [Result] typed to [T].
 *
 * @return `Result.failure(this)`
 */
fun <T : Any> Throwable.failure(): Result<T> {
    return Result.failure(this)
}

/**
 * Converts a nullable value to a non-null successful [Result], or a [NullPointerException] failure if `null`.
 *
 * Prefer this over [successNullable] when `null` is an illegal state that should propagate as an error.
 *
 * @return `Result.success(this)` when non-null; `Result.failure(NullPointerException)` when null.
 */
fun <T : Any> T?.successOrFailureIfNull(): Result<T> {
    return this?.success() ?: NullPointerException("Value is null").failure()
}
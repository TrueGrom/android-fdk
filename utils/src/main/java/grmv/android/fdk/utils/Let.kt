package grmv.android.fdk.utils

/**
 * Executes [block] with the receiver and [other] only when [other] is non-null.
 *
 * Useful as a null-safe two-argument `let` when the receiver is guaranteed non-null
 * but the second value is optional.
 *
 * @param other The second argument; if `null` the block is skipped and `null` is returned.
 * @param block Called with `this` and the non-null [other].
 * @return The block result, or `null` if [other] was `null`.
 */
inline fun <T : Any, O: Any, R : Any> T.letWith(other: O?, block: (T, O) -> R): R? {
    return if (other != null) {
        block(this, other)
    } else {
        null
    }
}

/**
 * Executes [block] only when both the receiver and [other] are non-null.
 *
 * Acts as a two-argument null-safe `let` for cases where both values are optional.
 *
 * @param other The second argument; block is skipped if either this or [other] is `null`.
 * @param block Called with both non-null values.
 * @return The block result, or `null` if either value was `null`.
 */
inline fun <T : Any, O: Any, R : Any> T?.letBoth(other: O?, block: (T, O) -> R): R? {
    return if (other != null && this != null) {
        block(this, other)
    } else {
        null
    }
}
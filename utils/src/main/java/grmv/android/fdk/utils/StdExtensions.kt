package grmv.android.fdk.utils


/** Maps `true` to `1` and `false` to `0`. */
fun Boolean.toInt() = if (this) 1 else 0

/** Returns `false` when this value is `0`, `true` for any other value. */
fun Int.toBool() = this != 0

/** Wraps this value in a single-element, read-only list. */
fun <T : Any> T.toList() = listOf(this)
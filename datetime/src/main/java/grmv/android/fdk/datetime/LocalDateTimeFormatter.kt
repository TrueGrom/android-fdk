@file:Suppress("FunctionName")

package grmv.android.fdk.datetime

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val HmmFmt = DateTimeFormatter.ofPattern("H:mm")
private val DdMMyyyyFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")
private val DdMMFmt = DateTimeFormatter.ofPattern("dd.MM")
private val DdMMMFmt = DateTimeFormatter.ofPattern("dd MMM")
private val DdMMMyyyyFmt = DateTimeFormatter.ofPattern("dd MMM yyyy")
private val DdMMMMFmt = DateTimeFormatter.ofPattern("dd MMMM")
private val DdMMMMyyyyFmt = DateTimeFormatter.ofPattern("dd MMMM yyyy")
private val DdMMyyyyHmmFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy H:mm")
private val DdMMMHmmFmt = DateTimeFormatter.ofPattern("dd MMM H:mm")
private val DdMMMyyyyHmmFmt = DateTimeFormatter.ofPattern("dd MMM yyyy H:mm")

/** Formats the time portion as `H:mm` (e.g. `9:05`) using [locale]. */
fun LocalDateTime.Hmm(locale: Locale): String {
    return format(HmmFmt.withLocale(locale))
}

/** Formats this datetime's date portion as `dd.MM.yyyy` using [locale]. */
fun LocalDateTime.ddMMyyyy(locale: Locale): String {
    return format(DdMMyyyyFmt.withLocale(locale))
}

/**
 * Formats the date as `dd.MM` when the year matches the current year, or `dd.MM.yyyy` otherwise.
 *
 * Use this to omit the year for recent dates while keeping it for other years. The "current year"
 * is read from the system clock at call time, so the output of a fixed date can change across a
 * New Year boundary. The time portion is ignored.
 *
 * @param locale selects locale-sensitive output.
 */
fun LocalDateTime.ddMMOptionalYear(locale: Locale): String {
    return if (year == LocalDateTime.now().year) {
        format(DdMMFmt.withLocale(locale))
    } else {
        format(DdMMyyyyFmt.withLocale(locale))
    }
}

/**
 * Formats the date as `dd MMM` when the year matches the current year, or `dd MMM yyyy` otherwise.
 *
 * @param locale selects locale-sensitive month name.
 */
fun LocalDateTime.ddMMMOptionalYear(locale: Locale): String {
    return if (year == LocalDateTime.now().year) {
        format(DdMMMFmt.withLocale(locale))
    } else {
        format(DdMMMyyyyFmt.withLocale(locale))
    }
}

/**
 * Formats the date as `dd MMMM` when the year matches the current year, or `dd MMMM yyyy` otherwise.
 *
 * @param locale selects locale-sensitive full month name.
 */
fun LocalDateTime.ddMMMMOptionalYear(locale: Locale): String {
    return if (year == LocalDateTime.now().year) {
        format(DdMMMMFmt.withLocale(locale))
    } else {
        format(DdMMMMyyyyFmt.withLocale(locale))
    }
}

/** Formats this datetime as `dd.MM.yyyy H:mm` (e.g. `01.06.2024 9:05`) using [locale]. */
fun LocalDateTime.ddMMyyyyHmm(locale: Locale): String {
    return format(DdMMyyyyHmmFmt.withLocale(locale))
}

/**
 * Formats this datetime as `dd MMM H:mm` when the year matches the current year,
 * or `dd MMM yyyy H:mm` otherwise.
 *
 * Unlike the date-only `OptionalYear` helpers, the time portion is always included. The
 * "current year" is read from the system clock at call time.
 *
 * @param locale selects locale-sensitive month name.
 */
fun LocalDateTime.ddMMMHmmOptionalYear(locale: Locale): String {
    return if (year == LocalDateTime.now().year) {
        format(DdMMMHmmFmt.withLocale(locale))
    } else {
        format(DdMMMyyyyHmmFmt.withLocale(locale))
    }
}

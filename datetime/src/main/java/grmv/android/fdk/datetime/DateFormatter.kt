@file:Suppress("FunctionName")

package grmv.android.fdk.datetime

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DdMMyy = DateTimeFormatter.ofPattern("dd.MM.yy")
private val DdMMM = DateTimeFormatter.ofPattern("dd MMM")
private val DdMMyyyy = DateTimeFormatter.ofPattern("dd.MM.yyyy")
private val DdMMMyyyy = DateTimeFormatter.ofPattern("dd MMM yyyy")
private val DdMM = DateTimeFormatter.ofPattern("dd.MM")
private val MMMdyyyy = DateTimeFormatter.ofPattern("MMM d, yyyy")
private val Eeee = DateTimeFormatter.ofPattern("EEEE")
private val DdMMMMyyyy = DateTimeFormatter.ofPattern("dd MMMM yyyy")
private val MMMMyyyy = DateTimeFormatter.ofPattern("MMMM yyyy")

/** Formats this date as `dd.MM.yy` using [locale] for locale-sensitive output. */
fun LocalDate.ddMMyy(locale: Locale): String {
    return format(DdMMyy.withLocale(locale))
}

/** Formats this date as `dd MMM` (e.g. `01 Jun`) using [locale] for month name. */
fun LocalDate.ddMMM(locale: Locale): String {
    return format(DdMMM.withLocale(locale))
}

/** Formats this date as `dd.MM.yyyy` using [locale]. */
fun LocalDate.ddMMyyyy(locale: Locale): String {
    return format(DdMMyyyy.withLocale(locale))
}

/** Formats this date as `dd MMM yyyy` (e.g. `01 Jun 2024`) using [locale] for month name. */
fun LocalDate.ddMMMyyyy(locale: Locale): String {
    return format(DdMMMyyyy.withLocale(locale))
}

/** Formats this date as `dd.MM` using [locale]. */
fun LocalDate.ddMM(locale: Locale): String {
    return format(DdMM.withLocale(locale))
}

/** Formats this date as `MMM d, yyyy` (e.g. `Jun 1, 2024`) using [locale] for month name. */
fun LocalDate.MMMdyyyy(locale: Locale): String {
    return format(MMMdyyyy.withLocale(locale))
}

/** Formats this date as a full weekday name (`EEEE`, e.g. `Saturday`) using [locale]. */
fun LocalDate.EEEE(locale: Locale): String {
    return format(Eeee.withLocale(locale))
}

/** Formats this date as `dd MMMM yyyy` (e.g. `01 June 2024`) using [locale] for full month name. */
fun LocalDate.ddMMMMyyyy(locale: Locale): String {
    return format(DdMMMMyyyy.withLocale(locale))
}

/**
 * Formats this date as `dd MMM` when the year matches the current year, or `dd MMM yyyy` otherwise.
 *
 * Use this to omit the year for recent dates while keeping it unambiguous for past or future
 * years. The "current year" is read from the system clock at call time, so the output of a
 * fixed date can change across a New Year boundary.
 *
 * @param locale selects locale-sensitive month name.
 */
fun LocalDate.ddMMMOptionalYear(locale: Locale): String {
    return if (year == LocalDateTime.now().year) {
        format(DdMMM.withLocale(locale))
    } else {
        format(DdMMMyyyy.withLocale(locale))
    }
}

/** Formats this date as `MMMM yyyy` (e.g. `June 2024`) using [locale] for full month name. */
fun LocalDate.MMMMyyyy(locale: Locale): String {
    return format(MMMMyyyy.withLocale(locale))
}

/**
 * Formats a date range as `"start - end"`. When [startDate] and [endDate] fall in different years,
 * both are rendered as `dd MMM yyyy`; when they share the same year, both are rendered as `dd MMM`.
 *
 * @param locale selects locale-sensitive month names.
 */
fun rangeOptionalYear(locale: Locale, startDate: LocalDate, endDate: LocalDate): String {
    return if (startDate.year != endDate.year) {
        "${startDate.ddMMMyyyy(locale)} - ${endDate.ddMMMyyyy(locale)}"
    } else {
        "${startDate.ddMMM(locale)} - ${endDate.ddMMM(locale)}"
    }
}

@file:Suppress("FunctionName")

package grmv.android.fdk.uikit.datetime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import grmv.android.fdk.datetime.EEEE
import grmv.android.fdk.datetime.MMMMyyyy
import grmv.android.fdk.datetime.MMMdyyyy
import grmv.android.fdk.datetime.ddMM
import grmv.android.fdk.datetime.ddMMMOptionalYear
import grmv.android.fdk.datetime.ddMMMyyyy
import grmv.android.fdk.datetime.ddMMMMyyyy
import grmv.android.fdk.datetime.ddMMM
import grmv.android.fdk.datetime.ddMMyy
import grmv.android.fdk.datetime.ddMMyyyy
import grmv.android.fdk.datetime.rangeOptionalYear
import grmv.android.fdk.uikit.locale.currentLocale
import java.time.LocalDate
import java.util.Locale

/**
 * Proxy over the [LocalDate] format extensions, bound to a fixed date and the ambient locale.
 * Each method formats the captured date with the resolved [Locale].
 */
interface LocalDateFormatBuilder {
    /** Formats as `dd/MM/yy`, e.g. `05/06/26`. */
    fun ddMMyy(): String

    /** Formats as `dd MMM`, e.g. `05 Jun`. */
    fun ddMMM(): String

    /** Formats as `dd/MM/yyyy`, e.g. `05/06/2026`. */
    fun ddMMyyyy(): String

    /** Formats as `dd MMM yyyy`, e.g. `05 Jun 2026`. */
    fun ddMMMyyyy(): String

    /** Formats as `dd/MM`, e.g. `05/06`. */
    fun ddMM(): String

    /** Formats as `MMM d, yyyy`, e.g. `Jun 5, 2026`. */
    fun MMMdyyyy(): String

    /** Formats as the full weekday name, e.g. `Friday`. */
    fun EEEE(): String

    /** Formats as `dd MMMM yyyy`, e.g. `05 June 2026`. */
    fun ddMMMMyyyy(): String

    /** Formats as `dd MMM` when in the current year, or `dd MMM yyyy` otherwise. */
    fun ddMMMOptionalYear(): String

    /** Formats as `MMMM yyyy`, e.g. `June 2026`. */
    fun MMMMyyyy(): String

    /**
     * Formats the captured date through [endDate] as a range. Year is included on both ends only
     * when the two dates fall in different years.
     *
     * @param endDate the inclusive end of the range; must be on or after the captured date.
     */
    fun rangeOptionalYear(endDate: LocalDate): String
}

private class LocalDateFormatBuilderImpl(
    private val date: LocalDate,
    private val locale: Locale,
) : LocalDateFormatBuilder {
    override fun ddMMyy() = date.ddMMyy(locale)
    override fun ddMMM() = date.ddMMM(locale)
    override fun ddMMyyyy() = date.ddMMyyyy(locale)
    override fun ddMMMyyyy() = date.ddMMMyyyy(locale)
    override fun ddMM() = date.ddMM(locale)
    override fun MMMdyyyy() = date.MMMdyyyy(locale)
    override fun EEEE() = date.EEEE(locale)
    override fun ddMMMMyyyy() = date.ddMMMMyyyy(locale)
    override fun ddMMMOptionalYear() = date.ddMMMOptionalYear(locale)
    override fun MMMMyyyy() = date.MMMMyyyy(locale)
    override fun rangeOptionalYear(endDate: LocalDate) = rangeOptionalYear(locale, date, endDate)
}

/**
 * Formats [date] with the ambient locale, selecting the pattern via [block] on a
 * [LocalDateFormatBuilder] — e.g. `localizedFormat(date) { ddMMyyyy() }`.
 *
 * The locale is resolved from [currentLocale], so the result recomposes when the device language
 * changes; no [Locale] argument is needed at the call site. Call this from a composable scope and
 * select the desired output by invoking the matching builder method inside [block].
 *
 * @param date the date to format.
 * @param block selects the output pattern by calling a method on the receiver
 *   [LocalDateFormatBuilder].
 * @return the formatted, locale-aware string produced by [block].
 */
@Composable
@ReadOnlyComposable
fun localizedFormat(date: LocalDate, block: LocalDateFormatBuilder.() -> String): String {
    return LocalDateFormatBuilderImpl(date, currentLocale).block()
}

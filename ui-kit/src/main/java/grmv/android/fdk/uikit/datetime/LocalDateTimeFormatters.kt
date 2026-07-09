@file:Suppress("FunctionName")

package grmv.android.fdk.uikit.datetime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import grmv.android.fdk.uikit.locale.currentLocale
import grmv.android.fdk.datetime.Hmm
import grmv.android.fdk.datetime.ddMMMHmmOptionalYear
import grmv.android.fdk.datetime.ddMMMMOptionalYear
import grmv.android.fdk.datetime.ddMMMOptionalYear
import grmv.android.fdk.datetime.ddMMOptionalYear
import grmv.android.fdk.datetime.ddMMyyyy
import grmv.android.fdk.datetime.ddMMyyyyHmm
import java.time.LocalDateTime
import java.util.Locale

/**
 * Proxy over the [LocalDateTime] format extensions, bound to a fixed datetime and the ambient
 * locale. Each method formats the captured datetime with the resolved [Locale].
 */
interface LocalDateTimeFormatBuilder {
    /** Formats the time as `H:mm`, e.g. `9:05`. */
    fun Hmm(): String

    /** Formats the date as `dd.MM.yyyy`, e.g. `01.06.2024`. */
    fun ddMMyyyy(): String

    /** Formats as `dd.MM` when in the current year, or `dd.MM.yyyy` otherwise. */
    fun ddMMOptionalYear(): String

    /** Formats as `dd MMM` when in the current year, or `dd MMM yyyy` otherwise. */
    fun ddMMMOptionalYear(): String

    /** Formats as `dd MMMM` when in the current year, or `dd MMMM yyyy` otherwise. */
    fun ddMMMMOptionalYear(): String

    /** Formats as `dd.MM.yyyy H:mm`, e.g. `01.06.2024 9:05`. */
    fun ddMMyyyyHmm(): String

    /** Formats as `dd MMM H:mm` when in the current year, or `dd MMM yyyy H:mm` otherwise. */
    fun ddMMMHmmOptionalYear(): String
}

private class LocalDateTimeFormatBuilderImpl(
    private val dateTime: LocalDateTime,
    private val locale: Locale,
) : LocalDateTimeFormatBuilder {
    override fun Hmm() = dateTime.Hmm(locale)
    override fun ddMMyyyy() = dateTime.ddMMyyyy(locale)
    override fun ddMMOptionalYear() = dateTime.ddMMOptionalYear(locale)
    override fun ddMMMOptionalYear() = dateTime.ddMMMOptionalYear(locale)
    override fun ddMMMMOptionalYear() = dateTime.ddMMMMOptionalYear(locale)
    override fun ddMMyyyyHmm() = dateTime.ddMMyyyyHmm(locale)
    override fun ddMMMHmmOptionalYear() = dateTime.ddMMMHmmOptionalYear(locale)
}

/**
 * Formats [dateTime] with the ambient locale, selecting the pattern via [block] on a
 * [LocalDateTimeFormatBuilder] — e.g. `localizedFormat(dateTime) { ddMMyyyyHmm() }`.
 *
 * The locale is resolved from [currentLocale], so the result recomposes when the device language
 * changes; no [Locale] argument is needed at the call site. Call this from a composable scope and
 * select the desired output by invoking the matching builder method inside [block].
 *
 * @param dateTime the date-time to format.
 * @param block selects the output pattern by calling a method on the receiver
 *   [LocalDateTimeFormatBuilder].
 * @return the formatted, locale-aware string produced by [block].
 */
@Composable
@ReadOnlyComposable
fun localizedFormat(dateTime: LocalDateTime, block: LocalDateTimeFormatBuilder.() -> String): String {
    return LocalDateTimeFormatBuilderImpl(dateTime, currentLocale).block()
}

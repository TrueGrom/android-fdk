package grmv.android.fdk.datetime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale

class LocalDateTimeFormatterTest {

    private val dateTime = LocalDateTime.of(2024, 1, 15, 9, 5)
    private val locale = Locale.ENGLISH

    private fun thisYear(month: Int, day: Int, hour: Int, minute: Int) =
        LocalDateTime.of(LocalDate.now().year, month, day, hour, minute)

    @Test
    fun `Hmm - non-padded hour, padded minute`() {
        assertEquals("9:05", dateTime.Hmm(locale))
    }

    @Test
    fun `Hmm - two-digit hour - 24h format`() {
        assertEquals("14:30", LocalDateTime.of(2024, 1, 15, 14, 30).Hmm(locale))
    }

    @Test
    fun `ddMMyyyy - formats as dd_MM_yyyy`() {
        assertEquals("15.01.2024", dateTime.ddMMyyyy(locale))
    }

    @Test
    fun `ddMMyyyyHmm - formats as dd_MM_yyyy H mm`() {
        assertEquals("15.01.2024 9:05", dateTime.ddMMyyyyHmm(locale))
    }

    @Test
    fun `ddMMOptionalYear - current year - omits year`() {
        assertEquals("15.01", thisYear(1, 15, 9, 5).ddMMOptionalYear(locale))
    }

    @Test
    fun `ddMMOptionalYear - other year - includes year`() {
        assertEquals("15.01.2000", LocalDateTime.of(2000, 1, 15, 9, 5).ddMMOptionalYear(locale))
    }

    @Test
    fun `ddMMMOptionalYear - current year - omits year`() {
        assertEquals("15 Jan", thisYear(1, 15, 9, 5).ddMMMOptionalYear(locale))
    }

    @Test
    fun `ddMMMOptionalYear - other year - includes year`() {
        assertEquals("15 Jan 2000", LocalDateTime.of(2000, 1, 15, 9, 5).ddMMMOptionalYear(locale))
    }

    @Test
    fun `ddMMMMOptionalYear - current year - omits year`() {
        assertEquals("15 January", thisYear(1, 15, 9, 5).ddMMMMOptionalYear(locale))
    }

    @Test
    fun `ddMMMMOptionalYear - other year - includes year`() {
        assertEquals("15 January 2000", LocalDateTime.of(2000, 1, 15, 9, 5).ddMMMMOptionalYear(locale))
    }

    @Test
    fun `ddMMMHmmOptionalYear - current year - omits year`() {
        assertEquals("15 Jan 9:05", thisYear(1, 15, 9, 5).ddMMMHmmOptionalYear(locale))
    }

    @Test
    fun `ddMMMHmmOptionalYear - other year - includes year`() {
        assertEquals("15 Jan 2000 9:05", LocalDateTime.of(2000, 1, 15, 9, 5).ddMMMHmmOptionalYear(locale))
    }

    // --- locale change ---

    @Test
    fun `ddMMMMOptionalYear - German locale, other year - localizes full month`() {
        assertEquals(
            "15 Januar 2000",
            LocalDateTime.of(2000, 1, 15, 9, 5).ddMMMMOptionalYear(Locale.GERMAN),
        )
    }

    @Test
    fun `ddMMMMOptionalYear - French locale, other year - localizes full month`() {
        assertEquals(
            "15 janvier 2000",
            LocalDateTime.of(2000, 1, 15, 9, 5).ddMMMMOptionalYear(Locale.FRENCH),
        )
    }

    @Test
    fun `ddMMMMOptionalYear - shared formatter applies per-call locale, not a fixed one`() {
        // Guards the hoisted withLocale() refactor: same cached formatter, different locales.
        val dt = LocalDateTime.of(2000, 1, 15, 9, 5)
        assertNotEquals(
            dt.ddMMMMOptionalYear(Locale.ENGLISH),
            dt.ddMMMMOptionalYear(Locale.GERMAN),
        )
    }
}

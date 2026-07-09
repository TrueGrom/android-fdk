package grmv.android.fdk.datetime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class DateFormatterTest {

    // 2024-01-15 is a Monday — fixes weekday-dependent output.
    private val date = LocalDate.of(2024, 1, 15)
    private val locale = Locale.ENGLISH

    @Test
    fun `ddMMyy - formats as dd_MM_yy`() {
        assertEquals("15.01.24", date.ddMMyy(locale))
    }

    @Test
    fun `ddMMM - formats as dd MMM`() {
        assertEquals("15 Jan", date.ddMMM(locale))
    }

    @Test
    fun `ddMMyyyy - formats as dd_MM_yyyy`() {
        assertEquals("15.01.2024", date.ddMMyyyy(locale))
    }

    @Test
    fun `ddMMMyyyy - formats as dd MMM yyyy`() {
        assertEquals("15 Jan 2024", date.ddMMMyyyy(locale))
    }

    @Test
    fun `ddMM - formats as dd_MM`() {
        assertEquals("15.01", date.ddMM(locale))
    }

    @Test
    fun `MMMdyyyy - formats as MMM d, yyyy`() {
        assertEquals("Jan 15, 2024", date.MMMdyyyy(locale))
    }

    @Test
    fun `EEEE - formats as full weekday`() {
        assertEquals("Monday", date.EEEE(locale))
    }

    @Test
    fun `ddMMMMyyyy - formats as dd MMMM yyyy`() {
        assertEquals("15 January 2024", date.ddMMMMyyyy(locale))
    }

    @Test
    fun `MMMMyyyy - formats as MMMM yyyy`() {
        assertEquals("January 2024", date.MMMMyyyy(locale))
    }

    @Test
    fun `ddMMMOptionalYear - current year - omits year`() {
        val sameYear = LocalDate.of(LocalDate.now().year, 1, 15)
        assertEquals("15 Jan", sameYear.ddMMMOptionalYear(locale))
    }

    @Test
    fun `ddMMMOptionalYear - other year - includes year`() {
        val pastYear = LocalDate.of(2000, 1, 15)
        assertEquals("15 Jan 2000", pastYear.ddMMMOptionalYear(locale))
    }

    @Test
    fun `rangeOptionalYear - same year - omits year on both ends`() {
        val start = LocalDate.of(2024, 1, 15)
        val end = LocalDate.of(2024, 3, 20)
        assertEquals("15 Jan - 20 Mar", rangeOptionalYear(locale, start, end))
    }

    @Test
    fun `rangeOptionalYear - different years - includes year on both ends`() {
        val start = LocalDate.of(2023, 12, 20)
        val end = LocalDate.of(2024, 1, 5)
        assertEquals("20 Dec 2023 - 05 Jan 2024", rangeOptionalYear(locale, start, end))
    }

    // --- locale change ---

    @Test
    fun `MMMMyyyy - German locale - localizes full month`() {
        assertEquals("Januar 2024", date.MMMMyyyy(Locale.GERMAN))
    }

    @Test
    fun `MMMMyyyy - French locale - localizes full month`() {
        assertEquals("janvier 2024", date.MMMMyyyy(Locale.FRENCH))
    }

    @Test
    fun `EEEE - German locale - localizes full weekday`() {
        assertEquals("Montag", date.EEEE(Locale.GERMAN))
    }

    @Test
    fun `EEEE - French locale - localizes full weekday`() {
        assertEquals("lundi", date.EEEE(Locale.FRENCH))
    }

    @Test
    fun `ddMMMMyyyy - French locale - localizes full month with day and year`() {
        assertEquals("15 janvier 2024", date.ddMMMMyyyy(Locale.FRENCH))
    }

    @Test
    fun `EEEE - shared formatter applies per-call locale, not a fixed one`() {
        // Guards the hoisted withLocale() refactor: same cached formatter, different locales.
        assertNotEquals(date.EEEE(Locale.ENGLISH), date.EEEE(Locale.GERMAN))
    }
}

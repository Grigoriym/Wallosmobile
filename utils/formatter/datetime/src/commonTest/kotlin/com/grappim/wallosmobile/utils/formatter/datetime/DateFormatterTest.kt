package com.grappim.wallosmobile.utils.formatter.datetime

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DateFormatterTest {

    private val formatter = DateFormatter()

    @Test
    fun `parse reads a wire date`() {
        assertEquals(LocalDate(2025, 3, 5), formatter.parseIsoDate("2025-03-05"))
        assertEquals(LocalDate(2026, 12, 31), formatter.parseIsoDate("2026-12-31"))
    }

    @Test
    fun `an unset date is blank, not null, and reads as absent`() {
        // Every active row sends `cancellation_date: ""` (API doc §3.1).
        assertNull(formatter.parseIsoDate(""))
        assertNull(formatter.parseIsoDate("   "))
    }

    @Test
    fun `a date this build cannot read is absent, not a failure`() {
        // One bad value must not sink the list it arrived in.
        assertNull(formatter.parseIsoDate("05/03/2025"))
        assertNull(formatter.parseIsoDate("2025-13-01"))
        assertNull(formatter.parseIsoDate("2025-02-30"))
        assertNull(formatter.parseIsoDate("0000-00-00"))
        assertNull(formatter.parseIsoDate("2025-03-05 00:00:00"))
    }

    @Test
    fun `format writes the shape the API expects`() {
        assertEquals("2025-03-05", formatter.formatIsoDate(LocalDate(2025, 3, 5)))
        assertEquals("2025-12-31", formatter.formatIsoDate(LocalDate(2025, 12, 31)))
    }

    @Test
    fun `format and parse round-trip`() {
        val date = LocalDate(2025, 3, 5)
        assertEquals(date, formatter.parseIsoDate(formatter.formatIsoDate(date)))
    }
}

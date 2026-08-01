package com.grappim.wallosmobile.utils.formatter.decimal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MoneyFormatterTest {

    private val formatter = MoneyFormatter()

    @Test
    fun `format always writes two decimals`() {
        assertEquals("€0.00", formatter.format(0.0, "€"))
        assertEquals("€12.50", formatter.format(12.5, "€"))
        assertEquals("€12.99", formatter.format(12.99, "€"))
        assertEquals("€8.00", formatter.format(8.0, "€"))
    }

    @Test
    fun `format groups thousands the way the server does`() {
        assertEquals("$1,234.56", formatter.format(1234.56, "$"))
        assertEquals("$999.99", formatter.format(999.99, "$"))
        assertEquals("$1,234,567.89", formatter.format(1234567.89, "$"))
    }

    @Test
    fun `format rounds to the nearest cent, half up`() {
        assertEquals("$0.13", formatter.format(0.125, "$"))
        assertEquals("$0.12", formatter.format(0.124, "$"))
    }

    @Test
    fun `format takes the symbol as given`() {
        // Wallos stores an arbitrary string, so nothing here may assume a one-character
        // symbol or an ISO code.
        assertEquals("zł49.00", formatter.format(49.0, "zł"))
        assertEquals("49.00", formatter.format(49.0, ""))
    }

    @Test
    fun `format puts a negative sign ahead of the symbol`() {
        assertEquals("-€5.00", formatter.format(-5.0, "€"))
    }

    @Test
    fun `parse reads a monthly_cost string`() {
        assertEquals(120.24, formatter.parse("120.24"))
        assertEquals(1234.56, formatter.parse("1,234.56"))
        assertEquals(1234567.89, formatter.parse("1,234,567.89"))
        assertEquals(0.0, formatter.parse("0.00"))
    }

    @Test
    fun `parse returns null for anything it cannot read`() {
        // A total the app can't read must not render as zero.
        assertNull(formatter.parse(""))
        assertNull(formatter.parse("   "))
        assertNull(formatter.parse("n/a"))
        assertNull(formatter.parse("€120.24"))
    }

    @Test
    fun `format and parse round-trip`() {
        val formatted = formatter.format(1234.56, "")
        assertEquals(1234.56, formatter.parse(formatted))
    }
}

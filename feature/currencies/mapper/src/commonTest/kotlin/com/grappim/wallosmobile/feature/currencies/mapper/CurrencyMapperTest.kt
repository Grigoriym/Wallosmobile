package com.grappim.wallosmobile.feature.currencies.mapper

import com.grappim.wallosmobile.feature.currencies.dto.CurrencyDTO
import com.grappim.wallosmobile.feature.subscriptions.mapper.HtmlUnescaper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CurrencyMapperTest {

    private val mapper = CurrencyMapper(HtmlUnescaper())

    @Test
    fun `maps a row, parsing rate into a Double`() {
        val result = mapper.toDomain(
            CurrencyDTO(id = 1, name = "Euro", symbol = "€", code = "EUR", rate = "1.1000", inUse = true),
            mainCurrencyId = 3
        )

        assertEquals(1, result.id)
        assertEquals("Euro", result.name)
        assertEquals("€", result.symbol)
        assertEquals("EUR", result.code)
        assertEquals(1.1, result.rate)
        assertTrue(result.inUse)
        assertFalse(result.isMain)
    }

    @Test
    fun `marks the row matching the main currency id`() {
        val result = mapper.toDomain(CurrencyDTO(id = 3, name = "Euro"), mainCurrencyId = 3)

        assertTrue(result.isMain)
    }

    @Test
    fun `falls back to 1_0 when rate isn't parseable`() {
        val result = mapper.toDomain(CurrencyDTO(id = 1, rate = ""), mainCurrencyId = null)

        assertEquals(1.0, result.rate)
    }

    @Test
    fun `unescapes user-entered name and symbol`() {
        val result = mapper.toDomain(CurrencyDTO(id = 9, name = "US &amp; Co", symbol = "&amp;"), mainCurrencyId = null)

        assertEquals("US & Co", result.name)
        assertEquals("&", result.symbol)
    }
}

package com.grappim.wallosmobile.feature.subscriptions.mapper

import com.grappim.wallosmobile.feature.subscriptions.dto.CurrencyDTO
import kotlin.test.Test
import kotlin.test.assertEquals

class CurrencyMapperTest {

    private val mapper = CurrencyMapper(HtmlUnescaper())

    @Test
    fun `maps a row and drops rate and in_use`() {
        val result = mapper.toDomain(
            CurrencyDTO(id = 1, name = "Euro", symbol = "€", code = "EUR", rate = "1", inUse = true)
        )

        assertEquals(1, result.id)
        assertEquals("Euro", result.name)
        assertEquals("€", result.symbol)
        assertEquals("EUR", result.code)
    }

    @Test
    fun `unescapes a user-entered name and symbol`() {
        val result = mapper.toDomain(CurrencyDTO(id = 9, name = "Cash &amp; Coin", symbol = "&amp;"))

        assertEquals("Cash & Coin", result.name)
        assertEquals("&", result.symbol)
    }
}

package com.grappim.wallosmobile.feature.subscriptions.mapper

import com.grappim.wallosmobile.feature.subscriptions.domain.model.Currency
import kotlin.test.Test
import kotlin.test.assertEquals

class CurrencyEntityMapperTest {

    private val mapper = CurrencyEntityMapper()

    @Test
    fun `a currency survives the round trip unchanged`() {
        val currency = Currency(id = 2, name = "US Dollar", symbol = "$", code = "USD")

        assertEquals(currency, mapper.toDomain(mapper.toEntity(currency)))
    }

    /** Already unescaped by `CurrencyMapper` on the way in — nothing here launders it twice. */
    @Test
    fun `the symbol is stored exactly as the model carries it`() {
        val currency = Currency(id = 1, name = "Ampersand & Co", symbol = "&", code = "AMP")

        assertEquals("&", mapper.toEntity(currency).symbol)
        assertEquals("Ampersand & Co", mapper.toEntity(currency).name)
    }
}

package com.grappim.wallosmobile.feature.dashboard.data.mapper

import com.grappim.wallosmobile.core.domain.WallosError
import com.grappim.wallosmobile.feature.dashboard.dto.MonthlyCostDTO
import com.grappim.wallosmobile.utils.formatter.decimal.MoneyFormatter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MonthlyCostMapperTest {

    private val mapper = MonthlyCostMapper(MoneyFormatter())

    @Test
    fun `parses the comma-grouped monthly_cost string`() {
        val dto = MonthlyCostDTO(title = "March 2025", monthlyCost = "1,234.56", currencySymbol = "€")

        val result = mapper.toDomain(dto)

        assertEquals("March 2025", result.title)
        assertEquals(1234.56, result.amount)
        assertEquals("€", result.currencySymbol)
    }

    @Test
    fun `an unparseable monthly_cost fails as Malformed`() {
        val dto = MonthlyCostDTO(title = "March 2025", monthlyCost = "not a number", currencySymbol = "€")

        assertFailsWith<WallosError.Malformed> { mapper.toDomain(dto) }
    }
}

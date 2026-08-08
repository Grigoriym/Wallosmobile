package com.grappim.wallosmobile.feature.dashboard.data.mapper

import com.grappim.wallosmobile.feature.dashboard.dto.PeriodBudgetDTO
import kotlin.test.Test
import kotlin.test.assertEquals

class PeriodBudgetMapperTest {

    private val mapper = PeriodBudgetMapper()

    @Test
    fun `maps every field the domain model keeps`() {
        val dto = PeriodBudgetDTO(
            periodBudget = 100.0,
            amountRemainingThisPeriod = 40.0,
            amountOverBudget = 0.0,
            isOverBudget = false,
            periodLabel = "Aug 1 - Aug 31",
            currencySymbol = "€"
        )

        val result = mapper.toDomain(dto)

        assertEquals("Aug 1 - Aug 31", result.periodLabel)
        assertEquals(100.0, result.periodBudget)
        assertEquals(40.0, result.amountRemainingThisPeriod)
        assertEquals(0.0, result.amountOverBudget)
        assertEquals(false, result.isOverBudget)
        assertEquals("€", result.currencySymbol)
    }
}

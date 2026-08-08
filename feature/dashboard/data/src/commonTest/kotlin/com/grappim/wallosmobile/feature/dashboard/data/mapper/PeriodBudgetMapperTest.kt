package com.grappim.wallosmobile.feature.dashboard.data.mapper

import com.grappim.wallosmobile.feature.dashboard.dto.PeriodBudgetDTO
import com.grappim.wallosmobile.utils.formatter.datetime.DateFormatter
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PeriodBudgetMapperTest {

    private val mapper = PeriodBudgetMapper(DateFormatter())

    @Test
    fun `maps every field the domain model keeps`() {
        val dto = PeriodBudgetDTO(
            periodBudget = 100.0,
            amountRemainingThisPeriod = 40.0,
            amountOverBudget = 0.0,
            isOverBudget = false,
            periodStart = "2025-08-01",
            periodEnd = "2025-08-31",
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
        assertEquals(LocalDate(2025, 8, 1), result.periodStart)
        assertEquals(LocalDate(2025, 8, 31), result.periodEnd)
    }

    @Test
    fun `an unparseable period_start throws rather than silently dropping the period`() {
        val dto = PeriodBudgetDTO(
            periodBudget = 100.0,
            amountRemainingThisPeriod = 40.0,
            amountOverBudget = 0.0,
            isOverBudget = false,
            periodStart = "not-a-date",
            periodEnd = "2025-08-31",
            periodLabel = "Aug 1 - Aug 31",
            currencySymbol = "€"
        )

        assertFailsWith<IllegalArgumentException> { mapper.toDomain(dto) }
    }
}

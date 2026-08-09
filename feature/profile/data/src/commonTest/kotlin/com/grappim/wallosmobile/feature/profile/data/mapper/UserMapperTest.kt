package com.grappim.wallosmobile.feature.profile.data.mapper

import com.grappim.wallosmobile.feature.profile.domain.model.BudgetPeriodType
import com.grappim.wallosmobile.feature.profile.dto.UserDTO
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class UserMapperTest {

    private val mapper = UserMapper()

    @Test
    fun `maps every field the domain model keeps`() {
        val dto = UserDTO(
            id = 1,
            username = "gregorz",
            email = "gregorz@example.com",
            budget = 150.5,
            periodBudget = 25.0,
            mainCurrency = 1,
            budgetPeriodType = "weekly",
            budgetPeriodAnchorDate = "2026-07-18",
            totpEnabled = 0
        )

        val result = mapper.toDomain(dto)

        assertEquals(1, result.id)
        assertEquals("gregorz", result.username)
        assertEquals("gregorz@example.com", result.email)
        assertEquals(150.5, result.budget)
        assertEquals(25.0, result.periodBudget)
        assertEquals(1, result.mainCurrencyId)
        assertEquals(BudgetPeriodType.WEEKLY, result.budgetPeriodType)
        assertEquals(LocalDate(2026, 7, 18), result.budgetPeriodAnchorDate)
        assertFalse(result.totpEnabled)
    }

    @Test
    fun `an unrecognized period type falls back to monthly`() {
        val dto = UserDTO(
            id = 1,
            username = "gregorz",
            email = "gregorz@example.com",
            budget = 0.0,
            periodBudget = 0.0,
            mainCurrency = 1,
            budgetPeriodType = "yearly",
            budgetPeriodAnchorDate = "2026-07-18",
            totpEnabled = 1
        )

        val result = mapper.toDomain(dto)

        assertEquals(BudgetPeriodType.MONTHLY, result.budgetPeriodType)
    }
}

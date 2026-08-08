package com.grappim.wallosmobile.feature.dashboard.domain.model

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PeriodBudgetTest {

    private val today = LocalDate(2026, 8, 8)

    @Test
    fun `redundant when the period is exactly the calendar month`() {
        val periodBudget = periodBudget(periodStart = LocalDate(2026, 8, 1), periodEnd = LocalDate(2026, 8, 31))

        assertTrue(periodBudget.isRedundantWithCalendarMonth(today))
    }

    /** The live instance's own period (`WALLOS_API.md`/M10's preamble): `Jul 18–Aug 17`. */
    @Test
    fun `not redundant when the period is anchored elsewhere`() {
        val periodBudget = periodBudget(periodStart = LocalDate(2026, 7, 18), periodEnd = LocalDate(2026, 8, 17))

        assertFalse(periodBudget.isRedundantWithCalendarMonth(today))
    }

    private fun periodBudget(periodStart: LocalDate, periodEnd: LocalDate) = PeriodBudget(
        periodLabel = "label",
        periodBudget = 100.0,
        amountRemainingThisPeriod = 40.0,
        amountOverBudget = 0.0,
        isOverBudget = false,
        currencySymbol = "€",
        periodStart = periodStart,
        periodEnd = periodEnd
    )
}

package com.grappim.wallosmobile.feature.dashboard.domain.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * The active budget period (`docs/WALLOS_API.md` §3.6), trimmed to what the dashboard card
 * renders. [amountRemainingThisPeriod] is clamped to 0 by the server once spend passes the
 * budget, which is why [amountOverBudget] rides alongside rather than being derived from it.
 */
data class PeriodBudget(
    val periodLabel: String,
    val periodBudget: Double,
    val amountRemainingThisPeriod: Double,
    val amountOverBudget: Double,
    val isOverBudget: Boolean,
    val currencySymbol: String,
    val periodStart: LocalDate,
    val periodEnd: LocalDate
) {
    /**
     * Mirrors `stats_calculations.php:290–293`: a monthly period whose anchor lands exactly on
     * the calendar month's own boundaries carries nothing this card doesn't already show via the
     * monthly budget, so the period budget card is redundant and gets hidden.
     */
    fun isRedundantWithCalendarMonth(today: LocalDate): Boolean {
        val calendarMonthStart = LocalDate(today.year, today.month, 1)
        val calendarMonthEnd = calendarMonthStart.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
        return periodStart == calendarMonthStart && periodEnd == calendarMonthEnd
    }
}

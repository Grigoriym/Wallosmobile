package com.grappim.wallosmobile.feature.dashboard.domain.model

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
    val currencySymbol: String
)

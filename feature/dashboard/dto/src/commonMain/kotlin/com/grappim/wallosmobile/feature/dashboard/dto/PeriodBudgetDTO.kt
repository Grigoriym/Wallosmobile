package com.grappim.wallosmobile.feature.dashboard.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `get_period_budget.php` (`docs/WALLOS_API.md` §3.6). `budget_period_type`,
 * `budget_period_anchor_date` and `reference_date` still aren't modeled — [periodLabel] is
 * already the human-readable string the server derives from them. `period_start`/`period_end`
 * are restored (10.2) to compute [com.grappim.wallosmobile.feature.dashboard.domain.model.PeriodBudget]'s
 * `isRedundantWithCalendarMonth`. `amount_needed_this_period`/`amount_needed_full_period` are
 * projections the dashboard card doesn't show either.
 */
@Serializable
data class PeriodBudgetDTO(
    @SerialName("period_budget") val periodBudget: Double,
    @SerialName("amount_remaining_this_period") val amountRemainingThisPeriod: Double,
    @SerialName("amount_over_budget") val amountOverBudget: Double,
    @SerialName("is_over_budget") val isOverBudget: Boolean,
    @SerialName("period_start") val periodStart: String,
    @SerialName("period_end") val periodEnd: String,
    @SerialName("period_label") val periodLabel: String,
    @SerialName("currency_symbol") val currencySymbol: String
)

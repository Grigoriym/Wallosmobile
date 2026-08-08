package com.grappim.wallosmobile.feature.dashboard.ui

import com.grappim.wallosmobile.utils.ui.NativeText
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** One row of the upcoming-payments list — already the rolled-forward, sorted, active-only set 8.2 computes. */
data class UpcomingPaymentUiItem(val id: Int, val name: String, val price: String, val nextPayment: String)

/**
 * @param title the server's own period label (`MonthlyCost.title`, e.g. "August 2026"), blank
 *   while loading or on [error].
 */
data class MonthlyCostCardUiState(
    val title: String = "",
    val amount: String = "",
    val error: NativeText = NativeText.Empty
)

/**
 * @param isHidden the version-gating decision M8's preamble settled: an instance without
 *   `get_period_budget` (`WallosError.UnsupportedEndpoint`) hides this card rather than showing
 *   [error] — there is nothing wrong to report, the endpoint just isn't there.
 * @param amountOverBudget shown only behind [isOverBudget]: the server clamps
 *   `amountRemainingThisPeriod` to 0 once spend passes the budget, so this is the only field left
 *   that says by how much.
 */
data class PeriodBudgetCardUiState(
    val isHidden: Boolean = false,
    val periodLabel: String = "",
    val budgetAmount: String = "",
    val remainingAmount: String = "",
    val isOverBudget: Boolean = false,
    val amountOverBudget: String = "",
    val error: NativeText = NativeText.Empty
)

/**
 * No cache behind any of the three sources (M8 preamble), so [isLoading] is the only load state
 * this screen has — there is no cached row to leave standing under a failed refresh the way the
 * subscriptions list does.
 */
data class DashboardUiState(
    val isLoading: Boolean = false,
    val monthlyCost: MonthlyCostCardUiState = MonthlyCostCardUiState(),
    val periodBudget: PeriodBudgetCardUiState = PeriodBudgetCardUiState(),
    val upcomingPayments: ImmutableList<UpcomingPaymentUiItem> = persistentListOf(),
    val onRetryClick: () -> Unit = {}
)

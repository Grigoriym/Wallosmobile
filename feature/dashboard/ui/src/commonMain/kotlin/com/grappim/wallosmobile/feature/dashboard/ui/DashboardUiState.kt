package com.grappim.wallosmobile.feature.dashboard.ui

import com.grappim.wallosmobile.utils.ui.NativeText
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** One row of a subscription-and-date list — shared by the Overdue and Upcoming sections, whose rows are the same shape. */
data class UpcomingPaymentUiItem(val id: Int, val name: String, val price: String, val nextPayment: String)

/**
 * @param title the server's own period label (`MonthlyCost.title`, e.g. "August 2026"), blank
 *   while loading or on [error]. [costAmount] is shown whenever loaded — summed locally from
 *   active subscriptions (`SubscriptionStatsCalculator`, 10.8), not `MonthlyCost.amount`, which is
 *   a different metric (`get_monthly_cost.php`'s own per-occurrence total) the web dashboard never
 *   renders. [budgetAmount], [usedPercent], [remainingAmount] and [overBudgetAmount] stay blank
 *   when the account has no monthly budget set (`MonthlyBudget.from`'s own `null` gate, 10.2).
 */
data class MonthlyBudgetCardUiState(
    val title: String = "",
    val costAmount: String = "",
    val budgetAmount: String = "",
    val usedPercent: String = "",
    val remainingAmount: String = "",
    val isOverBudget: Boolean = false,
    val overBudgetAmount: String = "",
    val error: NativeText = NativeText.Empty
)

/**
 * @param isHidden any of three gates: the version-gating decision M8's preamble settled (an
 *   instance without `get_period_budget`, `WallosError.UnsupportedEndpoint`, hides this card
 *   rather than showing [error]), 10.2's `PeriodBudget.isRedundantWithCalendarMonth` (the active
 *   period is a plain calendar month, so this card would repeat what Monthly Budget already
 *   shows), or 10.9's `periodBudget <= 0` (the account has no period budget set at all, so
 *   `get_period_budget.php` still answers `success: true` with a zeroed budget the web itself
 *   never renders a card for).
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

/** Shown only when [activeCount] is greater than 0 (`index.php:373`'s own gate). */
data class YourSubscriptionsCardUiState(
    val activeCount: Int = 0,
    val monthlyCost: String = "",
    val yearlyCost: String = ""
)

/** Shown only when [inactiveCount] is greater than 0 (`index.php:411`'s own gate). */
data class YourSavingsCardUiState(val inactiveCount: Int = 0, val savingsPerMonth: String = "")

/**
 * No cache behind any of the sources (M8 preamble), so [isLoading] is the only load state this
 * screen has — there is no cached row to leave standing under a failed refresh the way the
 * subscriptions list does.
 */
data class DashboardUiState(
    val isLoading: Boolean = false,
    val monthlyBudget: MonthlyBudgetCardUiState = MonthlyBudgetCardUiState(),
    val periodBudget: PeriodBudgetCardUiState = PeriodBudgetCardUiState(),
    val overdueRenewals: ImmutableList<UpcomingPaymentUiItem> = persistentListOf(),
    val upcomingPayments: ImmutableList<UpcomingPaymentUiItem> = persistentListOf(),
    val yourSubscriptions: YourSubscriptionsCardUiState = YourSubscriptionsCardUiState(),
    val yourSavings: YourSavingsCardUiState = YourSavingsCardUiState(),
    val onRetryClick: () -> Unit = {}
)

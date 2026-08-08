package com.grappim.wallosmobile.feature.dashboard.domain.model

import com.grappim.wallosmobile.feature.dashboard.domain.calculator.SubscriptionStats
import com.grappim.wallosmobile.feature.profile.domain.model.User
import com.grappim.wallosmobile.feature.subscriptions.domain.model.Subscription

/**
 * The sources [com.grappim.wallosmobile.feature.dashboard.domain.usecase.DashboardHomeUseCase]
 * composes, kept as independent [Result]s rather than one failure sinking the whole screen — an
 * old instance without `get_period_budget` (`WallosError.UnsupportedEndpoint`) must not blank out
 * [monthlyCost], [user] or [upcomingPayments], which came from elsewhere. [monthlyBudget],
 * [subscriptionStats] and [isPeriodBudgetRedundant] are derived, not fetched — both
 * [subscriptionStats]'s own cost figure and [monthlyBudget]'s are summed locally from the
 * subscriptions cache (10.8), not from [monthlyCost]'s `.amount`. [monthlyBudget] and
 * [subscriptionStats] are still `null` rather than a zeroed-out instance whenever [monthlyCost]
 * itself failed, though — both format their numbers using its `.currencySymbol`, so a failed fetch
 * still means nothing to format with. [monthlyBudget] additionally needs [user] to have succeeded,
 * since it reads `User.budget`. [isPeriodBudgetRedundant] is computed here, against the same `today` this use
 * case already receives, rather than by the UI re-deriving it from a live clock — that would make
 * the card's hidden state depend on the wall-clock date a ViewModel test happens to run on
 * instead of a fixed value a test controls (`PeriodBudget.isRedundantWithCalendarMonth`, 10.2).
 */
data class DashboardHomeData(
    val monthlyCost: Result<MonthlyCost>,
    val periodBudget: Result<PeriodBudget>,
    val isPeriodBudgetRedundant: Boolean,
    val user: Result<User>,
    val upcomingPayments: List<Subscription>,
    val overdueRenewals: List<Subscription>,
    val monthlyBudget: MonthlyBudget?,
    val subscriptionStats: SubscriptionStats?
)

package com.grappim.wallosmobile.feature.dashboard.domain.model

import com.grappim.wallosmobile.feature.dashboard.domain.calculator.SubscriptionStats
import com.grappim.wallosmobile.feature.profile.domain.model.User
import com.grappim.wallosmobile.feature.subscriptions.domain.model.Subscription

/**
 * The sources [com.grappim.wallosmobile.feature.dashboard.domain.usecase.DashboardHomeUseCase]
 * composes, kept as independent [Result]s rather than one failure sinking the whole screen — an
 * old instance without `get_period_budget` (`WallosError.UnsupportedEndpoint`) must not blank out
 * [monthlyCost], [user] or [upcomingPayments], which came from elsewhere. [monthlyBudget] and
 * [subscriptionStats] are derived, not fetched, and are `null` rather than a zeroed-out instance
 * whenever [monthlyCost] itself failed — [monthlyBudget] additionally needs [user] to have
 * succeeded, since it reads `User.budget`.
 */
data class DashboardHomeData(
    val monthlyCost: Result<MonthlyCost>,
    val periodBudget: Result<PeriodBudget>,
    val user: Result<User>,
    val upcomingPayments: List<Subscription>,
    val overdueRenewals: List<Subscription>,
    val monthlyBudget: MonthlyBudget?,
    val subscriptionStats: SubscriptionStats?
)

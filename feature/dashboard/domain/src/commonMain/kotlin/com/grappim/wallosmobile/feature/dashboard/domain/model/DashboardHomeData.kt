package com.grappim.wallosmobile.feature.dashboard.domain.model

import com.grappim.wallosmobile.feature.subscriptions.domain.model.Subscription

/**
 * The three sources [com.grappim.wallosmobile.feature.dashboard.domain.usecase.DashboardHomeUseCase]
 * composes, kept as independent [Result]s rather than one failure sinking the whole screen — an
 * old instance without `get_period_budget` (`WallosError.UnsupportedEndpoint`) must not blank out
 * [monthlyCost] or [upcomingPayments], which came from elsewhere.
 */
data class DashboardHomeData(
    val monthlyCost: Result<MonthlyCost>,
    val periodBudget: Result<PeriodBudget>,
    val upcomingPayments: List<Subscription>,
    val overdueRenewals: List<Subscription>
)

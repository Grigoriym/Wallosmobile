package com.grappim.wallosmobile.feature.dashboard.domain.usecase

import com.grappim.wallosmobile.feature.dashboard.domain.calculator.UpcomingPaymentsCalculator
import com.grappim.wallosmobile.feature.dashboard.domain.model.DashboardHomeData
import com.grappim.wallosmobile.feature.dashboard.domain.repo.DashboardRepository
import com.grappim.wallosmobile.feature.subscriptions.domain.repo.SubscriptionsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.Factory

/**
 * The app's first real use case (plan §6): a dashboard screen needs three sources, not one call.
 * `getMonthlyCost`/`getPeriodBudget` are independent round trips (`DashboardRepository`, no
 * cache), while the upcoming-payments list is derived from the subscriptions cache that's already
 * there — a single current snapshot, not a re-fetch, since nothing on this screen writes and the
 * cache is refreshed elsewhere.
 */
interface DashboardHomeUseCase {
    suspend fun getDashboardHomeData(today: LocalDate): DashboardHomeData
}

@Factory(binds = [DashboardHomeUseCase::class])
class DashboardHomeUseCaseImpl(
    private val dashboardRepository: DashboardRepository,
    private val subscriptionsRepository: SubscriptionsRepository
) : DashboardHomeUseCase {

    private val upcomingPaymentsCalculator = UpcomingPaymentsCalculator()

    override suspend fun getDashboardHomeData(today: LocalDate): DashboardHomeData = coroutineScope {
        val monthlyCostDeferred = async { dashboardRepository.getMonthlyCost(today.monthNumber, today.year) }
        val periodBudgetDeferred = async { dashboardRepository.getPeriodBudget(today) }
        val subscriptions = subscriptionsRepository.observeSubscriptions().first()

        DashboardHomeData(
            monthlyCost = monthlyCostDeferred.await(),
            periodBudget = periodBudgetDeferred.await(),
            upcomingPayments = upcomingPaymentsCalculator.calculate(subscriptions, today)
        )
    }
}

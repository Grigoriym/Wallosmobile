package com.grappim.wallosmobile.feature.dashboard.domain.usecase

import com.grappim.wallosmobile.core.logger.LogPriority
import com.grappim.wallosmobile.core.logger.logcat
import com.grappim.wallosmobile.feature.dashboard.domain.calculator.SubscriptionStatsCalculator
import com.grappim.wallosmobile.feature.dashboard.domain.calculator.UpcomingPaymentsCalculator
import com.grappim.wallosmobile.feature.dashboard.domain.model.DashboardHomeData
import com.grappim.wallosmobile.feature.dashboard.domain.model.MonthlyBudget
import com.grappim.wallosmobile.feature.dashboard.domain.repo.DashboardRepository
import com.grappim.wallosmobile.feature.profile.domain.repo.ProfileRepository
import com.grappim.wallosmobile.feature.subscriptions.domain.repo.SubscriptionsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.Factory

/**
 * The app's first real use case (plan §6): a dashboard screen needs several sources, not one call.
 * `getMonthlyCost`/`getPeriodBudget`/`getUser` are independent round trips (`DashboardRepository`,
 * `ProfileRepository`, no cache). The upcoming-payments/overdue-renewals lists and the subscription
 * stats are derived from the subscriptions cache, which this use case refreshes itself before
 * reading — like a web page reload, every dashboard open is a fresh fetch, not a read of whatever
 * the *last* screen that happened to call `refreshSubscriptions()` left behind (found 2026-08-24,
 * `docs/issues/2026-08-24-dashboard-zeros-after-login.md`: before this, a freshly-logged-in user
 * landed on an empty cache with nothing showing zero/empty until they happened to visit the
 * Subscriptions tab). A refresh failure is logged and otherwise ignored here — same offline-first
 * contract as everywhere else: the cache is left exactly as it was, and whatever it holds (possibly
 * still empty) is what gets read below.
 *
 * `monthlyBudget`/`subscriptionStats` are still gated on `monthlyCost.getOrNull()` (10.8) even
 * though neither reads its `.amount` any more — both format their own numbers using its
 * `.currencySymbol`, so a failed `getMonthlyCost` still means "nothing to format with," not "no
 * cost data." `getMonthlyCost` stays fetched purely for `MonthlyCost.title`/`.currencySymbol`.
 */
interface DashboardHomeUseCase {
    suspend fun getDashboardHomeData(today: LocalDate): DashboardHomeData
}

@Factory(binds = [DashboardHomeUseCase::class])
class DashboardHomeUseCaseImpl(
    private val dashboardRepository: DashboardRepository,
    private val subscriptionsRepository: SubscriptionsRepository,
    private val profileRepository: ProfileRepository
) : DashboardHomeUseCase {

    private val upcomingPaymentsCalculator = UpcomingPaymentsCalculator()
    private val subscriptionStatsCalculator = SubscriptionStatsCalculator()

    override suspend fun getDashboardHomeData(today: LocalDate): DashboardHomeData = coroutineScope {
        val monthlyCostDeferred = async { dashboardRepository.getMonthlyCost(today.monthNumber, today.year) }
        val periodBudgetDeferred = async { dashboardRepository.getPeriodBudget(today) }
        val userDeferred = async { profileRepository.getUser() }
        val subscriptionsRefreshDeferred = async { subscriptionsRepository.refreshSubscriptions() }

        subscriptionsRefreshDeferred.await().onFailure { throwable ->
            logcat(priority = LogPriority.WARN, throwable = throwable) {
                "Refreshing subscriptions for dashboard failed"
            }
        }
        val subscriptions = subscriptionsRepository.observeSubscriptions().first()
        val upcomingAndOverdue = upcomingPaymentsCalculator.calculate(subscriptions, today)

        val monthlyCost = monthlyCostDeferred.await()
        val user = userDeferred.await()
        val periodBudget = periodBudgetDeferred.await()
        val subscriptionStats = subscriptionStatsCalculator.calculate(subscriptions)

        DashboardHomeData(
            monthlyCost = monthlyCost,
            periodBudget = periodBudget,
            isPeriodBudgetRedundant = periodBudget.getOrNull()?.isRedundantWithCalendarMonth(today) == true,
            user = user,
            upcomingPayments = upcomingAndOverdue.upcoming,
            overdueRenewals = upcomingAndOverdue.overdue,
            monthlyBudget = monthlyCost.getOrNull()?.let {
                val activeMonthlyCost = subscriptionStats.yourSubscriptions.monthlyCost
                user.getOrNull()?.let { u -> MonthlyBudget.from(u.budget, activeMonthlyCost) }
            },
            subscriptionStats = monthlyCost.getOrNull()?.let { subscriptionStats }
        )
    }
}

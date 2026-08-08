package com.grappim.wallosmobile.feature.dashboard.domain.usecase

import com.grappim.wallosmobile.core.domain.WallosError
import com.grappim.wallosmobile.feature.dashboard.domain.calculator.SubscriptionStats
import com.grappim.wallosmobile.feature.dashboard.domain.model.MonthlyBudget
import com.grappim.wallosmobile.feature.dashboard.domain.model.MonthlyCost
import com.grappim.wallosmobile.feature.dashboard.domain.model.PeriodBudget
import com.grappim.wallosmobile.feature.dashboard.domain.model.YourSavings
import com.grappim.wallosmobile.feature.dashboard.domain.model.YourSubscriptions
import com.grappim.wallosmobile.feature.dashboard.domain.repo.DashboardRepository
import com.grappim.wallosmobile.feature.profile.domain.model.User
import com.grappim.wallosmobile.feature.profile.domain.repo.ProfileRepository
import com.grappim.wallosmobile.feature.subscriptions.domain.model.AddSubscriptionParams
import com.grappim.wallosmobile.feature.subscriptions.domain.model.BillingCycle
import com.grappim.wallosmobile.feature.subscriptions.domain.model.Currency
import com.grappim.wallosmobile.feature.subscriptions.domain.model.EditSubscriptionParams
import com.grappim.wallosmobile.feature.subscriptions.domain.model.PriceConversion
import com.grappim.wallosmobile.feature.subscriptions.domain.model.Subscription
import com.grappim.wallosmobile.feature.subscriptions.domain.repo.SubscriptionsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DashboardHomeUseCaseTest {

    private val today = LocalDate(2026, 8, 8)

    @Test
    fun `all sources are present`() = runTest {
        val monthlyCost = MonthlyCost(title = "August 2026", amount = 42.0, currencySymbol = "€")
        val periodBudget = PeriodBudget(
            periodLabel = "Aug 1 - Aug 31",
            periodBudget = 100.0,
            amountRemainingThisPeriod = 58.0,
            amountOverBudget = 0.0,
            isOverBudget = false,
            currencySymbol = "€",
            periodStart = LocalDate(2026, 8, 1),
            periodEnd = LocalDate(2026, 8, 31)
        )
        val user = user(budget = 50.0)
        val sub = subscription(id = 1).copy(nextPayment = LocalDate(2026, 8, 20), price = 42.0)
        val dashboardRepository = FakeDashboardRepository(
            monthlyCost = Result.success(monthlyCost),
            periodBudget = Result.success(periodBudget)
        )
        val subscriptionsRepository = FakeSubscriptionsRepository(listOf(sub))
        val profileRepository = FakeProfileRepository(Result.success(user))

        val result = useCase(dashboardRepository, subscriptionsRepository, profileRepository)
            .getDashboardHomeData(today)

        assertEquals(monthlyCost, result.monthlyCost.getOrThrow())
        assertEquals(periodBudget, result.periodBudget.getOrThrow())
        assertTrue(result.isPeriodBudgetRedundant)
        assertEquals(user, result.user.getOrThrow())
        assertEquals(listOf(1), result.upcomingPayments.map { it.id })
        assertEquals(emptyList(), result.overdueRenewals)
        assertEquals(MonthlyBudget.from(budget = 50.0, monthlyCost = 42.0), result.monthlyBudget)
        assertEquals(
            SubscriptionStats(
                yourSubscriptions = YourSubscriptions(activeCount = 1, monthlyCost = 42.0, yearlyCost = 504.0),
                yourSavings = YourSavings(inactiveCount = 0, savingsPerMonth = 0.0)
            ),
            result.subscriptionStats
        )
        assertEquals(8, dashboardRepository.lastMonth)
        assertEquals(2026, dashboardRepository.lastYear)
        assertEquals(today, dashboardRepository.lastReferenceDate)
    }

    @Test
    fun `a period-budget failure still leaves monthly cost and upcoming payments populated`() = runTest {
        val monthlyCost = MonthlyCost(title = "August 2026", amount = 42.0, currencySymbol = "€")
        val sub = subscription(id = 1).copy(nextPayment = LocalDate(2026, 8, 20))
        val dashboardRepository = FakeDashboardRepository(
            monthlyCost = Result.success(monthlyCost),
            periodBudget = Result.failure(WallosError.UnsupportedEndpoint)
        )
        val subscriptionsRepository = FakeSubscriptionsRepository(listOf(sub))

        val result = useCase(dashboardRepository, subscriptionsRepository).getDashboardHomeData(today)

        assertEquals(monthlyCost, result.monthlyCost.getOrThrow())
        assertTrue(result.periodBudget.isFailure)
        assertEquals(WallosError.UnsupportedEndpoint, result.periodBudget.exceptionOrNull())
        assertFalse(result.isPeriodBudgetRedundant)
        assertEquals(listOf(1), result.upcomingPayments.map { it.id })
    }

    @Test
    fun `a period anchored off the calendar month is not redundant`() = runTest {
        val periodBudget = PeriodBudget(
            periodLabel = "Jul 18 - Aug 17",
            periodBudget = 100.0,
            amountRemainingThisPeriod = 58.0,
            amountOverBudget = 0.0,
            isOverBudget = false,
            currencySymbol = "€",
            periodStart = LocalDate(2026, 7, 18),
            periodEnd = LocalDate(2026, 8, 17)
        )
        val dashboardRepository = FakeDashboardRepository(
            monthlyCost = Result.success(MonthlyCost(title = "August 2026", amount = 42.0, currencySymbol = "€")),
            periodBudget = Result.success(periodBudget)
        )
        val subscriptionsRepository = FakeSubscriptionsRepository(emptyList())

        val result = useCase(dashboardRepository, subscriptionsRepository).getDashboardHomeData(today)

        assertFalse(result.isPeriodBudgetRedundant)
    }

    @Test
    fun `a getUser failure leaves the rest of DashboardHomeData populated`() = runTest {
        val monthlyCost = MonthlyCost(title = "August 2026", amount = 42.0, currencySymbol = "€")
        val periodBudget = PeriodBudget(
            periodLabel = "Aug 1 - Aug 31",
            periodBudget = 100.0,
            amountRemainingThisPeriod = 58.0,
            amountOverBudget = 0.0,
            isOverBudget = false,
            currencySymbol = "€",
            periodStart = LocalDate(2026, 8, 1),
            periodEnd = LocalDate(2026, 8, 31)
        )
        val sub = subscription(id = 1).copy(nextPayment = LocalDate(2026, 8, 20), price = 42.0)
        val dashboardRepository = FakeDashboardRepository(
            monthlyCost = Result.success(monthlyCost),
            periodBudget = Result.success(periodBudget)
        )
        val subscriptionsRepository = FakeSubscriptionsRepository(listOf(sub))
        val profileRepository = FakeProfileRepository(Result.failure(WallosError.Server("boom")))

        val result = useCase(dashboardRepository, subscriptionsRepository, profileRepository)
            .getDashboardHomeData(today)

        assertTrue(result.user.isFailure)
        assertEquals(WallosError.Server("boom"), result.user.exceptionOrNull())
        assertEquals(monthlyCost, result.monthlyCost.getOrThrow())
        assertEquals(periodBudget, result.periodBudget.getOrThrow())
        assertEquals(listOf(1), result.upcomingPayments.map { it.id })
        assertNull(result.monthlyBudget)
        assertEquals(
            SubscriptionStats(
                yourSubscriptions = YourSubscriptions(activeCount = 1, monthlyCost = 42.0, yearlyCost = 504.0),
                yourSavings = YourSavings(inactiveCount = 0, savingsPerMonth = 0.0)
            ),
            result.subscriptionStats
        )
    }

    @Test
    fun `a monthly-cost failure leaves monthly budget and subscription stats absent, not zeroed`() = runTest {
        val dashboardRepository = FakeDashboardRepository(
            monthlyCost = Result.failure(WallosError.Server("boom")),
            periodBudget = Result.failure(WallosError.UnsupportedEndpoint)
        )
        val subscriptionsRepository = FakeSubscriptionsRepository(emptyList())
        val profileRepository = FakeProfileRepository(Result.success(user(budget = 50.0)))

        val result = useCase(dashboardRepository, subscriptionsRepository, profileRepository)
            .getDashboardHomeData(today)

        assertTrue(result.monthlyCost.isFailure)
        assertNull(result.monthlyBudget)
        assertNull(result.subscriptionStats)
    }

    @Test
    fun `an upcoming-payments feed with no active subscriptions renders empty, not failed`() = runTest {
        val dashboardRepository = FakeDashboardRepository(
            monthlyCost = Result.success(MonthlyCost(title = "August 2026", amount = 0.0, currencySymbol = "€")),
            periodBudget = Result.failure(WallosError.UnsupportedEndpoint)
        )
        val subscriptionsRepository = FakeSubscriptionsRepository(emptyList())

        val result = useCase(dashboardRepository, subscriptionsRepository).getDashboardHomeData(today)

        assertEquals(emptyList(), result.upcomingPayments)
        assertEquals(emptyList(), result.overdueRenewals)
    }

    @Test
    fun `a past-due non-auto-renewing subscription surfaces as an overdue renewal`() = runTest {
        val dashboardRepository = FakeDashboardRepository(
            monthlyCost = Result.success(MonthlyCost(title = "August 2026", amount = 0.0, currencySymbol = "€")),
            periodBudget = Result.failure(WallosError.UnsupportedEndpoint)
        )
        val sub = subscription(id = 1).copy(autoRenew = false, nextPayment = LocalDate(2026, 8, 1))
        val subscriptionsRepository = FakeSubscriptionsRepository(listOf(sub))

        val result = useCase(dashboardRepository, subscriptionsRepository).getDashboardHomeData(today)

        assertEquals(listOf(1), result.overdueRenewals.map { it.id })
        assertEquals(emptyList(), result.upcomingPayments)
    }

    private fun useCase(
        dashboardRepository: DashboardRepository,
        subscriptionsRepository: SubscriptionsRepository,
        profileRepository: ProfileRepository = FakeProfileRepository(Result.success(user()))
    ): DashboardHomeUseCase = DashboardHomeUseCaseImpl(dashboardRepository, subscriptionsRepository, profileRepository)

    private fun user(budget: Double = 0.0) = User(id = 1, budget = budget, periodBudget = 0.0, mainCurrencyId = 1)

    private fun subscription(id: Int) = Subscription(
        id = id,
        name = "",
        logo = "",
        price = 0.0,
        currencyId = 1,
        currencySymbol = "€",
        cycle = BillingCycle.MONTHS,
        frequency = 1,
        nextPayment = null,
        startDate = null,
        isActive = true,
        notes = "",
        url = "",
        categoryName = "",
        paymentMethodName = "",
        payerName = "",
        categoryId = null,
        paymentMethodId = null,
        payerUserId = null,
        autoRenew = true,
        notify = false,
        notifyDaysBefore = null
    )

    private class FakeDashboardRepository(
        private val monthlyCost: Result<MonthlyCost>,
        private val periodBudget: Result<PeriodBudget>
    ) : DashboardRepository {

        var lastMonth: Int? = null
            private set
        var lastYear: Int? = null
            private set
        var lastReferenceDate: LocalDate? = null
            private set

        override suspend fun getMonthlyCost(month: Int, year: Int): Result<MonthlyCost> {
            lastMonth = month
            lastYear = year
            return monthlyCost
        }

        override suspend fun getPeriodBudget(referenceDate: LocalDate?): Result<PeriodBudget> {
            lastReferenceDate = referenceDate
            return periodBudget
        }
    }

    private class FakeProfileRepository(private val user: Result<User>) : ProfileRepository {
        override suspend fun getUser(): Result<User> = user
    }

    private class FakeSubscriptionsRepository(private val subscriptions: List<Subscription>) : SubscriptionsRepository {

        override fun observeSubscriptions(): Flow<List<Subscription>> = flowOf(subscriptions)

        override fun observePriceConversion(): Flow<PriceConversion> = MutableStateFlow(PriceConversion())

        override fun observeCurrencies(): Flow<List<Currency>> = flowOf(emptyList())

        override suspend fun refreshSubscriptions(): Result<Unit> = error("not used by this test")

        override fun observeSubscription(id: Int): Flow<Subscription?> = flowOf(subscriptions).map { list ->
            list.firstOrNull { it.id == id }
        }

        override suspend fun refreshSubscription(id: Int): Result<Unit> = error("not used by this test")

        override suspend fun addSubscription(params: AddSubscriptionParams): Result<Int> =
            error("not used by this test")

        override suspend fun editSubscription(id: Int, params: EditSubscriptionParams): Result<Unit> =
            error("not used by this test")

        override suspend fun deleteSubscription(id: Int): Result<Unit> = error("not used by this test")
    }
}

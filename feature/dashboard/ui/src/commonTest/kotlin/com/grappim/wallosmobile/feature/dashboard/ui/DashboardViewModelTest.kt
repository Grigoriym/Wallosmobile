package com.grappim.wallosmobile.feature.dashboard.ui

import com.grappim.wallosmobile.core.domain.WallosError
import com.grappim.wallosmobile.feature.dashboard.domain.model.DashboardHomeData
import com.grappim.wallosmobile.feature.dashboard.domain.model.MonthlyBudget
import com.grappim.wallosmobile.feature.dashboard.domain.model.MonthlyCost
import com.grappim.wallosmobile.feature.dashboard.domain.model.PeriodBudget
import com.grappim.wallosmobile.feature.dashboard.domain.usecase.DashboardHomeUseCase
import com.grappim.wallosmobile.feature.profile.domain.model.User
import com.grappim.wallosmobile.feature.subscriptions.domain.model.BillingCycle
import com.grappim.wallosmobile.feature.subscriptions.domain.model.Subscription
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.error_not_found
import com.grappim.wallosmobile.testing.MainDispatcherRule
import com.grappim.wallosmobile.utils.formatter.datetime.DateFormatter
import com.grappim.wallosmobile.utils.formatter.decimal.MoneyFormatter
import com.grappim.wallosmobile.utils.ui.NativeText
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DashboardViewModelTest {

    private val useCase = FakeDashboardHomeUseCase()
    private val mainDispatcherRule = MainDispatcherRule()

    /** Neither the ViewModel nor these tests read `subscriptionStats` yet — that's 10.7. */
    private val user = Result.success(User(id = 1, budget = 0.0, periodBudget = 0.0, mainCurrencyId = 1))

    private val periodBudget = PeriodBudget(
        periodLabel = "Aug 1 - Aug 31",
        periodBudget = 100.0,
        amountRemainingThisPeriod = 58.0,
        amountOverBudget = 0.0,
        isOverBudget = false,
        currencySymbol = "€",
        periodStart = LocalDate(2026, 8, 1),
        periodEnd = LocalDate(2026, 8, 31)
    )

    @BeforeTest
    fun setup() {
        mainDispatcherRule.setup()
    }

    @AfterTest
    fun tearDown() {
        mainDispatcherRule.tearDown()
    }

    private fun viewModel() = DashboardViewModel(
        dashboardHomeUseCase = useCase,
        moneyFormatter = MoneyFormatter(),
        dateFormatter = DateFormatter()
    )

    @Test
    fun `all three sources render once the use case answers`() = runTest {
        useCase.result = DashboardHomeData(
            monthlyCost = Result.success(MonthlyCost(title = "August 2026", amount = 42.0, currencySymbol = "€")),
            periodBudget = Result.success(periodBudget),
            isPeriodBudgetRedundant = false,
            upcomingPayments = listOf(subscription(id = 1, nextPayment = LocalDate(2026, 8, 20))),
            overdueRenewals = emptyList(),
            user = user,
            monthlyBudget = null,
            subscriptionStats = null
        )

        val state = viewModel().uiState.value

        assertFalse(state.isLoading)
        assertEquals("August 2026", state.monthlyBudget.title)
        assertEquals("€42.00", state.monthlyBudget.costAmount)
        assertFalse(state.periodBudget.isHidden)
        assertEquals("€58.00", state.periodBudget.remainingAmount)
        assertFalse(state.periodBudget.isOverBudget)
        assertEquals(1, state.upcomingPayments.size)
        assertEquals("Disney+", state.upcomingPayments.first().name)
        assertEquals("20 Aug 2026", state.upcomingPayments.first().nextPayment)
    }

    /** M8's version-gating decision: `UnsupportedEndpoint` hides the card rather than erroring it. */
    @Test
    fun `an unsupported period-budget endpoint hides the card instead of showing an error`() = runTest {
        useCase.result = DashboardHomeData(
            monthlyCost = Result.success(MonthlyCost(title = "August 2026", amount = 42.0, currencySymbol = "€")),
            periodBudget = Result.failure(WallosError.UnsupportedEndpoint),
            isPeriodBudgetRedundant = false,
            upcomingPayments = emptyList(),
            overdueRenewals = emptyList(),
            user = user,
            monthlyBudget = null,
            subscriptionStats = null
        )

        val state = viewModel().uiState.value

        assertTrue(state.periodBudget.isHidden)
        assertTrue(state.periodBudget.error.isEmpty())
    }

    /** 10.2/10.6: a period anchored on the calendar month repeats Monthly Budget, so it's hidden too. */
    @Test
    fun `a period budget redundant with the calendar month hides the card even though it succeeded`() = runTest {
        useCase.result = DashboardHomeData(
            monthlyCost = Result.success(MonthlyCost(title = "August 2026", amount = 42.0, currencySymbol = "€")),
            periodBudget = Result.success(periodBudget),
            isPeriodBudgetRedundant = true,
            upcomingPayments = emptyList(),
            overdueRenewals = emptyList(),
            user = user,
            monthlyBudget = null,
            subscriptionStats = null
        )

        val state = viewModel().uiState.value

        assertTrue(state.periodBudget.isHidden)
        assertTrue(state.periodBudget.error.isEmpty())
    }

    /** Any other period-budget failure is a real error, and the card still shows it. */
    @Test
    fun `any other period-budget failure shows an error rather than hiding the card`() = runTest {
        useCase.result = DashboardHomeData(
            monthlyCost = Result.success(MonthlyCost(title = "August 2026", amount = 42.0, currencySymbol = "€")),
            periodBudget = Result.failure(WallosError.NotFound("Unauthorized or Not Found")),
            isPeriodBudgetRedundant = false,
            upcomingPayments = emptyList(),
            overdueRenewals = emptyList(),
            user = user,
            monthlyBudget = null,
            subscriptionStats = null
        )

        val state = viewModel().uiState.value

        assertFalse(state.periodBudget.isHidden)
        assertEquals(NativeText.Resource(RString.error_not_found), state.periodBudget.error)
    }

    @Test
    fun `a monthly-cost failure still leaves the other two sources populated`() = runTest {
        useCase.result = DashboardHomeData(
            monthlyCost = Result.failure(WallosError.Server("boom")),
            periodBudget = Result.success(periodBudget),
            isPeriodBudgetRedundant = false,
            upcomingPayments = listOf(subscription(id = 1, nextPayment = LocalDate(2026, 8, 20))),
            overdueRenewals = emptyList(),
            user = user,
            monthlyBudget = null,
            subscriptionStats = null
        )

        val state = viewModel().uiState.value

        assertTrue(state.monthlyBudget.error.isNotEmpty())
        assertFalse(state.periodBudget.isHidden)
        assertEquals("€58.00", state.periodBudget.remainingAmount)
        assertEquals(1, state.upcomingPayments.size)
    }

    @Test
    fun `an over-budget period surfaces the over-budget amount`() = runTest {
        useCase.result = DashboardHomeData(
            monthlyCost = Result.success(MonthlyCost(title = "August 2026", amount = 42.0, currencySymbol = "€")),
            periodBudget = Result.success(
                periodBudget.copy(amountRemainingThisPeriod = 0.0, amountOverBudget = 12.5, isOverBudget = true)
            ),
            isPeriodBudgetRedundant = false,
            upcomingPayments = emptyList(),
            overdueRenewals = emptyList(),
            user = user,
            monthlyBudget = null,
            subscriptionStats = null
        )

        val state = viewModel().uiState.value

        assertTrue(state.periodBudget.isOverBudget)
        assertEquals("€12.50", state.periodBudget.amountOverBudget)
    }

    /** 10.2's gate: cost always shows; the budget/used/remaining rows only appear once a monthly budget is set. */
    @Test
    fun `monthly budget rows render alongside the cost when a monthly budget is set`() = runTest {
        useCase.result = DashboardHomeData(
            monthlyCost = Result.success(MonthlyCost(title = "August 2026", amount = 42.0, currencySymbol = "€")),
            periodBudget = Result.failure(WallosError.UnsupportedEndpoint),
            isPeriodBudgetRedundant = false,
            upcomingPayments = emptyList(),
            overdueRenewals = emptyList(),
            user = user,
            monthlyBudget = MonthlyBudget(
                amount = 100.0,
                used = 42.0,
                remaining = 58.0,
                overBudget = 0.0,
                isOverBudget = false
            ),
            subscriptionStats = null
        )

        val state = viewModel().uiState.value

        assertEquals("€42.00", state.monthlyBudget.costAmount)
        assertEquals("€100.00", state.monthlyBudget.budgetAmount)
        assertEquals("42.00%", state.monthlyBudget.usedPercent)
        assertEquals("€58.00", state.monthlyBudget.remainingAmount)
        assertFalse(state.monthlyBudget.isOverBudget)
    }

    /** No `MonthlyBudget` (the account has no budget set, 10.2's `null` gate) leaves the extra rows blank. */
    @Test
    fun `no monthly budget leaves the budget rows blank`() = runTest {
        useCase.result = DashboardHomeData(
            monthlyCost = Result.success(MonthlyCost(title = "August 2026", amount = 42.0, currencySymbol = "€")),
            periodBudget = Result.failure(WallosError.UnsupportedEndpoint),
            isPeriodBudgetRedundant = false,
            upcomingPayments = emptyList(),
            overdueRenewals = emptyList(),
            user = user,
            monthlyBudget = null,
            subscriptionStats = null
        )

        val state = viewModel().uiState.value

        assertEquals("€42.00", state.monthlyBudget.costAmount)
        assertTrue(state.monthlyBudget.budgetAmount.isEmpty())
        assertTrue(state.monthlyBudget.remainingAmount.isEmpty())
    }

    /** Overdue Renewals is a separate section from Upcoming Payments, not merged into it. */
    @Test
    fun `overdue renewals render separately from upcoming payments`() = runTest {
        useCase.result = DashboardHomeData(
            monthlyCost = Result.success(MonthlyCost(title = "August 2026", amount = 42.0, currencySymbol = "€")),
            periodBudget = Result.failure(WallosError.UnsupportedEndpoint),
            isPeriodBudgetRedundant = false,
            upcomingPayments = listOf(subscription(id = 1, nextPayment = LocalDate(2026, 8, 20))),
            overdueRenewals = listOf(subscription(id = 2, nextPayment = LocalDate(2026, 8, 1))),
            user = user,
            monthlyBudget = null,
            subscriptionStats = null
        )

        val state = viewModel().uiState.value

        assertEquals(listOf(1), state.upcomingPayments.map { it.id })
        assertEquals(listOf(2), state.overdueRenewals.map { it.id })
    }

    @Test
    fun `retry reloads from the use case`() = runTest {
        useCase.result = DashboardHomeData(
            monthlyCost = Result.failure(WallosError.Server("boom")),
            periodBudget = Result.failure(WallosError.UnsupportedEndpoint),
            isPeriodBudgetRedundant = false,
            upcomingPayments = emptyList(),
            overdueRenewals = emptyList(),
            user = user,
            monthlyBudget = null,
            subscriptionStats = null
        )
        val sut = viewModel()
        assertEquals(1, useCase.callCount)
        assertTrue(sut.uiState.value.monthlyBudget.error.isNotEmpty())

        useCase.result = DashboardHomeData(
            monthlyCost = Result.success(MonthlyCost(title = "August 2026", amount = 42.0, currencySymbol = "€")),
            periodBudget = Result.failure(WallosError.UnsupportedEndpoint),
            isPeriodBudgetRedundant = false,
            upcomingPayments = emptyList(),
            overdueRenewals = emptyList(),
            user = user,
            monthlyBudget = null,
            subscriptionStats = null
        )
        sut.uiState.value.onRetryClick()

        assertEquals(2, useCase.callCount)
        assertTrue(sut.uiState.value.monthlyBudget.error.isEmpty())
        assertEquals("€42.00", sut.uiState.value.monthlyBudget.costAmount)
    }

    private fun subscription(id: Int, nextPayment: LocalDate?) = Subscription(
        id = id,
        name = "Disney+",
        logo = "",
        price = 8.99,
        currencyId = 1,
        currencySymbol = "€",
        cycle = BillingCycle.MONTHS,
        frequency = 1,
        nextPayment = nextPayment,
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

    /**
     * Private to this file, as in the other UI ViewModel tests: `:testing` is on every module's
     * test classpath, so a fake declared there would drag `feature:dashboard:domain` into modules
     * that have no business seeing it.
     */
    private class FakeDashboardHomeUseCase : DashboardHomeUseCase {

        lateinit var result: DashboardHomeData
        var callCount = 0
            private set

        override suspend fun getDashboardHomeData(today: LocalDate): DashboardHomeData {
            callCount++
            return result
        }
    }
}

package com.grappim.wallosmobile.feature.subscriptions.ui.detail

import com.grappim.wallosmobile.core.api.BaseUrlProvider
import com.grappim.wallosmobile.core.domain.WallosError
import com.grappim.wallosmobile.feature.subscriptions.domain.model.BillingCycle
import com.grappim.wallosmobile.feature.subscriptions.domain.model.Subscription
import com.grappim.wallosmobile.feature.subscriptions.domain.repo.SubscriptionsRepository
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SubscriptionDetailViewModelTest {

    private val repository = FakeSubscriptionsRepository()
    private val baseUrlProvider = FakeBaseUrlProvider()
    private val mainDispatcherRule = MainDispatcherRule()

    @BeforeTest
    fun setup() {
        mainDispatcherRule.setup()
    }

    @AfterTest
    fun tearDown() {
        mainDispatcherRule.tearDown()
    }

    private fun viewModel(id: Int = SUBSCRIPTION_ID) = SubscriptionDetailViewModel(
        subscriptionId = id,
        subscriptionsRepository = repository,
        baseUrlProvider = baseUrlProvider,
        moneyFormatter = MoneyFormatter(),
        dateFormatter = DateFormatter()
    )

    @Test
    fun `the row loads itself and comes back rendered`() = runTest {
        repository.result = Result.success(subscription())

        val state = viewModel().uiState.value

        assertFalse(state.isLoading)
        assertTrue(state.error.isEmpty())

        val item = assertNotNull(state.subscription)
        assertEquals("Disney+", item.name)
        assertEquals("€8.99", item.price)
        assertEquals(BillingCycle.MONTHS, item.cycle)
        assertEquals(1, item.frequency)
        assertEquals("10 Mar 2026", item.nextPayment)
        assertEquals("5 Mar 2024", item.startDate)
        assertEquals("Entertainment", item.categoryName)
        assertEquals("Direct Debit", item.paymentMethodName)
        assertEquals("gregorz", item.payerName)
        assertEquals("Family plan", item.notes)
        assertEquals("https://disneyplus.com", item.url)
        assertTrue(item.isActive)
    }

    @Test
    fun `the route's id is the row that gets read`() = runTest {
        repository.result = Result.success(subscription())

        viewModel(id = 42)

        assertEquals(42, repository.requestedId)
    }

    @Test
    fun `a logo is a bare filename until the instance root is put in front of it`() = runTest {
        repository.result = Result.success(subscription(logo = "1732563509-Disney.png"))

        val item = assertNotNull(viewModel().uiState.value.subscription)

        assertEquals("$SERVER_URL/images/uploads/logos/1732563509-Disney.png", item.logoUrl)
    }

    @Test
    fun `a row with no logo asks for no image`() = runTest {
        repository.result = Result.success(subscription(logo = ""))

        assertEquals("", assertNotNull(viewModel().uiState.value.subscription).logoUrl)
    }

    @Test
    fun `an unknown cycle drops the cycle instead of guessing one`() = runTest {
        repository.result = Result.success(subscription(cycle = null))

        assertNull(assertNotNull(viewModel().uiState.value.subscription).cycle)
    }

    @Test
    fun `an unset start date leaves the row out`() = runTest {
        repository.result = Result.success(subscription(startDate = null))

        assertEquals("", assertNotNull(viewModel().uiState.value.subscription).startDate)
    }

    @Test
    fun `a row that belongs to someone else reads as not found`() = runTest {
        repository.result = Result.failure(WallosError.NotFound("Unauthorized or Not Found"))

        val state = viewModel().uiState.value

        assertEquals(NativeText.Resource(RString.error_not_found), state.error)
        assertNull(state.subscription)
        assertFalse(state.isLoading)
    }

    @Test
    fun `a failure clears the row it could not refresh`() = runTest {
        repository.result = Result.success(subscription())
        val sut = viewModel()
        assertNotNull(sut.uiState.value.subscription)

        repository.result = Result.failure(WallosError.Server("boom"))
        sut.uiState.value.onRetryClick()

        assertNull(sut.uiState.value.subscription)
    }

    @Test
    fun `retry clears the error and reloads`() = runTest {
        repository.result = Result.failure(WallosError.Server("boom"))
        val sut = viewModel()
        assertTrue(sut.uiState.value.error.isNotEmpty())
        assertEquals(1, repository.callCount)

        repository.result = Result.success(subscription())
        sut.uiState.value.onRetryClick()

        assertTrue(sut.uiState.value.error.isEmpty())
        assertNotNull(sut.uiState.value.subscription)
        assertEquals(2, repository.callCount)
    }

    private fun subscription(
        logo: String = "",
        cycle: BillingCycle? = BillingCycle.MONTHS,
        startDate: LocalDate? = LocalDate(2024, 3, 5)
    ) = Subscription(
        id = SUBSCRIPTION_ID,
        name = "Disney+",
        logo = logo,
        price = 8.99,
        currencyId = 1,
        currencySymbol = "€",
        cycle = cycle,
        frequency = 1,
        nextPayment = LocalDate(2026, 3, 10),
        startDate = startDate,
        isActive = true,
        notes = "Family plan",
        url = "https://disneyplus.com",
        categoryName = "Entertainment",
        paymentMethodName = "Direct Debit",
        payerName = "gregorz"
    )

    private companion object {
        const val SERVER_URL = "http://10.0.2.2:8282"
        const val SUBSCRIPTION_ID = 1
    }

    // Private to this file, as in 1.10 and 2.3: `:testing` is on every module's test classpath,
    // so a fake declared there drags `feature:subscriptions:domain` into modules that have no
    // business seeing it.
    private class FakeSubscriptionsRepository : SubscriptionsRepository {

        var result: Result<Subscription>? = null
        var requestedId: Int? = null
        var callCount = 0

        override suspend fun getSubscriptions(): Result<List<Subscription>> = Result.success(emptyList())

        override suspend fun getSubscription(id: Int): Result<Subscription> {
            callCount++
            requestedId = id
            return checkNotNull(result) { "the test did not set a result" }
        }
    }

    private class FakeBaseUrlProvider : BaseUrlProvider {

        /** With the trailing slash `BaseUrlProviderImpl` guarantees. */
        override fun getBaseUrl(): String = "$SERVER_URL/"
    }
}

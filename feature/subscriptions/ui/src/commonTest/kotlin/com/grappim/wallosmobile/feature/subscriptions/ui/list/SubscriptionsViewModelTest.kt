package com.grappim.wallosmobile.feature.subscriptions.ui.list

import com.grappim.wallosmobile.core.api.BaseUrlProvider
import com.grappim.wallosmobile.core.domain.WallosError
import com.grappim.wallosmobile.feature.subscriptions.domain.model.BillingCycle
import com.grappim.wallosmobile.feature.subscriptions.domain.model.Subscription
import com.grappim.wallosmobile.feature.subscriptions.domain.repo.SubscriptionsRepository
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.error_invalid_api_key
import com.grappim.wallosmobile.strings.generated.resources.error_unreachable
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

class SubscriptionsViewModelTest {

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

    // The formatters are pure classes with no seam worth faking (plan §6.1), so the assertions
    // below are on the strings the app really renders.
    private fun viewModel() = SubscriptionsViewModel(
        subscriptionsRepository = repository,
        baseUrlProvider = baseUrlProvider,
        moneyFormatter = MoneyFormatter(),
        dateFormatter = DateFormatter()
    )

    @Test
    fun `the list loads itself and comes back rendered`() = runTest {
        repository.result = Result.success(listOf(subscription()))

        val state = viewModel().uiState.value

        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
        assertTrue(state.error.isEmpty())

        val item = state.items.single()
        assertEquals(1, item.id)
        assertEquals("Disney+", item.name)
        assertEquals("€8.99", item.price)
        assertEquals("10 Mar 2026", item.nextPayment)
        assertEquals(BillingCycle.MONTHS, item.cycle)
        assertEquals(1, item.frequency)
        assertTrue(item.isActive)
    }

    @Test
    fun `a logo is a bare filename until the instance root is put in front of it`() = runTest {
        repository.result = Result.success(listOf(subscription(logo = "1732563509-Disney.png")))

        val item = viewModel().uiState.value.items.single()

        assertEquals("$SERVER_URL/images/uploads/logos/1732563509-Disney.png", item.logoUrl)
    }

    @Test
    fun `a row with no logo asks for no image`() = runTest {
        repository.result = Result.success(listOf(subscription(logo = "")))

        assertEquals("", viewModel().uiState.value.items.single().logoUrl)
    }

    @Test
    fun `a relative logo URL would be a broken request, so no server means no image`() = runTest {
        baseUrlProvider.url = ""
        repository.result = Result.success(listOf(subscription(logo = "logo.png")))

        assertEquals("", viewModel().uiState.value.items.single().logoUrl)
    }

    @Test
    fun `an unknown cycle drops the cycle instead of guessing one`() = runTest {
        repository.result = Result.success(listOf(subscription(cycle = null)))

        assertEquals(null, viewModel().uiState.value.items.single().cycle)
    }

    @Test
    fun `an unset next payment leaves the line out`() = runTest {
        repository.result = Result.success(listOf(subscription(nextPayment = null)))

        assertEquals("", viewModel().uiState.value.items.single().nextPayment)
    }

    @Test
    fun `an empty instance is an empty state, not a failure`() = runTest {
        repository.result = Result.success(emptyList())

        val state = viewModel().uiState.value

        assertTrue(state.isEmpty)
        assertTrue(state.error.isEmpty())
    }

    @Test
    fun `an empty list while loading is not the empty state`() = runTest {
        repository.result = Result.success(emptyList())

        assertFalse(SubscriptionsUiState(isLoading = true).isEmpty)
    }

    @Test
    fun `a rejected key reports the key, not the connection`() = runTest {
        repository.result = Result.failure(WallosError.Unauthenticated("Invalid API key"))

        val state = viewModel().uiState.value

        assertEquals(NativeText.Resource(RString.error_invalid_api_key), state.error)
        assertFalse(state.isLoading)
        assertFalse(state.isEmpty)
    }

    @Test
    fun `a transport failure reports the connection`() = runTest {
        repository.result = Result.failure(IllegalStateException("no route to host"))

        assertEquals(NativeText.Resource(RString.error_unreachable), viewModel().uiState.value.error)
    }

    @Test
    fun `a failure clears the list it could not refresh`() = runTest {
        repository.result = Result.success(listOf(subscription()))
        val sut = viewModel()
        assertEquals(1, sut.uiState.value.items.size)

        repository.result = Result.failure(WallosError.Server("boom"))
        sut.uiState.value.onRefresh()

        assertTrue(sut.uiState.value.items.isEmpty())
        assertFalse(sut.uiState.value.isRefreshing)
    }

    @Test
    fun `retry clears the error and reloads`() = runTest {
        repository.result = Result.failure(WallosError.Server("boom"))
        val sut = viewModel()
        assertTrue(sut.uiState.value.error.isNotEmpty())

        repository.result = Result.success(listOf(subscription()))
        sut.uiState.value.onRetryClick()

        assertTrue(sut.uiState.value.error.isEmpty())
        assertEquals(1, sut.uiState.value.items.size)
    }

    @Test
    fun `refresh reloads and the first load is not a refresh`() = runTest {
        repository.result = Result.success(emptyList())
        val sut = viewModel()
        assertEquals(1, repository.callCount)

        repository.result = Result.success(listOf(subscription()))
        sut.uiState.value.onRefresh()

        assertEquals(2, repository.callCount)
        assertEquals(1, sut.uiState.value.items.size)
    }

    private fun subscription(
        logo: String = "",
        cycle: BillingCycle? = BillingCycle.MONTHS,
        nextPayment: LocalDate? = LocalDate(2026, 3, 10)
    ) = Subscription(
        id = 1,
        name = "Disney+",
        logo = logo,
        price = 8.99,
        currencyId = 1,
        currencySymbol = "€",
        cycle = cycle,
        frequency = 1,
        nextPayment = nextPayment,
        startDate = null,
        isActive = true,
        notes = "",
        url = "",
        categoryName = "Entertainment",
        paymentMethodName = "Direct Debit",
        payerName = "gregorz"
    )

    private companion object {
        const val SERVER_URL = "http://10.0.2.2:8282"
    }

    // Private to this file, as in 1.10 and 2.3: `:testing` is on every module's test classpath,
    // so a fake declared there drags `feature:subscriptions:domain` into modules that have no
    // business seeing it.
    private class FakeSubscriptionsRepository : SubscriptionsRepository {

        var result: Result<List<Subscription>> = Result.success(emptyList())
        var callCount = 0

        override suspend fun getSubscriptions(): Result<List<Subscription>> {
            callCount++
            return result
        }

        override suspend fun getSubscription(id: Int): Result<Subscription> =
            result.mapCatching { subscriptions -> subscriptions.first { it.id == id } }
    }

    private class FakeBaseUrlProvider : BaseUrlProvider {

        /** The trailing slash `BaseUrlProviderImpl` guarantees. */
        var url: String = "$SERVER_URL/"

        override fun getBaseUrl(): String = url
    }
}

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
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
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
        dateFormatter = DateFormatter(),
        subscriptionSorter = SubscriptionSorter()
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

    /** 2.4 cleared the list here. With a cache behind the error there is something true to keep. */
    @Test
    fun `a failure leaves the list it could not refresh standing`() = runTest {
        repository.result = Result.success(listOf(subscription()))
        val sut = viewModel()
        assertEquals(1, sut.uiState.value.items.size)

        repository.result = Result.failure(WallosError.Server("boom"))
        sut.uiState.value.onRefresh()

        assertEquals(1, sut.uiState.value.items.size)
        assertTrue(sut.uiState.value.error.isNotEmpty())
        assertFalse(sut.uiState.value.isRefreshing)
    }

    /**
     * 3.5's split: the same error means two different screens depending on whether the cache had
     * anything to keep. This is the cold-start-offline case — rows, marked stale.
     */
    @Test
    fun `a failure over cached rows is stale, not a screen of its own`() = runTest {
        repository.seed(listOf(subscription()))
        repository.result = Result.failure(WallosError.Server("boom"))

        val state = viewModel().uiState.value

        assertTrue(state.isStale)
        assertFalse(state.isFailed)
        assertFalse(state.isEmpty)
    }

    @Test
    fun `a failure with nothing cached owns the screen`() = runTest {
        repository.result = Result.failure(WallosError.Server("boom"))

        val state = viewModel().uiState.value

        assertTrue(state.isFailed)
        assertFalse(state.isStale)
    }

    @Test
    fun `a refresh that works takes the stale marker back off`() = runTest {
        repository.seed(listOf(subscription()))
        repository.result = Result.failure(WallosError.Server("boom"))
        val sut = viewModel()
        assertTrue(sut.uiState.value.isStale)

        repository.result = Result.success(listOf(subscription()))
        sut.uiState.value.onRefresh()

        assertFalse(sut.uiState.value.isStale)
        assertEquals(1, sut.uiState.value.items.size)
    }

    /** Cold start with a full cache: the rows are the first thing on screen, spinner or no server. */
    @Test
    fun `cached rows show without waiting for the refresh, and survive its failure`() = runTest {
        repository.seed(listOf(subscription()))
        repository.result = Result.failure(WallosError.Server("boom"))

        val state = viewModel().uiState.value

        assertEquals(1, state.items.size)
        assertFalse(state.isLoading)
        assertTrue(state.error.isNotEmpty())
    }

    /** The spinner is the *empty* cache's, and nothing else's. */
    @Test
    fun `an empty cache keeps the spinner up until the refresh answers`() = runTest {
        val gate = CompletableDeferred<Unit>()
        repository.gate = gate
        repository.result = Result.success(listOf(subscription()))

        val sut = viewModel()
        assertTrue(sut.uiState.value.isLoading)

        gate.complete(Unit)
        assertFalse(sut.uiState.value.isLoading)
        assertEquals(1, sut.uiState.value.items.size)
    }

    @Test
    fun `a full cache never shows the spinner at all`() = runTest {
        repository.seed(listOf(subscription()))
        repository.gate = CompletableDeferred()

        assertFalse(viewModel().uiState.value.isLoading)
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

    // 3.6 — filter and sort, client-side over the cache.

    @Test
    fun `a filter narrows what is drawn and leaves the cache alone`() = runTest {
        repository.result = Result.success(listOf(disney, internet))
        val sut = viewModel()

        sut.uiState.value.filters.onFilterChange(
            SubscriptionFilter(categories = persistentSetOf("Internet"))
        )

        assertEquals(listOf(2), sut.uiState.value.items.map { it.id })
        assertTrue(sut.uiState.value.hasCachedRows)
        assertEquals(1, repository.callCount)
    }

    @Test
    fun `the three dimensions narrow together`() = runTest {
        repository.result = Result.success(listOf(disney, internet))
        val sut = viewModel()

        sut.uiState.value.filters.onFilterChange(
            SubscriptionFilter(
                categories = persistentSetOf("Entertainment"),
                payers = persistentSetOf("partner")
            )
        )

        assertTrue(sut.uiState.value.items.isEmpty())
    }

    @Test
    fun `an empty selection means every value, so unpicking the last chip widens back out`() = runTest {
        repository.result = Result.success(listOf(disney, internet))
        val sut = viewModel()
        sut.uiState.value.filters.onFilterChange(
            SubscriptionFilter(categories = persistentSetOf("Internet"))
        )

        sut.uiState.value.filters.onFilterChange(SubscriptionFilter())

        assertEquals(2, sut.uiState.value.items.size)
    }

    @Test
    fun `the status filter splits active from inactive`() = runTest {
        repository.result = Result.success(listOf(disney, internet.copy(isActive = false)))
        val sut = viewModel()

        sut.uiState.value.filters.onFilterChange(
            SubscriptionFilter(status = SubscriptionStatusFilter.INACTIVE)
        )

        assertEquals(listOf(2), sut.uiState.value.items.map { it.id })
    }

    /** Narrowing to one category must not take the others out of the sheet that narrowed them. */
    @Test
    fun `the sheet's options come from the whole cache, not from the narrowed view`() = runTest {
        repository.result = Result.success(listOf(disney, internet))
        val sut = viewModel()

        sut.uiState.value.filters.onFilterChange(
            SubscriptionFilter(categories = persistentSetOf("Internet"))
        )

        val filters = sut.uiState.value.filters
        assertEquals(listOf("Entertainment", "Internet"), filters.categories)
        assertEquals(listOf("gregorz", "partner"), filters.payers)
        assertEquals(listOf("Direct Debit", "PayPal"), filters.paymentMethods)
    }

    @Test
    fun `a blank name is dropped rather than offered as an unpickable chip`() = runTest {
        repository.result = Result.success(listOf(disney.copy(payerName = "")))

        assertTrue(viewModel().uiState.value.filters.payers.isEmpty())
    }

    /** An instance with nothing on it and a filter that excludes everything are different screens. */
    @Test
    fun `a filter that matches nothing is not an empty instance`() = runTest {
        repository.result = Result.success(listOf(disney))
        val sut = viewModel()

        sut.uiState.value.filters.onFilterChange(
            SubscriptionFilter(categories = persistentSetOf("Utilities"))
        )

        assertTrue(sut.uiState.value.isNoMatch)
        assertFalse(sut.uiState.value.isEmpty)
    }

    @Test
    fun `clearing the filters brings every row back and leaves the sort alone`() = runTest {
        repository.result = Result.success(listOf(disney, internet))
        val sut = viewModel()
        sut.uiState.value.filters.onSortChange(SubscriptionSort.NAME)
        sut.uiState.value.filters.onFilterChange(
            SubscriptionFilter(categories = persistentSetOf("Utilities"))
        )
        assertTrue(sut.uiState.value.isNoMatch)

        sut.uiState.value.filters.onClear()

        assertEquals(2, sut.uiState.value.items.size)
        assertEquals(SubscriptionSort.NAME, sut.uiState.value.filters.sort)
    }

    /**
     * The states 3.5 split now ask the *cache*, not the drawn list: a filter matching nothing while
     * offline would otherwise flip the stale banner into a full-screen error over rows that exist.
     */
    @Test
    fun `a filter that matches nothing keeps a stale refresh a banner`() = runTest {
        repository.seed(listOf(disney))
        repository.result = Result.failure(WallosError.Server("boom"))
        val sut = viewModel()

        sut.uiState.value.filters.onFilterChange(
            SubscriptionFilter(categories = persistentSetOf("Utilities"))
        )

        assertTrue(sut.uiState.value.isStale)
        assertFalse(sut.uiState.value.isFailed)
    }

    @Test
    fun `the default order is the server's own, and sorting again needs no refetch`() = runTest {
        repository.result = Result.success(listOf(disney, internet))
        val sut = viewModel()
        assertEquals(listOf(1, 2), sut.uiState.value.items.map { it.id })

        sut.uiState.value.filters.onSortChange(SubscriptionSort.NAME)

        assertEquals(listOf(2, 1), sut.uiState.value.items.map { it.id })
        assertEquals(1, repository.callCount)
    }

    @Test
    fun `the sheet opens and closes without touching the list`() = runTest {
        repository.result = Result.success(listOf(disney))
        val sut = viewModel()
        assertFalse(sut.uiState.value.filters.isVisible)

        sut.uiState.value.filters.onOpen()
        assertTrue(sut.uiState.value.filters.isVisible)

        sut.uiState.value.filters.onDismiss()
        assertFalse(sut.uiState.value.filters.isVisible)
        assertEquals(1, sut.uiState.value.items.size)
    }

    private val disney = subscription()

    /** Later than [disney] and alphabetically before it, so the two orders are told apart. */
    private val internet = subscription(nextPayment = LocalDate(2026, 4, 12)).copy(
        id = 2,
        name = "1&1 Telekom",
        categoryName = "Internet",
        paymentMethodName = "PayPal",
        payerName = "partner"
    )

    /** Anything 3.6 varies is reached with `.copy()` rather than by growing this signature. */
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

    /**
     * Private to this file, as in 1.10 and 2.3: `:testing` is on every module's test classpath, so
     * a fake declared there drags `feature:subscriptions:domain` into modules that have no
     * business seeing it.
     *
     * It behaves the way the real one does since 3.4 — [result] is what the *refresh* returns, and
     * only a successful one reaches [cached], which is the only thing the ViewModel ever reads.
     * [seed] is the cache as a previous session left it.
     */
    private class FakeSubscriptionsRepository : SubscriptionsRepository {

        private val cached = MutableStateFlow<List<Subscription>>(emptyList())

        var result: Result<List<Subscription>> = Result.success(emptyList())
        var callCount = 0

        /** Set to hold a refresh open, for the states that only exist while one is in flight. */
        var gate: CompletableDeferred<Unit>? = null

        fun seed(subscriptions: List<Subscription>) {
            cached.value = subscriptions
        }

        override fun observeSubscriptions(): Flow<List<Subscription>> = cached

        override suspend fun refreshSubscriptions(): Result<Unit> {
            callCount++
            gate?.await()
            return result.map { subscriptions -> cached.value = subscriptions }
        }

        override fun observeSubscription(id: Int): Flow<Subscription?> = cached.map { subscriptions ->
            subscriptions.firstOrNull { it.id == id }
        }

        override suspend fun refreshSubscription(id: Int): Result<Unit> = refreshSubscriptions()
    }

    private class FakeBaseUrlProvider : BaseUrlProvider {

        /** The trailing slash `BaseUrlProviderImpl` guarantees. */
        var url: String = "$SERVER_URL/"

        override fun getBaseUrl(): String = url
    }
}

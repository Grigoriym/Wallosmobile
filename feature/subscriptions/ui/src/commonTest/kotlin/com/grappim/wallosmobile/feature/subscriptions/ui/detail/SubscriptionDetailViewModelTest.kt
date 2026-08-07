package com.grappim.wallosmobile.feature.subscriptions.ui.detail

import com.grappim.wallosmobile.core.api.BaseUrlProvider
import com.grappim.wallosmobile.core.domain.WallosError
import com.grappim.wallosmobile.feature.subscriptions.domain.model.AddSubscriptionParams
import com.grappim.wallosmobile.feature.subscriptions.domain.model.BillingCycle
import com.grappim.wallosmobile.feature.subscriptions.domain.model.Currency
import com.grappim.wallosmobile.feature.subscriptions.domain.model.EditSubscriptionParams
import com.grappim.wallosmobile.feature.subscriptions.domain.model.PriceConversion
import com.grappim.wallosmobile.feature.subscriptions.domain.model.Subscription
import com.grappim.wallosmobile.feature.subscriptions.domain.repo.SubscriptionsRepository
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.error_not_found
import com.grappim.wallosmobile.testing.MainDispatcherRule
import com.grappim.wallosmobile.utils.formatter.datetime.DateFormatter
import com.grappim.wallosmobile.utils.formatter.decimal.MoneyFormatter
import com.grappim.wallosmobile.utils.ui.NativeText
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

    /** 2.5 cleared the row here; the cached one is real, so a failed refresh leaves it alone. */
    @Test
    fun `a failure leaves the row it could not refresh standing`() = runTest {
        repository.result = Result.success(subscription())
        val sut = viewModel()
        assertNotNull(sut.uiState.value.subscription)

        repository.result = Result.failure(WallosError.Server("boom"))
        sut.uiState.value.onRetryClick()

        assertNotNull(sut.uiState.value.subscription)
        assertTrue(sut.uiState.value.error.isNotEmpty())
    }

    /** Opened from a cached list: the row is already there, so nothing waits on the network. */
    @Test
    fun `the cached row is on screen before the refresh answers`() = runTest {
        repository.seed(subscription())
        repository.result = Result.failure(WallosError.Server("boom"))

        val state = viewModel().uiState.value

        assertEquals("Disney+", assertNotNull(state.subscription).name)
        assertFalse(state.isLoading)
    }

    /** 3.5, as on the list: a cached row behind the error turns it into a banner over that row. */
    @Test
    fun `a failure over a cached row is stale, not a screen of its own`() = runTest {
        repository.seed(subscription())
        repository.result = Result.failure(WallosError.Server("boom"))

        val state = viewModel().uiState.value

        assertTrue(state.isStale)
        assertFalse(state.isFailed)
    }

    @Test
    fun `a failure with no cached row owns the screen`() = runTest {
        repository.result = Result.failure(WallosError.Server("boom"))

        val state = viewModel().uiState.value

        assertTrue(state.isFailed)
        assertFalse(state.isStale)
    }

    @Test
    fun `a refresh that works takes the stale marker back off`() = runTest {
        repository.seed(subscription())
        repository.result = Result.failure(WallosError.Server("boom"))
        val sut = viewModel()
        assertTrue(sut.uiState.value.isStale)

        repository.result = Result.success(subscription())
        sut.uiState.value.onRetryClick()

        assertFalse(sut.uiState.value.isStale)
        assertNotNull(sut.uiState.value.subscription)
    }

    /** A list refresh that dropped the row means the server hasn't got it — so neither has this. */
    @Test
    fun `a row the cache loses disappears from the screen`() = runTest {
        repository.seed(subscription())
        repository.result = Result.success(subscription())
        val sut = viewModel()
        assertNotNull(sut.uiState.value.subscription)

        repository.seed(subscription(id = 99))

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

    /**
     * 5.4: the amount is in the instance's main currency and the row still carries the id of the
     * one it was converted from (3.11), which is the only thing left to name it with.
     */
    @Test
    fun `a converted price names the currency it was converted from`() = runTest {
        repository.conversion.value = CONVERTING
        repository.currencies.value = CURRENCIES
        repository.result = Result.success(subscription().copy(currencyId = 2, currencySymbol = "€"))

        val item = assertNotNull(viewModel().uiState.value.subscription)

        assertEquals("USD", item.convertedFrom)
    }

    /** Wallos leaves a row that is already in the main currency alone, so there is nothing to say. */
    @Test
    fun `a row already in the main currency says nothing`() = runTest {
        repository.conversion.value = CONVERTING
        repository.currencies.value = CURRENCIES
        repository.result = Result.success(subscription().copy(currencyId = 1))

        val item = assertNotNull(viewModel().uiState.value.subscription)

        assertTrue(item.convertedFrom.isEmpty())
    }

    /**
     * The instance was asked to convert and cannot (3.11's silent failure): the price is the row's
     * own, so claiming a conversion would be the one thing worse than the missing banner.
     */
    @Test
    fun `a conversion that could not run leaves the price unlabelled`() = runTest {
        repository.conversion.value = CONVERTING.copy(hasRates = false)
        repository.currencies.value = CURRENCIES
        repository.result = Result.success(subscription().copy(currencyId = 2, currencySymbol = "$"))

        val item = assertNotNull(viewModel().uiState.value.subscription)

        assertTrue(item.convertedFrom.isEmpty())
    }

    /** Same answer as the symbol the row would have had: no label beats the wrong currency. */
    @Test
    fun `a source currency the cache does not hold is not named`() = runTest {
        repository.conversion.value = CONVERTING
        repository.currencies.value = listOf(EURO)
        repository.result = Result.success(subscription().copy(currencyId = 2, currencySymbol = "€"))

        val item = assertNotNull(viewModel().uiState.value.subscription)

        assertTrue(item.convertedFrom.isEmpty())
    }

    /** `code` is defaulted on the wire, so an instance that omits it still gets a real label. */
    @Test
    fun `a currency with no code is named by its name`() = runTest {
        repository.conversion.value = CONVERTING
        repository.currencies.value = listOf(EURO, DOLLAR.copy(code = ""))
        repository.result = Result.success(subscription().copy(currencyId = 2, currencySymbol = "€"))

        val item = assertNotNull(viewModel().uiState.value.subscription)

        assertEquals("US Dollar", item.convertedFrom)
    }

    // --- 5.6: a recovered server reloads its logos ---------------------------------------------

    /**
     * The construction-time load is never a recovery — nothing failed before it — so a logo that
     * already loaded fine must not be evicted from Coil's memory cache on every screen open.
     */
    @Test
    fun `a successful refresh with nothing to recover from leaves the logo refresh token alone`() = runTest {
        repository.result = Result.success(subscription())
        val sut = viewModel()
        assertEquals(0, assertNotNull(sut.uiState.value.subscription).logoRefreshToken)

        sut.uiState.value.onRetryClick()

        assertEquals(0, assertNotNull(sut.uiState.value.subscription).logoRefreshToken)
    }

    /** A failed refresh changed nothing about the row or its logo, so the token must not move. */
    @Test
    fun `a failed refresh leaves the logo refresh token alone`() = runTest {
        repository.result = Result.success(subscription())
        val sut = viewModel()
        val tokenBefore = assertNotNull(sut.uiState.value.subscription).logoRefreshToken

        repository.result = Result.failure(WallosError.Server("boom"))
        sut.uiState.value.onRetryClick()

        assertEquals(tokenBefore, assertNotNull(sut.uiState.value.subscription).logoRefreshToken)
    }

    /** The one case that does need a real retry: Coil never retries a request already `Error`. */
    @Test
    fun `a successful refresh that recovers from a failure bumps the logo refresh token`() = runTest {
        repository.result = Result.success(subscription())
        val sut = viewModel()
        val tokenBefore = assertNotNull(sut.uiState.value.subscription).logoRefreshToken

        repository.result = Result.failure(WallosError.Server("boom"))
        sut.uiState.value.onRetryClick()
        repository.result = Result.success(subscription())
        sut.uiState.value.onRetryClick()

        assertEquals(tokenBefore + 1, assertNotNull(sut.uiState.value.subscription).logoRefreshToken)
    }

    // --- 7.7: delete, behind a confirmation dialog -----------------------------------------------

    @Test
    fun `the delete action opens the confirmation dialog rather than deleting straight away`() = runTest {
        repository.result = Result.success(subscription())
        val sut = viewModel()

        sut.uiState.value.onDeleteClick()

        assertTrue(sut.uiState.value.isDeleteDialogOpen)
        assertNull(repository.deletedId)
    }

    @Test
    fun `dismissing the dialog deletes nothing`() = runTest {
        repository.result = Result.success(subscription())
        val sut = viewModel()
        sut.uiState.value.onDeleteClick()

        sut.uiState.value.onDeleteDialogDismiss()

        assertFalse(sut.uiState.value.isDeleteDialogOpen)
        assertNull(repository.deletedId)
    }

    @Test
    fun `confirming deletes the route's own id and closes the dialog`() = runTest {
        repository.result = Result.success(subscription())
        val sut = viewModel(id = 7)
        sut.uiState.value.onDeleteClick()

        sut.uiState.value.onDeleteConfirm()

        assertEquals(7, repository.deletedId)
        assertFalse(sut.uiState.value.isDeleteDialogOpen)
        assertFalse(sut.uiState.value.isDeleting)
    }

    /** A rejected delete leaves the dialog open with the reason on it, not a screen of its own. */
    @Test
    fun `a delete that fails surfaces the error and leaves the dialog open`() = runTest {
        repository.result = Result.success(subscription())
        repository.deleteResult = Result.failure(WallosError.Server("boom"))
        val sut = viewModel()
        sut.uiState.value.onDeleteClick()

        sut.uiState.value.onDeleteConfirm()

        assertTrue(sut.uiState.value.isDeleteDialogOpen)
        assertTrue(sut.uiState.value.deleteError.isNotEmpty())
        assertFalse(sut.uiState.value.isDeleting)
    }

    private fun subscription(
        id: Int = SUBSCRIPTION_ID,
        logo: String = "",
        cycle: BillingCycle? = BillingCycle.MONTHS,
        startDate: LocalDate? = LocalDate(2024, 3, 5)
    ) = Subscription(
        id = id,
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
        payerName = "gregorz",
        categoryId = 3,
        paymentMethodId = 2,
        payerUserId = 1,
        autoRenew = true,
        notify = false,
        notifyDaysBefore = null
    )

    private companion object {
        const val SERVER_URL = "http://10.0.2.2:8282"
        const val SUBSCRIPTION_ID = 1

        val EURO = Currency(id = 1, name = "Euro", symbol = "€", code = "EUR")
        val DOLLAR = Currency(id = 2, name = "US Dollar", symbol = "$", code = "USD")
        val CURRENCIES = listOf(EURO, DOLLAR)

        /** Asked for, able to, and converting into currency 1 — the state that produces a label. */
        val CONVERTING = PriceConversion(isEnabled = true, mainCurrencyId = 1, hasRates = true)
    }

    /**
     * Private to this file, as in 1.10 and 2.3: `:testing` is on every module's test classpath, so
     * a fake declared there drags `feature:subscriptions:domain` into modules that have no
     * business seeing it.
     *
     * As on the list (3.4), [result] is the *refresh*'s answer and only a successful one reaches
     * the cache the ViewModel reads. [seed] is the row a list refresh cached earlier.
     */
    private class FakeSubscriptionsRepository : SubscriptionsRepository {

        private val cached = MutableStateFlow<List<Subscription>>(emptyList())

        var result: Result<Subscription>? = null
        var requestedId: Int? = null
        var callCount = 0
        var deleteResult: Result<Unit> = Result.success(Unit)
        var deletedId: Int? = null

        /** What the same refresh left behind about the cached prices, and what names them (3.11). */
        val conversion = MutableStateFlow(PriceConversion())
        val currencies = MutableStateFlow<List<Currency>>(emptyList())

        fun seed(subscription: Subscription) {
            cached.value = listOf(subscription)
        }

        override fun observeSubscriptions(): Flow<List<Subscription>> = cached

        override fun observePriceConversion(): Flow<PriceConversion> = conversion

        override fun observeCurrencies(): Flow<List<Currency>> = currencies

        override suspend fun refreshSubscriptions(): Result<Unit> = Result.success(Unit)

        override fun observeSubscription(id: Int): Flow<Subscription?> = cached.map { subscriptions ->
            subscriptions.firstOrNull { it.id == id }
        }

        override suspend fun refreshSubscription(id: Int): Result<Unit> {
            callCount++
            requestedId = id
            return checkNotNull(result) { "the test did not set a result" }
                .map { subscription -> cached.value = listOf(subscription) }
        }

        override suspend fun addSubscription(params: AddSubscriptionParams): Result<Int> =
            error("not used by this test")

        override suspend fun editSubscription(id: Int, params: EditSubscriptionParams): Result<Unit> =
            error("not used by this test")

        override suspend fun deleteSubscription(id: Int): Result<Unit> {
            deletedId = id
            return deleteResult
        }
    }

    private class FakeBaseUrlProvider : BaseUrlProvider {

        /** With the trailing slash `BaseUrlProviderImpl` guarantees. */
        override fun getBaseUrl(): String = "$SERVER_URL/"
    }
}

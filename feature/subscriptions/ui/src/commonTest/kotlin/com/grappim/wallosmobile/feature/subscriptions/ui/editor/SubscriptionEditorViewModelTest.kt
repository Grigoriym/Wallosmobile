package com.grappim.wallosmobile.feature.subscriptions.ui.editor

import androidx.lifecycle.SavedStateHandle
import com.grappim.wallosmobile.core.domain.WallosError
import com.grappim.wallosmobile.feature.categories.domain.model.Category
import com.grappim.wallosmobile.feature.categories.domain.repo.CategoriesRepository
import com.grappim.wallosmobile.feature.household.domain.model.HouseholdMember
import com.grappim.wallosmobile.feature.household.domain.repo.HouseholdRepository
import com.grappim.wallosmobile.feature.paymentmethods.domain.model.PaymentMethod
import com.grappim.wallosmobile.feature.paymentmethods.domain.repo.PaymentMethodsRepository
import com.grappim.wallosmobile.feature.subscriptions.domain.model.AddSubscriptionParams
import com.grappim.wallosmobile.feature.subscriptions.domain.model.BillingCycle
import com.grappim.wallosmobile.feature.subscriptions.domain.model.Currency
import com.grappim.wallosmobile.feature.subscriptions.domain.model.EditSubscriptionParams
import com.grappim.wallosmobile.feature.subscriptions.domain.model.LogoFile
import com.grappim.wallosmobile.feature.subscriptions.domain.model.PriceConversion
import com.grappim.wallosmobile.feature.subscriptions.domain.model.Subscription
import com.grappim.wallosmobile.feature.subscriptions.domain.model.WritableBillingCycle
import com.grappim.wallosmobile.feature.subscriptions.domain.repo.SubscriptionsRepository
import com.grappim.wallosmobile.testing.MainDispatcherRule
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SubscriptionEditorViewModelTest {

    private val subscriptionsRepository = FakeSubscriptionsRepository()
    private val categoriesRepository = FakeCategoriesRepository()
    private val householdRepository = FakeHouseholdRepository()
    private val paymentMethodsRepository = FakePaymentMethodsRepository()
    private val mainDispatcherRule = MainDispatcherRule()

    @BeforeTest
    fun setup() {
        mainDispatcherRule.setup()
    }

    @AfterTest
    fun tearDown() {
        mainDispatcherRule.tearDown()
    }

    private fun viewModel(savedStateHandle: SavedStateHandle = SavedStateHandle(), subscriptionId: Int? = null) =
        SubscriptionEditorViewModel(
            subscriptionId = subscriptionId,
            subscriptionsRepository = subscriptionsRepository,
            categoriesRepository = categoriesRepository,
            householdRepository = householdRepository,
            paymentMethodsRepository = paymentMethodsRepository,
            savedStateHandle = savedStateHandle
        )

    @Test
    fun `reference data arrives in the pickers`() = runTest {
        subscriptionsRepository.currencies.value = listOf(EURO)
        categoriesRepository.categories =
            Result.success(listOf(Category(id = 1, name = "Entertainment", inUse = false)))
        householdRepository.members =
            Result.success(listOf(HouseholdMember(id = 1, name = "gregorz", email = "", inUse = false)))
        paymentMethodsRepository.methods = Result.success(
            listOf(PaymentMethod(id = 1, name = "Direct Debit", icon = "", enabled = true, inUse = false))
        )

        val state = viewModel().uiState.value

        assertEquals(PickerOption(1, "EUR (€)"), state.currency.options.single())
        assertEquals(PickerOption(1, "Entertainment"), state.category.options.single())
        assertEquals(PickerOption(1, "gregorz"), state.payer.options.single())
        assertEquals(PickerOption(1, "Direct Debit"), state.paymentMethod.options.single())
        assertFalse(state.category.isLoading)
        assertFalse(state.payer.isLoading)
        assertFalse(state.paymentMethod.isLoading)
    }

    /** A repository that never answers must not sink the whole form — the other three still load. */
    @Test
    fun `a reference-data load that fails leaves its picker empty rather than the screen`() = runTest {
        categoriesRepository.categories = Result.failure(WallosError.Server("boom"))
        subscriptionsRepository.currencies.value = listOf(EURO)

        val state = viewModel().uiState.value

        assertTrue(state.category.options.isEmpty())
        assertFalse(state.category.isLoading)
        assertEquals(PickerOption(1, "EUR (€)"), state.currency.options.single())
    }

    /**
     * 4.4's own finding: the three no-cache pickers can sit empty for real seconds waiting on the
     * network (measured with `dumpsys gfxinfo`/Perfetto against the local instance). A picker with
     * no options and no loading state is indistinguishable from a broken one — this is what tells
     * the two apart while `getCategories()` is still in flight.
     */
    @Test
    fun `a picker starts loading and clears once its data arrives`() = runTest {
        categoriesRepository.categories =
            Result.success(listOf(Category(id = 1, name = "Entertainment", inUse = false)))
        categoriesRepository.holdUntilReleased()

        val sut = viewModel()

        assertTrue(sut.uiState.value.category.isLoading)

        categoriesRepository.release()

        assertFalse(sut.uiState.value.category.isLoading)
        assertEquals(PickerOption(1, "Entertainment"), sut.uiState.value.category.options.single())
    }

    @Test
    fun `an incomplete form is rejected without reaching the repository`() = runTest {
        val sut = viewModel()

        sut.uiState.value.onSaveClick()

        assertTrue(sut.uiState.value.error.isNotEmpty())
        assertNull(subscriptionsRepository.addedParams)
    }

    @Test
    fun `a complete form adds the subscription`() = runTest {
        subscriptionsRepository.addResult = Result.success(42)
        val sut = viewModel()
        fillMinimalValidForm(sut)

        sut.uiState.value.onSaveClick()

        val params = assertNotNull(subscriptionsRepository.addedParams)
        assertEquals("Disney+", params.name)
        assertEquals(8.99, params.price)
        assertEquals(1, params.currencyId)
        assertEquals(WritableBillingCycle.MONTHS, params.cycle)
        assertEquals(LocalDate(2026, 3, 10), params.nextPayment)
        assertFalse(sut.uiState.value.isSaving)
        assertTrue(sut.uiState.value.error.isEmpty())
    }

    @Test
    fun `a blank logo URL is omitted rather than sent as an empty string`() = runTest {
        subscriptionsRepository.addResult = Result.success(1)
        val sut = viewModel()
        fillMinimalValidForm(sut)

        sut.uiState.value.onSaveClick()

        assertNull(assertNotNull(subscriptionsRepository.addedParams).logoUrl)
    }

    @Test
    fun `a typed logo URL reaches the add params`() = runTest {
        subscriptionsRepository.addResult = Result.success(1)
        val sut = viewModel()
        fillMinimalValidForm(sut)
        sut.uiState.value.onLogoUrlChange(" https://example.com/logo.png ")

        sut.uiState.value.onSaveClick()

        assertEquals("https://example.com/logo.png", assertNotNull(subscriptionsRepository.addedParams).logoUrl)
    }

    // --- 7.9: logo file upload -----------------------------------------------------------------

    @Test
    fun `a picked logo file is held in state until save`() = runTest {
        val sut = viewModel()

        sut.uiState.value.onLogoFilePick(
            LogoFile(bytes = byteArrayOf(1), fileName = "logo.png", mimeType = "image/png")
        )

        assertEquals("logo.png", sut.uiState.value.logoFile?.fileName)
    }

    @Test
    fun `a picked logo file reaches the add params`() = runTest {
        subscriptionsRepository.addResult = Result.success(1)
        val sut = viewModel()
        fillMinimalValidForm(sut)
        sut.uiState.value.onLogoFilePick(
            LogoFile(bytes = byteArrayOf(1), fileName = "logo.png", mimeType = "image/png")
        )

        sut.uiState.value.onSaveClick()

        assertEquals("logo.png", assertNotNull(subscriptionsRepository.addedParams).logoFile?.fileName)
    }

    @Test
    fun `no picked file leaves the add params without one`() = runTest {
        subscriptionsRepository.addResult = Result.success(1)
        val sut = viewModel()
        fillMinimalValidForm(sut)

        sut.uiState.value.onSaveClick()

        assertNull(assertNotNull(subscriptionsRepository.addedParams).logoFile)
    }

    /** Notify's day count is only meaningful once notify is on — off, it must not reach the server. */
    @Test
    fun `notify days before is only sent while notify is on`() = runTest {
        subscriptionsRepository.addResult = Result.success(1)
        val sut = viewModel()
        fillMinimalValidForm(sut)
        sut.uiState.value.onNotifyDaysBeforeChange("3")

        sut.uiState.value.onSaveClick()

        assertNull(assertNotNull(subscriptionsRepository.addedParams).notifyDaysBefore)
    }

    @Test
    fun `a save that fails surfaces the error and clears the saving flag`() = runTest {
        subscriptionsRepository.addResult = Result.failure(WallosError.Server("boom"))
        val sut = viewModel()
        fillMinimalValidForm(sut)

        sut.uiState.value.onSaveClick()

        assertTrue(sut.uiState.value.error.isNotEmpty())
        assertFalse(sut.uiState.value.isSaving)
    }

    @Test
    fun `the form survives process death through the saved state handle`() = runTest {
        val handle = SavedStateHandle()
        val first = viewModel(handle)
        first.uiState.value.onNameChange("Disney+")
        first.uiState.value.onPriceChange("8.99")

        val second = viewModel(handle)

        assertEquals("Disney+", second.uiState.value.name)
        assertEquals("8.99", second.uiState.value.price)
    }

    /** Pre-v1 stored state is disposable (5.2's precedent) — discarding it beats crashing on it. */
    @Test
    fun `unreadable stored state is discarded rather than crashing`() = runTest {
        val handle = SavedStateHandle()
        handle[KEY_FORM] = "not json"

        val sut = viewModel(handle)

        assertEquals("", sut.uiState.value.name)
    }

    // --- 7.7: editing an existing subscription -----------------------------------------------

    @Test
    fun `an id pre-fills the form from the cached row, with no extra round trip`() = runTest {
        subscriptionsRepository.seed(subscription())

        val state = viewModel(subscriptionId = 4).uiState.value

        assertEquals("Disney+", state.name)
        assertEquals("8.99", state.price)
        assertEquals(1, state.currency.selectedId)
        assertEquals(WritableBillingCycle.MONTHS, state.cycle)
        assertEquals("1", state.frequency)
        assertEquals(LocalDate(2026, 3, 10), state.nextPayment)
        assertEquals(3, state.category.selectedId)
        assertEquals(1, state.payer.selectedId)
        assertEquals(2, state.paymentMethod.selectedId)
        assertEquals("Family plan", state.notes)
        assertEquals("https://disneyplus.com", state.url)
        assertTrue(state.notify)
        assertEquals("3", state.notifyDaysBefore)
        assertTrue(state.autoRenew)
        assertFalse(state.inactive)
    }

    /** A cycle the editor cannot write back (One-time, or one this build doesn't know) falls to Months. */
    @Test
    fun `an unwritable cycle falls back to months rather than leaving the picker unset`() = runTest {
        subscriptionsRepository.seed(subscription(cycle = BillingCycle.ONE_TIME))

        val state = viewModel(subscriptionId = 4).uiState.value

        assertEquals(WritableBillingCycle.MONTHS, state.cycle)
    }

    /** Opened before the row is cached (a cold detail screen, say): the form is just blank. */
    @Test
    fun `an id the cache does not hold yet leaves the form blank`() = runTest {
        val state = viewModel(subscriptionId = 4).uiState.value

        assertEquals("", state.name)
    }

    @Test
    fun `saving an edit reaches editSubscription with the route's id, not add`() = runTest {
        subscriptionsRepository.seed(subscription())
        val sut = viewModel(subscriptionId = 4)

        sut.uiState.value.onSaveClick()

        assertEquals(4, subscriptionsRepository.editedId)
        assertNotNull(subscriptionsRepository.editedParams)
        assertNull(subscriptionsRepository.addedParams)
    }

    @Test
    fun `an edit that fails surfaces the error and clears the saving flag`() = runTest {
        subscriptionsRepository.seed(subscription())
        subscriptionsRepository.editResult = Result.failure(WallosError.Server("boom"))
        val sut = viewModel(subscriptionId = 4)

        sut.uiState.value.onSaveClick()

        assertTrue(sut.uiState.value.error.isNotEmpty())
        assertFalse(sut.uiState.value.isSaving)
    }

    /** A form already mid-edit after process death must not be clobbered by a second cache read. */
    @Test
    fun `a restored form is not overwritten by the cached row on a second construction`() = runTest {
        subscriptionsRepository.seed(subscription())
        val handle = SavedStateHandle()
        val first = viewModel(handle, subscriptionId = 4)
        assertEquals("Disney+", first.uiState.value.name)
        first.uiState.value.onNameChange("Disney+ (renamed)")

        subscriptionsRepository.seed(subscription().copy(name = "A different name by now"))
        val second = viewModel(handle, subscriptionId = 4)

        assertEquals("Disney+ (renamed)", second.uiState.value.name)
    }

    private fun subscription(cycle: BillingCycle? = BillingCycle.MONTHS) = Subscription(
        id = 4,
        name = "Disney+",
        logo = "",
        price = 8.99,
        currencyId = 1,
        currencySymbol = "€",
        cycle = cycle,
        frequency = 1,
        nextPayment = LocalDate(2026, 3, 10),
        startDate = null,
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
        notify = true,
        notifyDaysBefore = 3
    )

    private fun fillMinimalValidForm(sut: SubscriptionEditorViewModel) {
        val state = sut.uiState.value
        state.onNameChange("Disney+")
        state.onPriceChange("8.99")
        state.currency.onSelect(1)
        state.onNextPaymentChange(LocalDate(2026, 3, 10))
    }

    private companion object {
        const val KEY_FORM = "subscription_editor_form"
        val EURO = Currency(id = 1, name = "Euro", symbol = "€", code = "EUR")
    }

    /**
     * Private to this file, as in 1.10 and 2.3: `:testing` is on every module's test classpath, so
     * a fake declared there drags every feature domain module into modules that have no business
     * seeing it.
     */
    private class FakeSubscriptionsRepository : SubscriptionsRepository {

        private val cached = MutableStateFlow<List<Subscription>>(emptyList())

        val currencies = MutableStateFlow<List<Currency>>(emptyList())
        var addResult: Result<Int> = Result.success(1)
        var addedParams: AddSubscriptionParams? = null
        var editResult: Result<Unit> = Result.success(Unit)
        var editedId: Int? = null
        var editedParams: EditSubscriptionParams? = null

        fun seed(subscription: Subscription) {
            cached.value = listOf(subscription)
        }

        override fun observeSubscriptions(): Flow<List<Subscription>> = error("not used by this test")

        override fun observePriceConversion(): Flow<PriceConversion> = error("not used by this test")

        override fun observeCurrencies(): Flow<List<Currency>> = currencies

        override suspend fun refreshSubscriptions(): Result<Unit> = error("not used by this test")

        override fun observeSubscription(id: Int): Flow<Subscription?> = cached.map { subscriptions ->
            subscriptions.firstOrNull { it.id == id }
        }

        override suspend fun refreshSubscription(id: Int): Result<Unit> = error("not used by this test")

        override suspend fun addSubscription(params: AddSubscriptionParams): Result<Int> {
            addedParams = params
            return addResult
        }

        override suspend fun editSubscription(id: Int, params: EditSubscriptionParams): Result<Unit> {
            editedId = id
            editedParams = params
            return editResult
        }

        override suspend fun deleteSubscription(id: Int): Result<Unit> = error("not used by this test")
    }

    private class FakeCategoriesRepository : CategoriesRepository {

        var categories: Result<List<Category>> = Result.success(emptyList())
        private var gate: CompletableDeferred<Unit>? = null

        /** Leaves [getCategories] suspended until [release] — for asserting the in-flight loading state. */
        fun holdUntilReleased() {
            gate = CompletableDeferred()
        }

        fun release() {
            gate?.complete(Unit)
        }

        override suspend fun getCategories(): Result<List<Category>> {
            gate?.await()
            return categories
        }

        override suspend fun addCategory(name: String): Result<Int> = error("not used by this test")

        override suspend fun editCategory(id: Int, name: String): Result<Unit> = error("not used by this test")

        override suspend fun deleteCategory(id: Int): Result<Unit> = error("not used by this test")
    }

    private class FakeHouseholdRepository : HouseholdRepository {

        var members: Result<List<HouseholdMember>> = Result.success(emptyList())

        override suspend fun getMembers(): Result<List<HouseholdMember>> = members

        override suspend fun addMember(name: String, email: String): Result<Int> = error("not used by this test")

        override suspend fun editMember(id: Int, name: String, email: String): Result<Unit> =
            error("not used by this test")

        override suspend fun deleteMember(id: Int): Result<Unit> = error("not used by this test")
    }

    private class FakePaymentMethodsRepository : PaymentMethodsRepository {

        var methods: Result<List<PaymentMethod>> = Result.success(emptyList())

        override suspend fun getPaymentMethods(): Result<List<PaymentMethod>> = methods

        override suspend fun addPaymentMethod(name: String, enabled: Boolean, iconUrl: String?): Result<Int> =
            error("not used by this test")

        override suspend fun editPaymentMethod(
            id: Int,
            name: String,
            enabled: Boolean,
            iconUrl: String?
        ): Result<Unit> = error("not used by this test")

        override suspend fun deletePaymentMethod(id: Int): Result<Unit> = error("not used by this test")
    }
}

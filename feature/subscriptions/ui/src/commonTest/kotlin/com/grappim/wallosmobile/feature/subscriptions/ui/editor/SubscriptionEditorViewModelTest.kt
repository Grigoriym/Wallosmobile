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
import com.grappim.wallosmobile.feature.subscriptions.domain.model.Currency
import com.grappim.wallosmobile.feature.subscriptions.domain.model.EditSubscriptionParams
import com.grappim.wallosmobile.feature.subscriptions.domain.model.PriceConversion
import com.grappim.wallosmobile.feature.subscriptions.domain.model.Subscription
import com.grappim.wallosmobile.feature.subscriptions.domain.model.WritableBillingCycle
import com.grappim.wallosmobile.feature.subscriptions.domain.repo.SubscriptionsRepository
import com.grappim.wallosmobile.testing.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

    private fun viewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()) = SubscriptionEditorViewModel(
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
    }

    /** A repository that never answers must not sink the whole form — the other three still load. */
    @Test
    fun `a reference-data load that fails leaves its picker empty rather than the screen`() = runTest {
        categoriesRepository.categories = Result.failure(WallosError.Server("boom"))
        subscriptionsRepository.currencies.value = listOf(EURO)

        val state = viewModel().uiState.value

        assertTrue(state.category.options.isEmpty())
        assertEquals(PickerOption(1, "EUR (€)"), state.currency.options.single())
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

        val currencies = MutableStateFlow<List<Currency>>(emptyList())
        var addResult: Result<Int> = Result.success(1)
        var addedParams: AddSubscriptionParams? = null

        override fun observeSubscriptions(): Flow<List<Subscription>> = error("not used by this test")

        override fun observePriceConversion(): Flow<PriceConversion> = error("not used by this test")

        override fun observeCurrencies(): Flow<List<Currency>> = currencies

        override suspend fun refreshSubscriptions(): Result<Unit> = error("not used by this test")

        override fun observeSubscription(id: Int): Flow<Subscription?> = error("not used by this test")

        override suspend fun refreshSubscription(id: Int): Result<Unit> = error("not used by this test")

        override suspend fun addSubscription(params: AddSubscriptionParams): Result<Int> {
            addedParams = params
            return addResult
        }

        override suspend fun editSubscription(id: Int, params: EditSubscriptionParams): Result<Unit> =
            error("not used by this test")

        override suspend fun deleteSubscription(id: Int): Result<Unit> = error("not used by this test")
    }

    private class FakeCategoriesRepository : CategoriesRepository {

        var categories: Result<List<Category>> = Result.success(emptyList())

        override suspend fun getCategories(): Result<List<Category>> = categories

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

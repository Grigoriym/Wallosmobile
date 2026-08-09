package com.grappim.wallosmobile.feature.paymentmethods.ui.list

import com.grappim.wallosmobile.core.api.BaseUrlProvider
import com.grappim.wallosmobile.core.domain.WallosError
import com.grappim.wallosmobile.feature.paymentmethods.domain.model.PaymentMethod
import com.grappim.wallosmobile.feature.paymentmethods.domain.repo.PaymentMethodsRepository
import com.grappim.wallosmobile.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PaymentMethodsViewModelTest {

    private val paymentMethodsRepository = FakePaymentMethodsRepository()
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

    private fun viewModel() = PaymentMethodsViewModel(paymentMethodsRepository, baseUrlProvider)

    @Test
    fun `nothing loads until the screen triggers it`() = runTest {
        paymentMethodsRepository.methods = Result.success(
            listOf(PaymentMethod(id = 1, name = "PayPal", icon = "", enabled = true, inUse = true))
        )

        val state = viewModel().uiState.value

        assertTrue(state.items.isEmpty())
        assertFalse(state.isLoading)
    }

    @Test
    fun `onRetryClick loads the payment methods, building the icon URL off the instance root`() = runTest {
        paymentMethodsRepository.methods = Result.success(
            listOf(
                PaymentMethod(
                    id = 1,
                    name = "PayPal",
                    icon = "images/uploads/icons/paypal.png",
                    enabled = true,
                    inUse = true
                ),
                PaymentMethod(id = 2, name = "Cash", icon = "", enabled = false, inUse = false)
            )
        )
        val sut = viewModel()

        sut.uiState.value.onRetryClick()

        val state = sut.uiState.value
        assertFalse(state.isLoading)
        assertEquals(listOf("PayPal", "Cash"), state.items.map { it.name })
        assertEquals(
            "https://wallos.example.com/images/uploads/icons/paypal.png",
            state.items[0].iconUrl
        )
        assertEquals("", state.items[1].iconUrl)
        assertEquals(listOf(true, false), state.items.map { it.enabled })
    }

    /** The same call drives the first open and a return trip from the editor — see the ViewModel's own note. */
    @Test
    fun `onRetryClick reloads on every call, not only the first`() = runTest {
        paymentMethodsRepository.methods = Result.success(
            listOf(PaymentMethod(id = 1, name = "PayPal", icon = "", enabled = true, inUse = true))
        )
        val sut = viewModel()
        sut.uiState.value.onRetryClick()
        assertEquals(1, sut.uiState.value.items.size)

        paymentMethodsRepository.methods = Result.success(
            listOf(
                PaymentMethod(id = 1, name = "PayPal", icon = "", enabled = true, inUse = true),
                PaymentMethod(id = 2, name = "Cash", icon = "", enabled = false, inUse = false)
            )
        )
        sut.uiState.value.onRetryClick()

        assertEquals(2, sut.uiState.value.items.size)
    }

    @Test
    fun `a failure with nothing loaded is isFailed`() = runTest {
        paymentMethodsRepository.methods = Result.failure(WallosError.Server("boom"))
        val sut = viewModel()

        sut.uiState.value.onRetryClick()

        val state = sut.uiState.value
        assertTrue(state.isFailed)
        assertTrue(state.error.isNotEmpty())
    }

    @Test
    fun `an empty successful load is isEmpty, not isFailed`() = runTest {
        paymentMethodsRepository.methods = Result.success(emptyList())
        val sut = viewModel()

        sut.uiState.value.onRetryClick()

        val state = sut.uiState.value
        assertTrue(state.isEmpty)
        assertFalse(state.isFailed)
    }

    private class FakeBaseUrlProvider : BaseUrlProvider {
        override fun getBaseUrl(): String = "https://wallos.example.com/"
    }

    /**
     * Private to this file, as in `feature:household:ui`'s own ViewModel tests: `:testing` is on
     * every module's test classpath, so a fake declared there would drag
     * `feature:paymentmethods:domain` into modules that have no business seeing it.
     */
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

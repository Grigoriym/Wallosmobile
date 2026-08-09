package com.grappim.wallosmobile.feature.currencies.ui.list

import com.grappim.wallosmobile.core.domain.WallosError
import com.grappim.wallosmobile.feature.currencies.domain.model.Currency
import com.grappim.wallosmobile.feature.currencies.domain.repo.CurrenciesRepository
import com.grappim.wallosmobile.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CurrenciesViewModelTest {

    private val currenciesRepository = FakeCurrenciesRepository()
    private val mainDispatcherRule = MainDispatcherRule()

    @BeforeTest
    fun setup() {
        mainDispatcherRule.setup()
    }

    @AfterTest
    fun tearDown() {
        mainDispatcherRule.tearDown()
    }

    private fun viewModel() = CurrenciesViewModel(currenciesRepository)

    @Test
    fun `nothing loads until the screen triggers it`() = runTest {
        currenciesRepository.currencies = Result.success(
            listOf(usd(), eur())
        )

        val state = viewModel().uiState.value

        assertTrue(state.items.isEmpty())
        assertFalse(state.isLoading)
    }

    @Test
    fun `onRetryClick loads the currencies, main marked`() = runTest {
        currenciesRepository.currencies = Result.success(listOf(usd(), eur()))
        val sut = viewModel()

        sut.uiState.value.onRetryClick()

        val state = sut.uiState.value
        assertFalse(state.isLoading)
        assertEquals(listOf("US Dollar", "Euro"), state.items.map { it.name })
        assertEquals(listOf(true, false), state.items.map { it.isMain })
    }

    /** The same call drives the first open and a return trip from the editor — see the ViewModel's own note. */
    @Test
    fun `onRetryClick reloads on every call, not only the first`() = runTest {
        currenciesRepository.currencies = Result.success(listOf(usd()))
        val sut = viewModel()
        sut.uiState.value.onRetryClick()
        assertEquals(1, sut.uiState.value.items.size)

        currenciesRepository.currencies = Result.success(listOf(usd(), eur()))
        sut.uiState.value.onRetryClick()

        assertEquals(2, sut.uiState.value.items.size)
    }

    @Test
    fun `a failure with nothing loaded is isFailed`() = runTest {
        currenciesRepository.currencies = Result.failure(WallosError.Server("boom"))
        val sut = viewModel()

        sut.uiState.value.onRetryClick()

        val state = sut.uiState.value
        assertTrue(state.isFailed)
        assertTrue(state.error.isNotEmpty())
    }

    @Test
    fun `an empty successful load is isEmpty, not isFailed`() = runTest {
        currenciesRepository.currencies = Result.success(emptyList())
        val sut = viewModel()

        sut.uiState.value.onRetryClick()

        val state = sut.uiState.value
        assertTrue(state.isEmpty)
        assertFalse(state.isFailed)
    }

    private fun usd() =
        Currency(id = 1, name = "US Dollar", symbol = "$", code = "USD", rate = 1.0, inUse = true, isMain = true)

    private fun eur() =
        Currency(id = 2, name = "Euro", symbol = "€", code = "EUR", rate = 0.92, inUse = false, isMain = false)

    /**
     * Private to this file, as in `feature:household:ui`'s own ViewModel tests: `:testing` is on
     * every module's test classpath, so a fake declared there would drag
     * `feature:currencies:domain` into modules that have no business seeing it.
     */
    private class FakeCurrenciesRepository : CurrenciesRepository {

        var currencies: Result<List<Currency>> = Result.success(emptyList())

        override suspend fun getCurrencies(): Result<List<Currency>> = currencies

        override suspend fun addCurrency(name: String, symbol: String, code: String, rate: Double): Result<Int> =
            error("not used by this test")

        override suspend fun editCurrency(
            id: Int,
            name: String,
            symbol: String,
            code: String,
            rate: Double
        ): Result<Unit> = error("not used by this test")

        override suspend fun deleteCurrency(id: Int): Result<Unit> = error("not used by this test")
    }
}

package com.grappim.wallosmobile.feature.currencies.ui.editor

import app.cash.turbine.test
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

class CurrencyEditorViewModelTest {

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

    private fun viewModel(
        currencyId: Int? = null,
        name: String = "",
        symbol: String = "",
        code: String = "",
        rate: Double = 1.0
    ) = CurrencyEditorViewModel(currencyId, name, symbol, code, rate, currenciesRepository)

    @Test
    fun `the route's fields pre-fill an edit`() = runTest {
        val state = viewModel(currencyId = 1, name = "Euro", symbol = "€", code = "EUR", rate = 0.92).uiState.value

        assertEquals("Euro", state.name)
        assertEquals("€", state.symbol)
        assertEquals("EUR", state.code)
        assertEquals("0.92", state.rate)
    }

    @Test
    fun `saving a blank name shows an error and never calls the repository`() = runTest {
        val sut = viewModel(symbol = "$", code = "USD", rate = 1.0)

        sut.uiState.value.onSaveClick()

        assertTrue(sut.uiState.value.error.isNotEmpty())
        assertFalse(currenciesRepository.addCalled)
    }

    @Test
    fun `an unparseable rate shows an error and never calls the repository`() = runTest {
        val sut = viewModel(name = "US Dollar", symbol = "$", code = "USD")
        sut.uiState.value.onRateChange("not a number")

        sut.uiState.value.onSaveClick()

        assertTrue(sut.uiState.value.error.isNotEmpty())
        assertFalse(currenciesRepository.addCalled)
    }

    @Test
    fun `a null currency id adds rather than edits, and emits saved`() = runTest {
        currenciesRepository.addResult = Result.success(9)
        val sut = viewModel()
        sut.uiState.value.onNameChange("US Dollar")
        sut.uiState.value.onSymbolChange("$")
        sut.uiState.value.onCodeChange("USD")
        sut.uiState.value.onRateChange("1.0")

        sut.saved.test {
            sut.uiState.value.onSaveClick()
            awaitItem()
        }

        assertEquals(listOf("US Dollar", "$", "USD", 1.0), currenciesRepository.addedFields)
        assertFalse(sut.uiState.value.isSaving)
    }

    @Test
    fun `a set currency id edits rather than adds, and emits saved`() = runTest {
        currenciesRepository.editResult = Result.success(Unit)
        val sut = viewModel(currencyId = 4, name = "Euro", symbol = "€", code = "EUR", rate = 0.9)
        sut.uiState.value.onRateChange("0.95")

        sut.saved.test {
            sut.uiState.value.onSaveClick()
            awaitItem()
        }

        assertEquals(listOf(4, "Euro", "€", "EUR", 0.95), currenciesRepository.editedFields)
    }

    @Test
    fun `a save failure surfaces the error and never emits saved`() = runTest {
        currenciesRepository.addResult = Result.failure(WallosError.Server("boom"))
        val sut = viewModel(name = "US Dollar", symbol = "$", code = "USD", rate = 1.0)

        sut.uiState.value.onSaveClick()

        assertFalse(sut.uiState.value.isSaving)
        assertTrue(sut.uiState.value.error.isNotEmpty())
    }

    @Test
    fun `delete click opens the confirm dialog without deleting yet`() = runTest {
        val sut = viewModel(currencyId = 1, name = "US Dollar")

        sut.uiState.value.onDeleteClick()

        assertTrue(sut.uiState.value.isDeleteDialogOpen)
        assertFalse(currenciesRepository.deleteCalled)
    }

    @Test
    fun `confirming delete calls the repository and emits deleted`() = runTest {
        currenciesRepository.deleteResult = Result.success(Unit)
        val sut = viewModel(currencyId = 2, name = "Euro")
        sut.uiState.value.onDeleteClick()

        sut.deleted.test {
            sut.uiState.value.onDeleteConfirm()
            awaitItem()
        }

        assertEquals(2, currenciesRepository.deletedId)
        assertFalse(sut.uiState.value.isDeleteDialogOpen)
    }

    /** The main currency or one still referenced surfaces `WallosError.InUse`, not a crash. */
    @Test
    fun `a delete failure keeps the dialog open with the in-use error`() = runTest {
        currenciesRepository.deleteResult = Result.failure(WallosError.InUse("Cannot delete currency"))
        val sut = viewModel(currencyId = 1, name = "US Dollar")
        sut.uiState.value.onDeleteClick()

        sut.uiState.value.onDeleteConfirm()

        val state = sut.uiState.value
        assertTrue(state.isDeleteDialogOpen)
        assertFalse(state.isDeleting)
        assertTrue(state.deleteError.isNotEmpty())
    }

    private class FakeCurrenciesRepository : CurrenciesRepository {

        var addResult: Result<Int> = Result.success(1)
        var editResult: Result<Unit> = Result.success(Unit)
        var deleteResult: Result<Unit> = Result.success(Unit)

        var addCalled = false
            private set
        var deleteCalled = false
            private set
        var addedFields: List<Any>? = null
            private set
        var editedFields: List<Any>? = null
            private set
        var deletedId: Int? = null
            private set

        override suspend fun getCurrencies(): Result<List<Currency>> = error("not used by this test")

        override suspend fun addCurrency(name: String, symbol: String, code: String, rate: Double): Result<Int> {
            addCalled = true
            addedFields = listOf(name, symbol, code, rate)
            return addResult
        }

        override suspend fun editCurrency(
            id: Int,
            name: String,
            symbol: String,
            code: String,
            rate: Double
        ): Result<Unit> {
            editedFields = listOf(id, name, symbol, code, rate)
            return editResult
        }

        override suspend fun deleteCurrency(id: Int): Result<Unit> {
            deleteCalled = true
            deletedId = id
            return deleteResult
        }
    }
}

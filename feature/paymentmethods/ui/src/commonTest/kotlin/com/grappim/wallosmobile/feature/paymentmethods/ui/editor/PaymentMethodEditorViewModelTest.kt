package com.grappim.wallosmobile.feature.paymentmethods.ui.editor

import app.cash.turbine.test
import com.grappim.wallosmobile.core.domain.WallosError
import com.grappim.wallosmobile.feature.paymentmethods.domain.model.IconFile
import com.grappim.wallosmobile.feature.paymentmethods.domain.model.PaymentMethod
import com.grappim.wallosmobile.feature.paymentmethods.domain.repo.PaymentMethodsRepository
import com.grappim.wallosmobile.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PaymentMethodEditorViewModelTest {

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

    private fun viewModel(paymentMethodId: Int? = null, name: String = "", enabled: Boolean = true) =
        PaymentMethodEditorViewModel(paymentMethodId, name, enabled, paymentMethodsRepository)

    @Test
    fun `the route's name and enabled flag pre-fill an edit, and icon url starts blank`() = runTest {
        val state = viewModel(paymentMethodId = 1, name = "PayPal", enabled = false).uiState.value

        assertEquals("PayPal", state.name)
        assertFalse(state.enabled)
        assertEquals("", state.iconUrl)
    }

    @Test
    fun `saving a blank name shows an error and never calls the repository`() = runTest {
        val sut = viewModel()

        sut.uiState.value.onSaveClick()

        assertTrue(sut.uiState.value.error.isNotEmpty())
        assertFalse(paymentMethodsRepository.addCalled)
    }

    @Test
    fun `a null payment method id adds rather than edits, and emits saved`() = runTest {
        paymentMethodsRepository.addResult = Result.success(9)
        val sut = viewModel()
        sut.uiState.value.onNameChange("PayPal")
        sut.uiState.value.onEnabledChange(false)

        sut.saved.test {
            sut.uiState.value.onSaveClick()
            awaitItem()
        }

        assertEquals("PayPal", paymentMethodsRepository.addedName)
        assertEquals(false, paymentMethodsRepository.addedEnabled)
        assertNull(paymentMethodsRepository.addedIconUrl)
        assertFalse(sut.uiState.value.isSaving)
    }

    @Test
    fun `a blank icon url is sent as null, leaving the icon untouched`() = runTest {
        paymentMethodsRepository.addResult = Result.success(9)
        val sut = viewModel()
        sut.uiState.value.onNameChange("PayPal")
        sut.uiState.value.onIconUrlChange("   ")

        sut.saved.test {
            sut.uiState.value.onSaveClick()
            awaitItem()
        }

        assertNull(paymentMethodsRepository.addedIconUrl)
    }

    @Test
    fun `a non-blank icon url is forwarded trimmed`() = runTest {
        paymentMethodsRepository.addResult = Result.success(9)
        val sut = viewModel()
        sut.uiState.value.onNameChange("PayPal")
        sut.uiState.value.onIconUrlChange("  https://example.com/paypal.png  ")

        sut.saved.test {
            sut.uiState.value.onSaveClick()
            awaitItem()
        }

        assertEquals("https://example.com/paypal.png", paymentMethodsRepository.addedIconUrl)
    }

    @Test
    fun `picking an icon file forwards it to the repository on save`() = runTest {
        paymentMethodsRepository.addResult = Result.success(9)
        val sut = viewModel()
        sut.uiState.value.onNameChange("PayPal")
        val file = IconFile(bytes = byteArrayOf(1, 2, 3), fileName = "icon.png", mimeType = "image/png")
        sut.uiState.value.onIconFilePick(file)

        assertEquals(file, sut.uiState.value.iconFile)

        sut.saved.test {
            sut.uiState.value.onSaveClick()
            awaitItem()
        }

        assertEquals(file, paymentMethodsRepository.addedIconFile)
    }

    @Test
    fun `a set payment method id edits rather than adds, and emits saved`() = runTest {
        paymentMethodsRepository.editResult = Result.success(Unit)
        val sut = viewModel(paymentMethodId = 4, name = "PayPal", enabled = true)
        sut.uiState.value.onNameChange("PayPal renamed")
        sut.uiState.value.onEnabledChange(false)

        sut.saved.test {
            sut.uiState.value.onSaveClick()
            awaitItem()
        }

        assertEquals(4, paymentMethodsRepository.editedId)
        assertEquals("PayPal renamed", paymentMethodsRepository.editedName)
        assertEquals(false, paymentMethodsRepository.editedEnabled)
    }

    @Test
    fun `a save failure surfaces the error and never emits saved`() = runTest {
        paymentMethodsRepository.addResult = Result.failure(WallosError.Server("boom"))
        val sut = viewModel()
        sut.uiState.value.onNameChange("PayPal")

        sut.uiState.value.onSaveClick()

        assertFalse(sut.uiState.value.isSaving)
        assertTrue(sut.uiState.value.error.isNotEmpty())
    }

    @Test
    fun `delete click opens the confirm dialog without deleting yet`() = runTest {
        val sut = viewModel(paymentMethodId = 1, name = "PayPal")

        sut.uiState.value.onDeleteClick()

        assertTrue(sut.uiState.value.isDeleteDialogOpen)
        assertFalse(paymentMethodsRepository.deleteCalled)
    }

    @Test
    fun `confirming delete calls the repository and emits deleted`() = runTest {
        paymentMethodsRepository.deleteResult = Result.success(Unit)
        val sut = viewModel(paymentMethodId = 1, name = "PayPal")
        sut.uiState.value.onDeleteClick()

        sut.deleted.test {
            sut.uiState.value.onDeleteConfirm()
            awaitItem()
        }

        assertEquals(1, paymentMethodsRepository.deletedId)
        assertFalse(sut.uiState.value.isDeleteDialogOpen)
    }

    /** A method still referenced by a subscription surfaces `WallosError.InUse`, not a crash. */
    @Test
    fun `a delete failure keeps the dialog open with the in-use error`() = runTest {
        paymentMethodsRepository.deleteResult = Result.failure(WallosError.InUse("Payment method in use"))
        val sut = viewModel(paymentMethodId = 1, name = "PayPal")
        sut.uiState.value.onDeleteClick()

        sut.uiState.value.onDeleteConfirm()

        val state = sut.uiState.value
        assertTrue(state.isDeleteDialogOpen)
        assertFalse(state.isDeleting)
        assertTrue(state.deleteError.isNotEmpty())
    }

    private class FakePaymentMethodsRepository : PaymentMethodsRepository {

        var addResult: Result<Int> = Result.success(1)
        var editResult: Result<Unit> = Result.success(Unit)
        var deleteResult: Result<Unit> = Result.success(Unit)

        var addCalled = false
            private set
        var deleteCalled = false
            private set
        var addedName: String? = null
            private set
        var addedEnabled: Boolean? = null
            private set
        var addedIconUrl: String? = null
            private set
        var addedIconFile: IconFile? = null
            private set
        var editedId: Int? = null
            private set
        var editedName: String? = null
            private set
        var editedEnabled: Boolean? = null
            private set
        var deletedId: Int? = null
            private set

        override suspend fun getPaymentMethods(): Result<List<PaymentMethod>> = error("not used by this test")

        override suspend fun addPaymentMethod(
            name: String,
            enabled: Boolean,
            iconUrl: String?,
            iconFile: IconFile?
        ): Result<Int> {
            addCalled = true
            addedName = name
            addedEnabled = enabled
            addedIconUrl = iconUrl
            addedIconFile = iconFile
            return addResult
        }

        override suspend fun editPaymentMethod(
            id: Int,
            name: String,
            enabled: Boolean,
            iconUrl: String?,
            iconFile: IconFile?
        ): Result<Unit> {
            editedId = id
            editedName = name
            editedEnabled = enabled
            return editResult
        }

        override suspend fun deletePaymentMethod(id: Int): Result<Unit> {
            deleteCalled = true
            deletedId = id
            return deleteResult
        }
    }
}

package com.grappim.wallosmobile.feature.household.ui.editor

import app.cash.turbine.test
import com.grappim.wallosmobile.core.domain.WallosError
import com.grappim.wallosmobile.feature.household.domain.model.HouseholdMember
import com.grappim.wallosmobile.feature.household.domain.repo.HouseholdRepository
import com.grappim.wallosmobile.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HouseholdMemberEditorViewModelTest {

    private val householdRepository = FakeHouseholdRepository()
    private val mainDispatcherRule = MainDispatcherRule()

    @BeforeTest
    fun setup() {
        mainDispatcherRule.setup()
    }

    @AfterTest
    fun tearDown() {
        mainDispatcherRule.tearDown()
    }

    private fun viewModel(memberId: Int? = null, name: String = "", email: String = "") =
        HouseholdMemberEditorViewModel(memberId, name, email, householdRepository)

    @Test
    fun `the route's name and email pre-fill an edit`() = runTest {
        val state = viewModel(memberId = 1, name = "Gregory", email = "gregory@example.com").uiState.value

        assertEquals("Gregory", state.name)
        assertEquals("gregory@example.com", state.email)
    }

    @Test
    fun `saving a blank name shows an error and never calls the repository`() = runTest {
        val sut = viewModel()

        sut.uiState.value.onSaveClick()

        assertTrue(sut.uiState.value.error.isNotEmpty())
        assertFalse(householdRepository.addCalled)
    }

    @Test
    fun `a null member id adds rather than edits, and emits saved`() = runTest {
        householdRepository.addResult = Result.success(9)
        val sut = viewModel()
        sut.uiState.value.onNameChange("Sam")
        sut.uiState.value.onEmailChange("sam@example.com")

        sut.saved.test {
            sut.uiState.value.onSaveClick()
            awaitItem()
        }

        assertEquals("Sam" to "sam@example.com", householdRepository.addedNameAndEmail)
        assertFalse(sut.uiState.value.isSaving)
    }

    @Test
    fun `a blank email is accepted, since it's optional server-side`() = runTest {
        householdRepository.addResult = Result.success(9)
        val sut = viewModel()
        sut.uiState.value.onNameChange("Sam")

        sut.saved.test {
            sut.uiState.value.onSaveClick()
            awaitItem()
        }

        assertEquals("Sam" to "", householdRepository.addedNameAndEmail)
    }

    @Test
    fun `a set member id edits rather than adds, and emits saved`() = runTest {
        householdRepository.editResult = Result.success(Unit)
        val sut = viewModel(memberId = 4, name = "Gregory", email = "gregory@example.com")
        sut.uiState.value.onNameChange("Greg")

        sut.saved.test {
            sut.uiState.value.onSaveClick()
            awaitItem()
        }

        assertEquals(Triple(4, "Greg", "gregory@example.com"), householdRepository.editedIdNameAndEmail)
    }

    @Test
    fun `a save failure surfaces the error and never emits saved`() = runTest {
        householdRepository.addResult = Result.failure(WallosError.Server("boom"))
        val sut = viewModel()
        sut.uiState.value.onNameChange("Sam")

        sut.uiState.value.onSaveClick()

        assertFalse(sut.uiState.value.isSaving)
        assertTrue(sut.uiState.value.error.isNotEmpty())
    }

    @Test
    fun `delete click opens the confirm dialog without deleting yet`() = runTest {
        val sut = viewModel(memberId = 1, name = "Gregory")

        sut.uiState.value.onDeleteClick()

        assertTrue(sut.uiState.value.isDeleteDialogOpen)
        assertFalse(householdRepository.deleteCalled)
    }

    @Test
    fun `confirming delete calls the repository and emits deleted`() = runTest {
        householdRepository.deleteResult = Result.success(Unit)
        val sut = viewModel(memberId = 1, name = "Gregory")
        sut.uiState.value.onDeleteClick()

        sut.deleted.test {
            sut.uiState.value.onDeleteConfirm()
            awaitItem()
        }

        assertEquals(1, householdRepository.deletedId)
        assertFalse(sut.uiState.value.isDeleteDialogOpen)
    }

    /** The default member (id 1) or one still referenced surfaces `WallosError.InUse`, not a crash. */
    @Test
    fun `a delete failure keeps the dialog open with the in-use error`() = runTest {
        householdRepository.deleteResult = Result.failure(WallosError.InUse("Cannot delete member"))
        val sut = viewModel(memberId = 1, name = "Gregory")
        sut.uiState.value.onDeleteClick()

        sut.uiState.value.onDeleteConfirm()

        val state = sut.uiState.value
        assertTrue(state.isDeleteDialogOpen)
        assertFalse(state.isDeleting)
        assertTrue(state.deleteError.isNotEmpty())
    }

    private class FakeHouseholdRepository : HouseholdRepository {

        var addResult: Result<Int> = Result.success(1)
        var editResult: Result<Unit> = Result.success(Unit)
        var deleteResult: Result<Unit> = Result.success(Unit)

        var addCalled = false
            private set
        var deleteCalled = false
            private set
        var addedNameAndEmail: Pair<String, String>? = null
            private set
        var editedIdNameAndEmail: Triple<Int, String, String>? = null
            private set
        var deletedId: Int? = null
            private set

        override suspend fun getMembers(): Result<List<HouseholdMember>> = error("not used by this test")

        override suspend fun addMember(name: String, email: String): Result<Int> {
            addCalled = true
            addedNameAndEmail = name to email
            return addResult
        }

        override suspend fun editMember(id: Int, name: String, email: String): Result<Unit> {
            editedIdNameAndEmail = Triple(id, name, email)
            return editResult
        }

        override suspend fun deleteMember(id: Int): Result<Unit> {
            deleteCalled = true
            deletedId = id
            return deleteResult
        }
    }
}

package com.grappim.wallosmobile.feature.categories.ui.editor

import app.cash.turbine.test
import com.grappim.wallosmobile.core.domain.WallosError
import com.grappim.wallosmobile.feature.categories.domain.model.Category
import com.grappim.wallosmobile.feature.categories.domain.repo.CategoriesRepository
import com.grappim.wallosmobile.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CategoryEditorViewModelTest {

    private val categoriesRepository = FakeCategoriesRepository()
    private val mainDispatcherRule = MainDispatcherRule()

    @BeforeTest
    fun setup() {
        mainDispatcherRule.setup()
    }

    @AfterTest
    fun tearDown() {
        mainDispatcherRule.tearDown()
    }

    private fun viewModel(categoryId: Int? = null, name: String = "") =
        CategoryEditorViewModel(categoryId, name, categoriesRepository)

    @Test
    fun `the route's name pre-fills an edit`() = runTest {
        val state = viewModel(categoryId = 1, name = "Entertainment").uiState.value

        assertEquals("Entertainment", state.name)
    }

    @Test
    fun `saving a blank name shows an error and never calls the repository`() = runTest {
        val sut = viewModel()

        sut.uiState.value.onSaveClick()

        assertTrue(sut.uiState.value.error.isNotEmpty())
        assertFalse(categoriesRepository.addCalled)
    }

    @Test
    fun `a null category id adds rather than edits, and emits saved`() = runTest {
        categoriesRepository.addResult = Result.success(9)
        val sut = viewModel()
        sut.uiState.value.onNameChange("Streaming")

        sut.saved.test {
            sut.uiState.value.onSaveClick()
            awaitItem()
        }

        assertEquals("Streaming", categoriesRepository.addedName)
        assertFalse(sut.uiState.value.isSaving)
    }

    @Test
    fun `a set category id edits rather than adds, and emits saved`() = runTest {
        categoriesRepository.editResult = Result.success(Unit)
        val sut = viewModel(categoryId = 4, name = "Entertainment")
        sut.uiState.value.onNameChange("Fun")

        sut.saved.test {
            sut.uiState.value.onSaveClick()
            awaitItem()
        }

        assertEquals(4 to "Fun", categoriesRepository.editedIdAndName)
    }

    @Test
    fun `a save failure surfaces the error and never emits saved`() = runTest {
        categoriesRepository.addResult = Result.failure(WallosError.Server("boom"))
        val sut = viewModel()
        sut.uiState.value.onNameChange("Streaming")

        sut.uiState.value.onSaveClick()

        assertFalse(sut.uiState.value.isSaving)
        assertTrue(sut.uiState.value.error.isNotEmpty())
    }

    @Test
    fun `delete click opens the confirm dialog without deleting yet`() = runTest {
        val sut = viewModel(categoryId = 1, name = "Entertainment")

        sut.uiState.value.onDeleteClick()

        assertTrue(sut.uiState.value.isDeleteDialogOpen)
        assertFalse(categoriesRepository.deleteCalled)
    }

    @Test
    fun `confirming delete calls the repository and emits deleted`() = runTest {
        categoriesRepository.deleteResult = Result.success(Unit)
        val sut = viewModel(categoryId = 1, name = "Entertainment")
        sut.uiState.value.onDeleteClick()

        sut.deleted.test {
            sut.uiState.value.onDeleteConfirm()
            awaitItem()
        }

        assertEquals(1, categoriesRepository.deletedId)
        assertFalse(sut.uiState.value.isDeleteDialogOpen)
    }

    /** The default category (id 1) or one still referenced surfaces `WallosError.InUse`, not a crash. */
    @Test
    fun `a delete failure keeps the dialog open with the in-use error`() = runTest {
        categoriesRepository.deleteResult = Result.failure(WallosError.InUse("Cannot delete category"))
        val sut = viewModel(categoryId = 1, name = "Entertainment")
        sut.uiState.value.onDeleteClick()

        sut.uiState.value.onDeleteConfirm()

        val state = sut.uiState.value
        assertTrue(state.isDeleteDialogOpen)
        assertFalse(state.isDeleting)
        assertTrue(state.deleteError.isNotEmpty())
    }

    private class FakeCategoriesRepository : CategoriesRepository {

        var addResult: Result<Int> = Result.success(1)
        var editResult: Result<Unit> = Result.success(Unit)
        var deleteResult: Result<Unit> = Result.success(Unit)

        var addCalled = false
            private set
        var deleteCalled = false
            private set
        var addedName: String? = null
            private set
        var editedIdAndName: Pair<Int, String>? = null
            private set
        var deletedId: Int? = null
            private set

        override suspend fun getCategories(): Result<List<Category>> = error("not used by this test")

        override suspend fun addCategory(name: String): Result<Int> {
            addCalled = true
            addedName = name
            return addResult
        }

        override suspend fun editCategory(id: Int, name: String): Result<Unit> {
            editedIdAndName = id to name
            return editResult
        }

        override suspend fun deleteCategory(id: Int): Result<Unit> {
            deleteCalled = true
            deletedId = id
            return deleteResult
        }
    }
}

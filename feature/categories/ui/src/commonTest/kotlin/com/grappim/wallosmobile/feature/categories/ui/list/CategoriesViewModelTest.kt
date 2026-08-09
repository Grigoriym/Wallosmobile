package com.grappim.wallosmobile.feature.categories.ui.list

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

class CategoriesViewModelTest {

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

    private fun viewModel() = CategoriesViewModel(categoriesRepository)

    @Test
    fun `nothing loads until the screen triggers it`() = runTest {
        categoriesRepository.categories = Result.success(listOf(Category(id = 1, name = "Entertainment", inUse = true)))

        val state = viewModel().uiState.value

        assertTrue(state.items.isEmpty())
        assertFalse(state.isLoading)
    }

    @Test
    fun `onRetryClick loads the categories`() = runTest {
        categoriesRepository.categories = Result.success(
            listOf(
                Category(id = 1, name = "Entertainment", inUse = true),
                Category(id = 2, name = "Utilities", inUse = false)
            )
        )
        val sut = viewModel()

        sut.uiState.value.onRetryClick()

        val state = sut.uiState.value
        assertFalse(state.isLoading)
        assertEquals(listOf("Entertainment", "Utilities"), state.items.map { it.name })
    }

    /** The same call drives the first open and a return trip from the editor — see the ViewModel's own note. */
    @Test
    fun `onRetryClick reloads on every call, not only the first`() = runTest {
        categoriesRepository.categories = Result.success(listOf(Category(id = 1, name = "Entertainment", inUse = true)))
        val sut = viewModel()
        sut.uiState.value.onRetryClick()
        assertEquals(1, sut.uiState.value.items.size)

        categoriesRepository.categories = Result.success(
            listOf(
                Category(id = 1, name = "Entertainment", inUse = true),
                Category(id = 2, name = "Utilities", inUse = false)
            )
        )
        sut.uiState.value.onRetryClick()

        assertEquals(2, sut.uiState.value.items.size)
    }

    @Test
    fun `a failure with nothing loaded is isFailed`() = runTest {
        categoriesRepository.categories = Result.failure(WallosError.Server("boom"))
        val sut = viewModel()

        sut.uiState.value.onRetryClick()

        val state = sut.uiState.value
        assertTrue(state.isFailed)
        assertTrue(state.error.isNotEmpty())
    }

    @Test
    fun `an empty successful load is isEmpty, not isFailed`() = runTest {
        categoriesRepository.categories = Result.success(emptyList())
        val sut = viewModel()

        sut.uiState.value.onRetryClick()

        val state = sut.uiState.value
        assertTrue(state.isEmpty)
        assertFalse(state.isFailed)
    }

    /**
     * Private to this file, as in the other UI ViewModel tests: `:testing` is on every module's
     * test classpath, so a fake declared there would drag `feature:categories:domain` into modules
     * that have no business seeing it.
     */
    private class FakeCategoriesRepository : CategoriesRepository {

        var categories: Result<List<Category>> = Result.success(emptyList())

        override suspend fun getCategories(): Result<List<Category>> = categories

        override suspend fun addCategory(name: String): Result<Int> = error("not used by this test")

        override suspend fun editCategory(id: Int, name: String): Result<Unit> = error("not used by this test")

        override suspend fun deleteCategory(id: Int): Result<Unit> = error("not used by this test")
    }
}

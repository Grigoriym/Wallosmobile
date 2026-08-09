package com.grappim.wallosmobile.feature.household.ui.list

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

class HouseholdViewModelTest {

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

    private fun viewModel() = HouseholdViewModel(householdRepository)

    @Test
    fun `nothing loads until the screen triggers it`() = runTest {
        householdRepository.members = Result.success(
            listOf(HouseholdMember(id = 1, name = "Gregory", email = "gregory@example.com", inUse = true))
        )

        val state = viewModel().uiState.value

        assertTrue(state.items.isEmpty())
        assertFalse(state.isLoading)
    }

    @Test
    fun `onRetryClick loads the members`() = runTest {
        householdRepository.members = Result.success(
            listOf(
                HouseholdMember(id = 1, name = "Gregory", email = "gregory@example.com", inUse = true),
                HouseholdMember(id = 2, name = "Sam", email = "", inUse = false)
            )
        )
        val sut = viewModel()

        sut.uiState.value.onRetryClick()

        val state = sut.uiState.value
        assertFalse(state.isLoading)
        assertEquals(listOf("Gregory", "Sam"), state.items.map { it.name })
        assertEquals(listOf("gregory@example.com", ""), state.items.map { it.email })
    }

    /** The same call drives the first open and a return trip from the editor — see the ViewModel's own note. */
    @Test
    fun `onRetryClick reloads on every call, not only the first`() = runTest {
        householdRepository.members = Result.success(
            listOf(HouseholdMember(id = 1, name = "Gregory", email = "", inUse = true))
        )
        val sut = viewModel()
        sut.uiState.value.onRetryClick()
        assertEquals(1, sut.uiState.value.items.size)

        householdRepository.members = Result.success(
            listOf(
                HouseholdMember(id = 1, name = "Gregory", email = "", inUse = true),
                HouseholdMember(id = 2, name = "Sam", email = "", inUse = false)
            )
        )
        sut.uiState.value.onRetryClick()

        assertEquals(2, sut.uiState.value.items.size)
    }

    @Test
    fun `a failure with nothing loaded is isFailed`() = runTest {
        householdRepository.members = Result.failure(WallosError.Server("boom"))
        val sut = viewModel()

        sut.uiState.value.onRetryClick()

        val state = sut.uiState.value
        assertTrue(state.isFailed)
        assertTrue(state.error.isNotEmpty())
    }

    @Test
    fun `an empty successful load is isEmpty, not isFailed`() = runTest {
        householdRepository.members = Result.success(emptyList())
        val sut = viewModel()

        sut.uiState.value.onRetryClick()

        val state = sut.uiState.value
        assertTrue(state.isEmpty)
        assertFalse(state.isFailed)
    }

    /**
     * Private to this file, as in `feature:categories:ui`'s own ViewModel tests: `:testing` is on
     * every module's test classpath, so a fake declared there would drag `feature:household:domain`
     * into modules that have no business seeing it.
     */
    private class FakeHouseholdRepository : HouseholdRepository {

        var members: Result<List<HouseholdMember>> = Result.success(emptyList())

        override suspend fun getMembers(): Result<List<HouseholdMember>> = members

        override suspend fun addMember(name: String, email: String): Result<Int> = error("not used by this test")

        override suspend fun editMember(id: Int, name: String, email: String): Result<Unit> =
            error("not used by this test")

        override suspend fun deleteMember(id: Int): Result<Unit> = error("not used by this test")
    }
}

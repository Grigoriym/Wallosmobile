package com.grappim.wallosmobile.feature.profile.ui

import com.grappim.wallosmobile.core.domain.WallosError
import com.grappim.wallosmobile.feature.profile.domain.model.BudgetPeriodType
import com.grappim.wallosmobile.feature.profile.domain.model.User
import com.grappim.wallosmobile.feature.profile.domain.repo.ProfileRepository
import com.grappim.wallosmobile.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProfileViewModelTest {

    private val mainDispatcherRule = MainDispatcherRule()

    @BeforeTest
    fun setup() {
        mainDispatcherRule.setup()
    }

    @AfterTest
    fun tearDown() {
        mainDispatcherRule.tearDown()
    }

    private fun user(
        budget: Double = 50.0,
        periodBudget: Double = 12.5,
        budgetPeriodType: BudgetPeriodType = BudgetPeriodType.WEEKLY,
        budgetPeriodAnchorDate: LocalDate = LocalDate(2026, 1, 1)
    ) = User(
        id = 1,
        username = "greg",
        email = "greg@example.com",
        budget = budget,
        periodBudget = periodBudget,
        mainCurrencyId = 1,
        budgetPeriodType = budgetPeriodType,
        budgetPeriodAnchorDate = budgetPeriodAnchorDate,
        totpEnabled = false
    )

    @Test
    fun `a successful load pre-fills both budget fields`() = runTest {
        val repository =
            FakeProfileRepository(getUserResult = Result.success(user(budget = 100.0, periodBudget = 25.0)))
        val sut = ProfileViewModel(repository)

        sut.uiState.value.onRetryClick()

        val state = sut.uiState.value
        assertEquals("100.0", state.budget)
        assertEquals("25.0", state.periodBudget)
        assertFalse(state.isLoading)
    }

    @Test
    fun `a failed load with nothing shown yet surfaces as failed`() = runTest {
        val repository = FakeProfileRepository(getUserResult = Result.failure(WallosError.Server("boom")))
        val sut = ProfileViewModel(repository)

        sut.uiState.value.onRetryClick()

        assertTrue(sut.uiState.value.isFailed)
    }

    @Test
    fun `saving sends the loaded period type and anchor date unchanged`() = runTest {
        val repository = FakeProfileRepository(
            getUserResult = Result.success(
                user(
                    budgetPeriodType = BudgetPeriodType.FORTNIGHTLY,
                    budgetPeriodAnchorDate = LocalDate(2026, 3, 4)
                )
            )
        )
        val sut = ProfileViewModel(repository)
        sut.uiState.value.onRetryClick()
        sut.uiState.value.onBudgetChange("60.0")
        sut.uiState.value.onPeriodBudgetChange("15.0")

        sut.uiState.value.onSaveClick()

        assertEquals(60.0, repository.setBudgetCall?.monthlyBudget)
        assertEquals(15.0, repository.setBudgetCall?.periodBudget)
        assertEquals(BudgetPeriodType.FORTNIGHTLY, repository.setBudgetCall?.periodType)
        assertEquals(LocalDate(2026, 3, 4), repository.setBudgetCall?.anchorDate)
        assertFalse(sut.uiState.value.isSaving)
    }

    @Test
    fun `saving before a load ever succeeded shows an error and never calls the repository`() = runTest {
        val repository = FakeProfileRepository()
        val sut = ProfileViewModel(repository)

        sut.uiState.value.onSaveClick()

        assertTrue(sut.uiState.value.saveError.isNotEmpty())
        assertEquals(null, repository.setBudgetCall)
    }

    @Test
    fun `an unparseable budget shows an error and never calls the repository`() = runTest {
        val repository = FakeProfileRepository(getUserResult = Result.success(user()))
        val sut = ProfileViewModel(repository)
        sut.uiState.value.onRetryClick()
        sut.uiState.value.onBudgetChange("not a number")

        sut.uiState.value.onSaveClick()

        assertTrue(sut.uiState.value.saveError.isNotEmpty())
        assertEquals(null, repository.setBudgetCall)
    }

    @Test
    fun `a save failure surfaces the error`() = runTest {
        val repository = FakeProfileRepository(
            getUserResult = Result.success(user()),
            setBudgetResult = Result.failure(WallosError.Server("boom"))
        )
        val sut = ProfileViewModel(repository)
        sut.uiState.value.onRetryClick()

        sut.uiState.value.onSaveClick()

        assertFalse(sut.uiState.value.isSaving)
        assertTrue(sut.uiState.value.saveError.isNotEmpty())
    }

    private class FakeProfileRepository(
        private val getUserResult: Result<User> = Result.failure(WallosError.Server("not stubbed")),
        private val setBudgetResult: Result<Unit> = Result.success(Unit)
    ) : ProfileRepository {

        var setBudgetCall: SetBudgetCall? = null
            private set

        override suspend fun getUser(): Result<User> = getUserResult

        override suspend fun setBudget(
            monthlyBudget: Double,
            periodBudget: Double,
            periodType: BudgetPeriodType,
            anchorDate: LocalDate
        ): Result<Unit> {
            setBudgetCall = SetBudgetCall(monthlyBudget, periodBudget, periodType, anchorDate)
            return setBudgetResult
        }

        data class SetBudgetCall(
            val monthlyBudget: Double,
            val periodBudget: Double,
            val periodType: BudgetPeriodType,
            val anchorDate: LocalDate
        )
    }
}

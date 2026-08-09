package com.grappim.wallosmobile.feature.profile.data

import com.grappim.wallosmobile.core.api.FormParams
import com.grappim.wallosmobile.core.domain.WallosError
import com.grappim.wallosmobile.feature.profile.data.mapper.UserMapper
import com.grappim.wallosmobile.feature.profile.domain.model.BudgetPeriodType
import com.grappim.wallosmobile.feature.profile.dto.UserDTO
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProfileRepositoryImplTest {

    @Test
    fun `getUser maps the DTO through the mapper`() = runTest {
        val api = FakeProfileApi(
            user = UserDTO(
                id = 1,
                username = "gregorz",
                email = "gregorz@example.com",
                budget = 150.5,
                periodBudget = 25.0,
                mainCurrency = 1,
                budgetPeriodType = "monthly",
                budgetPeriodAnchorDate = "2026-07-18",
                totpEnabled = 0
            )
        )

        val result = repository(api).getUser().getOrThrow()

        assertEquals(1, result.id)
        assertEquals("gregorz", result.username)
        assertEquals(150.5, result.budget)
        assertEquals(25.0, result.periodBudget)
        assertEquals(1, result.mainCurrencyId)
        assertEquals(BudgetPeriodType.MONTHLY, result.budgetPeriodType)
    }

    @Test
    fun `a failed getUser surfaces the WallosError`() = runTest {
        val api = FakeProfileApi(failure = WallosError.Server("502 Bad Gateway"))

        assertFailsWith<WallosError.Server> { repository(api).getUser().getOrThrow() }
    }

    @Test
    fun `setBudget always sends the period type and anchor date alongside the amounts`() = runTest {
        val api = FakeProfileApi()

        repository(api).setBudget(
            monthlyBudget = 150.0,
            periodBudget = 25.0,
            periodType = BudgetPeriodType.WEEKLY,
            anchorDate = LocalDate(2026, 7, 18)
        ).getOrThrow()

        val fields = api.lastSetBudgetFields?.asMap()
        assertEquals("150.0", fields?.get("monthly_budget"))
        assertEquals("25.0", fields?.get("period_budget"))
        assertEquals("weekly", fields?.get("budget_period_type"))
        assertEquals("2026-07-18", fields?.get("budget_period_anchor_date"))
    }

    @Test
    fun `a failed setBudget surfaces the WallosError`() = runTest {
        val api = FakeProfileApi(failure = WallosError.Server("502 Bad Gateway"))

        assertFailsWith<WallosError.Server> {
            repository(api).setBudget(150.0, 25.0, BudgetPeriodType.MONTHLY, LocalDate(2026, 7, 18)).getOrThrow()
        }
    }

    private fun repository(api: ProfileApi): ProfileRepositoryImpl = ProfileRepositoryImpl(
        api = api,
        userMapper = UserMapper(),
        dispatcher = UnconfinedTestDispatcher()
    )

    private class FakeProfileApi(private val user: UserDTO? = null, private val failure: Throwable? = null) :
        ProfileApi {

        var lastSetBudgetFields: FormParams? = null
            private set

        override suspend fun getUser(): UserDTO {
            failure?.let { throw it }
            return user ?: error("user not set")
        }

        override suspend fun setBudget(fields: FormParams) {
            failure?.let { throw it }
            lastSetBudgetFields = fields
        }
    }
}

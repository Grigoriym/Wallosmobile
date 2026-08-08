package com.grappim.wallosmobile.feature.dashboard.data

import com.grappim.wallosmobile.core.domain.WallosError
import com.grappim.wallosmobile.feature.dashboard.data.mapper.MonthlyCostMapper
import com.grappim.wallosmobile.feature.dashboard.data.mapper.PeriodBudgetMapper
import com.grappim.wallosmobile.feature.dashboard.dto.MonthlyCostDTO
import com.grappim.wallosmobile.feature.dashboard.dto.PeriodBudgetDTO
import com.grappim.wallosmobile.utils.formatter.decimal.MoneyFormatter
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DashboardRepositoryImplTest {

    @Test
    fun `getMonthlyCost maps the DTO through the mapper`() = runTest {
        val api = FakeDashboardApi(
            monthlyCost = MonthlyCostDTO(title = "March 2025", monthlyCost = "120.24", currencySymbol = "€")
        )

        val result = repository(api).getMonthlyCost(month = 3, year = 2025).getOrThrow()

        assertEquals("March 2025", result.title)
        assertEquals(120.24, result.amount)
        assertEquals(3, api.lastMonthlyCostMonth)
        assertEquals(2025, api.lastMonthlyCostYear)
    }

    @Test
    fun `a failed getMonthlyCost surfaces the WallosError`() = runTest {
        val api = FakeDashboardApi(monthlyCostFailure = WallosError.Server("502 Bad Gateway"))

        assertFailsWith<WallosError.Server> { repository(api).getMonthlyCost(month = 3, year = 2025).getOrThrow() }
    }

    @Test
    fun `getPeriodBudget maps the DTO through the mapper and forwards the reference date`() = runTest {
        val api = FakeDashboardApi(
            periodBudget = PeriodBudgetDTO(
                periodBudget = 100.0,
                amountRemainingThisPeriod = 40.0,
                amountOverBudget = 0.0,
                isOverBudget = false,
                periodLabel = "Aug 1 - Aug 31",
                currencySymbol = "€"
            )
        )
        val referenceDate = LocalDate(2025, 8, 15)

        val result = repository(api).getPeriodBudget(referenceDate).getOrThrow()

        assertEquals("Aug 1 - Aug 31", result.periodLabel)
        assertEquals(referenceDate, api.lastReferenceDate)
    }

    @Test
    fun `an unsupported get_period_budget endpoint surfaces unchanged`() = runTest {
        val api = FakeDashboardApi(periodBudgetFailure = WallosError.UnsupportedEndpoint)

        assertFailsWith<WallosError.UnsupportedEndpoint> {
            repository(api).getPeriodBudget(referenceDate = null).getOrThrow()
        }
    }

    private fun repository(api: DashboardApi): DashboardRepositoryImpl = DashboardRepositoryImpl(
        api = api,
        monthlyCostMapper = MonthlyCostMapper(MoneyFormatter()),
        periodBudgetMapper = PeriodBudgetMapper(),
        dispatcher = UnconfinedTestDispatcher()
    )

    private class FakeDashboardApi(
        private val monthlyCost: MonthlyCostDTO? = null,
        private val monthlyCostFailure: Throwable? = null,
        private val periodBudget: PeriodBudgetDTO? = null,
        private val periodBudgetFailure: Throwable? = null
    ) : DashboardApi {

        var lastMonthlyCostMonth: Int? = null
            private set
        var lastMonthlyCostYear: Int? = null
            private set
        var lastReferenceDate: LocalDate? = null
            private set

        override suspend fun getMonthlyCost(month: Int, year: Int): MonthlyCostDTO {
            monthlyCostFailure?.let { throw it }
            lastMonthlyCostMonth = month
            lastMonthlyCostYear = year
            return monthlyCost ?: error("monthlyCost not set")
        }

        override suspend fun getPeriodBudget(referenceDate: LocalDate?): PeriodBudgetDTO {
            periodBudgetFailure?.let { throw it }
            lastReferenceDate = referenceDate
            return periodBudget ?: error("periodBudget not set")
        }
    }
}

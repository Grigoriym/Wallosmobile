package com.grappim.wallosmobile.feature.dashboard.data

import com.grappim.wallosmobile.core.api.FormParams
import com.grappim.wallosmobile.core.api.WallosApiClient
import com.grappim.wallosmobile.core.api.post
import com.grappim.wallosmobile.feature.dashboard.dto.MonthlyCostDTO
import com.grappim.wallosmobile.feature.dashboard.dto.PeriodBudgetDTO
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.Single

/** The two read endpoints Phase 4 needs (API doc §3.5–3.6), unwrapping the envelope. */
interface DashboardApi {

    suspend fun getMonthlyCost(month: Int, year: Int): MonthlyCostDTO

    suspend fun getPeriodBudget(referenceDate: LocalDate?): PeriodBudgetDTO
}

@Single(binds = [DashboardApi::class])
internal class DashboardApiImpl(private val apiClient: WallosApiClient) : DashboardApi {

    override suspend fun getMonthlyCost(month: Int, year: Int): MonthlyCostDTO = apiClient.post<MonthlyCostDTO>(
        MONTHLY_COST_PATH,
        FormParams().put(PARAM_MONTH, month.toString()).put(PARAM_YEAR, year.toString())
    )

    /** [referenceDate] omitted entirely rather than sent blank — the server's own default is "today". */
    override suspend fun getPeriodBudget(referenceDate: LocalDate?): PeriodBudgetDTO = apiClient.post<PeriodBudgetDTO>(
        PERIOD_BUDGET_PATH,
        FormParams().apply { referenceDate?.let { date(PARAM_REFERENCE_DATE, it) } }
    )

    private companion object {
        // Relative, no leading slash — a leading one discards the subpath of an install that
        // lives under e.g. `/wallos` (plan §4.1).
        const val MONTHLY_COST_PATH = "api/subscriptions/get_monthly_cost.php"
        const val PERIOD_BUDGET_PATH = "api/subscriptions/get_period_budget.php"

        const val PARAM_MONTH = "month"
        const val PARAM_YEAR = "year"
        const val PARAM_REFERENCE_DATE = "reference_date"
    }
}

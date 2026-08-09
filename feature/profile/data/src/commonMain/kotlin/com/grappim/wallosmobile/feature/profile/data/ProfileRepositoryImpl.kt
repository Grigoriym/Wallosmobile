package com.grappim.wallosmobile.feature.profile.data

import com.grappim.wallosmobile.core.api.FormParams
import com.grappim.wallosmobile.core.asynckmp.IoDispatcher
import com.grappim.wallosmobile.core.domain.resultOf
import com.grappim.wallosmobile.feature.profile.data.mapper.UserMapper
import com.grappim.wallosmobile.feature.profile.domain.model.BudgetPeriodType
import com.grappim.wallosmobile.feature.profile.domain.model.User
import com.grappim.wallosmobile.feature.profile.domain.repo.ProfileRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.Single

/** No cache behind the call (like `DashboardRepository`) — a single-row snapshot, not a list. */
@Single(binds = [ProfileRepository::class])
internal class ProfileRepositoryImpl(
    private val api: ProfileApi,
    private val userMapper: UserMapper,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher
) : ProfileRepository {

    override suspend fun getUser(): Result<User> = resultOf {
        withContext(dispatcher) { userMapper.toDomain(api.getUser()) }
    }

    override suspend fun setBudget(
        monthlyBudget: Double,
        periodBudget: Double,
        periodType: BudgetPeriodType,
        anchorDate: LocalDate
    ): Result<Unit> = resultOf {
        withContext(dispatcher) {
            api.setBudget(
                FormParams()
                    .put(PARAM_MONTHLY_BUDGET, monthlyBudget.toString())
                    .put(PARAM_PERIOD_BUDGET, periodBudget.toString())
                    .put(PARAM_BUDGET_PERIOD_TYPE, periodType.wireValue)
                    .date(PARAM_BUDGET_PERIOD_ANCHOR_DATE, anchorDate)
            )
        }
    }

    private companion object {
        const val PARAM_MONTHLY_BUDGET = "monthly_budget"
        const val PARAM_PERIOD_BUDGET = "period_budget"
        const val PARAM_BUDGET_PERIOD_TYPE = "budget_period_type"
        const val PARAM_BUDGET_PERIOD_ANCHOR_DATE = "budget_period_anchor_date"
    }
}

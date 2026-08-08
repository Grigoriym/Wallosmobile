package com.grappim.wallosmobile.feature.dashboard.data

import com.grappim.wallosmobile.core.asynckmp.IoDispatcher
import com.grappim.wallosmobile.core.domain.resultOf
import com.grappim.wallosmobile.feature.dashboard.data.mapper.MonthlyCostMapper
import com.grappim.wallosmobile.feature.dashboard.data.mapper.PeriodBudgetMapper
import com.grappim.wallosmobile.feature.dashboard.domain.model.MonthlyCost
import com.grappim.wallosmobile.feature.dashboard.domain.model.PeriodBudget
import com.grappim.wallosmobile.feature.dashboard.domain.repo.DashboardRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.Single

/**
 * No cache behind either call (unlike `SubscriptionsRepository`, 3.4) — these are period
 * snapshots, not a list to page through offline, so every call is a round trip to [DashboardApi].
 */
@Single(binds = [DashboardRepository::class])
internal class DashboardRepositoryImpl(
    private val api: DashboardApi,
    private val monthlyCostMapper: MonthlyCostMapper,
    private val periodBudgetMapper: PeriodBudgetMapper,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher
) : DashboardRepository {

    override suspend fun getMonthlyCost(month: Int, year: Int): Result<MonthlyCost> = resultOf {
        withContext(dispatcher) { monthlyCostMapper.toDomain(api.getMonthlyCost(month, year)) }
    }

    override suspend fun getPeriodBudget(referenceDate: LocalDate?): Result<PeriodBudget> = resultOf {
        withContext(dispatcher) { periodBudgetMapper.toDomain(api.getPeriodBudget(referenceDate)) }
    }
}

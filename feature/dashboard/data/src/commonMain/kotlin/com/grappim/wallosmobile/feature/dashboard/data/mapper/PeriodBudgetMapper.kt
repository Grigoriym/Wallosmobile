package com.grappim.wallosmobile.feature.dashboard.data.mapper

import com.grappim.wallosmobile.feature.dashboard.domain.model.PeriodBudget
import com.grappim.wallosmobile.feature.dashboard.dto.PeriodBudgetDTO
import org.koin.core.annotation.Single

/** A field-for-field trim of `get_period_budget.php` (API doc §3.6) — see [PeriodBudget]'s own doc. */
@Single
class PeriodBudgetMapper {

    fun toDomain(dto: PeriodBudgetDTO): PeriodBudget = PeriodBudget(
        periodLabel = dto.periodLabel,
        periodBudget = dto.periodBudget,
        amountRemainingThisPeriod = dto.amountRemainingThisPeriod,
        amountOverBudget = dto.amountOverBudget,
        isOverBudget = dto.isOverBudget,
        currencySymbol = dto.currencySymbol
    )
}

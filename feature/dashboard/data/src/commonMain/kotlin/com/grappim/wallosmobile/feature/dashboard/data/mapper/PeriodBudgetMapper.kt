package com.grappim.wallosmobile.feature.dashboard.data.mapper

import com.grappim.wallosmobile.feature.dashboard.domain.model.PeriodBudget
import com.grappim.wallosmobile.feature.dashboard.dto.PeriodBudgetDTO
import com.grappim.wallosmobile.utils.formatter.datetime.DateFormatter
import org.koin.core.annotation.Single

/** A field-for-field trim of `get_period_budget.php` (API doc §3.6) — see [PeriodBudget]'s own doc. */
@Single
class PeriodBudgetMapper(private val dateFormatter: DateFormatter) {

    fun toDomain(dto: PeriodBudgetDTO): PeriodBudget = PeriodBudget(
        periodLabel = dto.periodLabel,
        periodBudget = dto.periodBudget,
        amountRemainingThisPeriod = dto.amountRemainingThisPeriod,
        amountOverBudget = dto.amountOverBudget,
        isOverBudget = dto.isOverBudget,
        currencySymbol = dto.currencySymbol,
        // Unlike `start_date`/`next_payment`, the server always sends a well-formed date here —
        // there's no "unset" case for the active period's own bounds.
        periodStart = requireNotNull(dateFormatter.parseIsoDate(dto.periodStart)) {
            "get_period_budget.php returned an unparseable period_start: ${dto.periodStart}"
        },
        periodEnd = requireNotNull(dateFormatter.parseIsoDate(dto.periodEnd)) {
            "get_period_budget.php returned an unparseable period_end: ${dto.periodEnd}"
        }
    )
}

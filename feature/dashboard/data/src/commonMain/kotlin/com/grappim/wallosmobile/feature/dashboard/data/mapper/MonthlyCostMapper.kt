package com.grappim.wallosmobile.feature.dashboard.data.mapper

import com.grappim.wallosmobile.core.domain.WallosError
import com.grappim.wallosmobile.feature.dashboard.domain.model.MonthlyCost
import com.grappim.wallosmobile.feature.dashboard.dto.MonthlyCostDTO
import com.grappim.wallosmobile.utils.formatter.decimal.MoneyFormatter
import org.koin.core.annotation.Single

/**
 * `monthly_cost` is a comma-grouped string (API doc §3.5), not a JSON number — parsed here rather
 * than mapped straight across, so an unparseable value fails the call instead of silently
 * rendering as `0`.
 */
@Single
class MonthlyCostMapper(private val moneyFormatter: MoneyFormatter) {

    fun toDomain(dto: MonthlyCostDTO): MonthlyCost {
        val amount = moneyFormatter.parse(dto.monthlyCost) ?: throw WallosError.Malformed(dto.monthlyCost)
        return MonthlyCost(title = dto.title, amount = amount, currencySymbol = dto.currencySymbol)
    }
}

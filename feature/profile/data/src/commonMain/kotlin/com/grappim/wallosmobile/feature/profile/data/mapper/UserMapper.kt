package com.grappim.wallosmobile.feature.profile.data.mapper

import com.grappim.wallosmobile.feature.profile.domain.model.BudgetPeriodType
import com.grappim.wallosmobile.feature.profile.domain.model.User
import com.grappim.wallosmobile.feature.profile.dto.UserDTO
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.Single

/** A field-for-field trim of `get_user.php` (API doc §3.9) — see [User]'s own doc. */
@Single
class UserMapper {

    fun toDomain(dto: UserDTO): User = User(
        id = dto.id,
        username = dto.username,
        email = dto.email,
        budget = dto.budget,
        periodBudget = dto.periodBudget,
        mainCurrencyId = dto.mainCurrency,
        budgetPeriodType = BudgetPeriodType.fromWireValue(dto.budgetPeriodType),
        budgetPeriodAnchorDate = LocalDate.parse(dto.budgetPeriodAnchorDate),
        totpEnabled = dto.totpEnabled == TOTP_ENABLED
    )

    private companion object {
        const val TOTP_ENABLED = 1
    }
}

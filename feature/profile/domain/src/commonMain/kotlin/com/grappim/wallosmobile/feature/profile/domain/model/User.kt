package com.grappim.wallosmobile.feature.profile.domain.model

import kotlinx.datetime.LocalDate

/** `get_user.php`'s row (`docs/WALLOS_API.md` §3.9), trimmed to the fields 9.8/9.9 need. */
data class User(
    val id: Int,
    val username: String,
    val email: String,
    val budget: Double,
    val periodBudget: Double,
    val mainCurrencyId: Int,
    val budgetPeriodType: BudgetPeriodType,
    val budgetPeriodAnchorDate: LocalDate,
    val totpEnabled: Boolean
)

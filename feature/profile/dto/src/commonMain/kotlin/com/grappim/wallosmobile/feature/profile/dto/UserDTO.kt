package com.grappim.wallosmobile.feature.profile.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `get_user.php` (`docs/WALLOS_API.md` §3.9) — `password`/`api_key` are always masked and never
 * worth modeling; `avatar`, `language`, `firstname`, `lastname`, `oidc_sub` are on the wire but
 * unused by any screen this app has, so they stay unmodeled too (9.8's own scope, per the
 * checklist step's field list).
 */
@Serializable
data class UserDTO(
    val id: Int,
    val username: String,
    val email: String,
    val budget: Double,
    @SerialName("period_budget") val periodBudget: Double,
    @SerialName("main_currency") val mainCurrency: Int,
    @SerialName("budget_period_type") val budgetPeriodType: String,
    @SerialName("budget_period_anchor_date") val budgetPeriodAnchorDate: String,
    @SerialName("totp_enabled") val totpEnabled: Int
)

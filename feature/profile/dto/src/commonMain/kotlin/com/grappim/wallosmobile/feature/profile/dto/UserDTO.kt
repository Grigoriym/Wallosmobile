package com.grappim.wallosmobile.feature.profile.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `get_user.php` (`docs/WALLOS_API.md` §3.9), trimmed to what M10's dashboard card needs —
 * `password`/`api_key` are always masked and never worth modeling; `username`, `email` and the
 * rest are M9's 9.9 job, not this one (M10's own preamble).
 */
@Serializable
data class UserDTO(
    val id: Int,
    val budget: Double,
    @SerialName("period_budget") val periodBudget: Double,
    @SerialName("main_currency") val mainCurrency: Int
)

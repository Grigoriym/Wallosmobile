package com.grappim.wallosmobile.feature.dashboard.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `get_monthly_cost.php` (`docs/WALLOS_API.md` §3.5). `localized_monthly_cost` and
 * `currency_code` aren't modeled — `MoneyFormatter` formats [monthlyCost] with [currencySymbol]
 * the same way every other amount in the app is rendered, and nothing reads the currency code on
 * its own.
 *
 * @param monthlyCost a comma-grouped, always-two-decimals **string** (`number_format`), not a
 *   JSON number — parse with `MoneyFormatter.parse`, never `toDouble()`.
 */
@Serializable
data class MonthlyCostDTO(
    val title: String,
    @SerialName("monthly_cost") val monthlyCost: String,
    @SerialName("currency_symbol") val currencySymbol: String
)

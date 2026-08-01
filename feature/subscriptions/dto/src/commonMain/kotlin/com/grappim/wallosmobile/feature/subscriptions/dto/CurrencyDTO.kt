package com.grappim.wallosmobile.feature.subscriptions.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * An entry of `get_currencies.php` (API doc §3.10). The list is what turns a subscription's
 * `currency_id` into something a price can be rendered with (plan §7.1).
 *
 * [rate] is a **string** in the JSON (`"1"`), not a number, and [inUse] a real boolean — both
 * verified against the live instance. The response's top-level `main_currency` belongs to the
 * envelope, not here.
 */
@Serializable
data class CurrencyDTO(
    val id: Int,
    val name: String = "",
    val symbol: String = "",
    val code: String = "",
    val rate: String = "1",
    @SerialName("in_use") val inUse: Boolean = false
)

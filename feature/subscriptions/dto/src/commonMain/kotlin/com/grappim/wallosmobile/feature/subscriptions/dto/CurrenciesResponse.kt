package com.grappim.wallosmobile.feature.subscriptions.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `get_currencies.php` (API doc §3.10).
 *
 * @param mainCurrency the id conversion converts *into*, and the only way to tell which of
 *   [currencies] a converted price is denominated in — the subscription rows never say (3.11).
 *   Nullable rather than required: it is the one field here an older instance might not send, and
 *   its absence only costs conversion, not the list.
 */
@Serializable
data class CurrenciesResponse(
    val currencies: List<CurrencyDTO>,
    @SerialName("main_currency") val mainCurrency: Int? = null
)

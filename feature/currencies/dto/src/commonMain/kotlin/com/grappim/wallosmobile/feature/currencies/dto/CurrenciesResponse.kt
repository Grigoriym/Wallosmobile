package com.grappim.wallosmobile.feature.currencies.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `get_currencies.php`'s full envelope (`docs/WALLOS_API.md` §3.10), decoded straight into this
 * shape rather than through the generic `WallosCrudApi<CurrencyDTO>.getAll()`: that call only ever
 * reads `envelope[listKey]`, dropping the top-level [mainCurrency] this screen needs to mark which
 * currency is main and to disable Delete on it (9.1's milestone preamble).
 */
@Serializable
data class CurrenciesResponse(
    val currencies: List<CurrencyDTO>,
    @SerialName("main_currency") val mainCurrency: Int? = null
)

package com.grappim.wallosmobile.feature.subscriptions.dto

import kotlinx.serialization.Serializable

/**
 * `get_currencies.php` (API doc §3.10). The envelope also carries a top-level `main_currency`,
 * which nothing in v1 reads — it exists to detect the silent
 * `convert_currency`-without-exchange-rates case, and conversion is Phase 2b.
 */
@Serializable
data class CurrenciesResponse(val currencies: List<CurrencyDTO>)

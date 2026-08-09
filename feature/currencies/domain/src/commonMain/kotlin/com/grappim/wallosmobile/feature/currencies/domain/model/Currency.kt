package com.grappim.wallosmobile.feature.currencies.domain.model

/**
 * A currency the instance knows about, with [rate] and [inUse] both carried — unlike
 * [com.grappim.wallosmobile.feature.subscriptions.domain.model.Currency]'s trimmed copy for the
 * price picker, this is the CRUD screen for currencies itself, so both matter. [isMain] is derived
 * from `get_currencies.php`'s top-level `main_currency`, not a per-row field on the wire.
 */
data class Currency(
    val id: Int,
    val name: String,
    val symbol: String,
    val code: String,
    val rate: Double,
    val inUse: Boolean,
    val isMain: Boolean
)

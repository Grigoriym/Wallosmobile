package com.grappim.wallosmobile.feature.subscriptions.mapper

import com.grappim.wallosmobile.feature.subscriptions.domain.model.Currency
import com.grappim.wallosmobile.feature.subscriptions.dto.CurrencyDTO
import org.koin.core.annotation.Single

/**
 * A `currencies` row into the model. `rate` and `in_use` are dropped rather than carried:
 * conversion is Phase 2b and the in-use flag only guards deletes (2.1).
 */
@Single
class CurrencyMapper(private val htmlUnescaper: HtmlUnescaper) {

    /**
     * [CurrencyDTO.name] and [CurrencyDTO.symbol] are user-editable in Wallos and go through the
     * same `htmlspecialchars` as everything else, so a symbol like `&` needs the same laundering
     * as a subscription name.
     */
    fun toDomain(dto: CurrencyDTO): Currency = Currency(
        id = dto.id,
        name = htmlUnescaper.unescape(dto.name),
        symbol = htmlUnescaper.unescape(dto.symbol),
        code = dto.code
    )
}

package com.grappim.wallosmobile.feature.currencies.mapper

import com.grappim.wallosmobile.feature.currencies.domain.model.Currency
import com.grappim.wallosmobile.feature.currencies.dto.CurrencyDTO
import com.grappim.wallosmobile.feature.subscriptions.mapper.HtmlUnescaper
import org.koin.core.annotation.Single

/**
 * A `currencies` row into the model. [rate] round-trips as a string on the wire and a [Double]
 * here — [DEFAULT_RATE] only covers a row somehow missing the field entirely, not a real value,
 * since every row checked live carries one.
 */
@Single
class CurrencyMapper(private val htmlUnescaper: HtmlUnescaper) {

    /** @param mainCurrencyId `get_currencies.php`'s top-level `main_currency`, envelope-only. */
    fun toDomain(dto: CurrencyDTO, mainCurrencyId: Int?): Currency = Currency(
        id = dto.id,
        name = htmlUnescaper.unescape(dto.name),
        symbol = htmlUnescaper.unescape(dto.symbol),
        code = dto.code,
        rate = dto.rate.toDoubleOrNull() ?: DEFAULT_RATE,
        inUse = dto.inUse,
        isMain = dto.id == mainCurrencyId
    )

    private companion object {
        const val DEFAULT_RATE = 1.0
    }
}

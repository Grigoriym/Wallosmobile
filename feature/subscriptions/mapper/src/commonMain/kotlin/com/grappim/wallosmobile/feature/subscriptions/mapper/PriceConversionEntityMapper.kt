package com.grappim.wallosmobile.feature.subscriptions.mapper

import com.grappim.wallosmobile.core.storage.db.PriceConversionEntity
import com.grappim.wallosmobile.feature.subscriptions.domain.model.PriceConversion
import org.koin.core.annotation.Single

/**
 * The cached conversion state both ways (3.11). Every field is already a SQLite primitive, so like
 * [CurrencyEntityMapper] it exists to keep the cache from naming a domain type.
 *
 * [toDomain] takes a nullable entity because the row is absent until the first refresh, and
 * "never refreshed" and "refreshed, nothing converted" are the same thing to a reader.
 */
@Single
class PriceConversionEntityMapper {

    fun toEntity(conversion: PriceConversion): PriceConversionEntity = PriceConversionEntity(
        isEnabled = conversion.isEnabled,
        mainCurrencyId = conversion.mainCurrencyId,
        hasRates = conversion.hasRates
    )

    fun toDomain(entity: PriceConversionEntity?): PriceConversion = if (entity == null) {
        PriceConversion()
    } else {
        PriceConversion(
            isEnabled = entity.isEnabled,
            mainCurrencyId = entity.mainCurrencyId,
            hasRates = entity.hasRates
        )
    }
}

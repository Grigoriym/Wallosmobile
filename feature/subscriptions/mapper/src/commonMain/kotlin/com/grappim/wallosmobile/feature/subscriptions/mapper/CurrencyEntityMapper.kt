package com.grappim.wallosmobile.feature.subscriptions.mapper

import com.grappim.wallosmobile.core.storage.db.CurrencyEntity
import com.grappim.wallosmobile.feature.subscriptions.domain.model.Currency
import org.koin.core.annotation.Single

/**
 * The cached currency both ways (3.4). Every field is already a SQLite primitive, so unlike
 * [SubscriptionEntityMapper] there is nothing to convert — it exists so that the cache never
 * names a domain type and the repository never names an entity.
 */
@Single
class CurrencyEntityMapper {

    fun toEntity(currency: Currency): CurrencyEntity = CurrencyEntity(
        id = currency.id,
        name = currency.name,
        symbol = currency.symbol,
        code = currency.code
    )

    fun toDomain(entity: CurrencyEntity): Currency = Currency(
        id = entity.id,
        name = entity.name,
        symbol = entity.symbol,
        code = entity.code
    )
}

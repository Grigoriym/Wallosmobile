package com.grappim.wallosmobile.feature.subscriptions.data

import com.grappim.wallosmobile.core.storage.db.CurrencyDao
import com.grappim.wallosmobile.core.storage.db.PriceConversionDao
import com.grappim.wallosmobile.core.storage.db.SubscriptionDao
import com.grappim.wallosmobile.feature.subscriptions.domain.model.Currency
import com.grappim.wallosmobile.feature.subscriptions.domain.model.PriceConversion
import com.grappim.wallosmobile.feature.subscriptions.domain.model.Subscription
import com.grappim.wallosmobile.feature.subscriptions.mapper.CurrencyEntityMapper
import com.grappim.wallosmobile.feature.subscriptions.mapper.PriceConversionEntityMapper
import com.grappim.wallosmobile.feature.subscriptions.mapper.SubscriptionEntityMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

/**
 * The database half of [SubscriptionsRepositoryImpl]: the two DAOs and the two entity mappers
 * behind one seam that speaks domain models only.
 *
 * It is a class rather than four more constructor parameters on the repository because that is
 * genuinely one job — everything about the cache being *rows* stops here, and the repository is
 * left holding the network and the order things happen in.
 *
 * Nothing here fails in a way a caller can act on: a DAO throwing is a broken install, not an
 * outcome, and `resultOf` in the repository catches it either way.
 */
@Single
internal class SubscriptionsCache(
    private val subscriptionDao: SubscriptionDao,
    private val currencyDao: CurrencyDao,
    private val priceConversionDao: PriceConversionDao,
    private val subscriptionEntityMapper: SubscriptionEntityMapper,
    private val currencyEntityMapper: CurrencyEntityMapper,
    private val priceConversionEntityMapper: PriceConversionEntityMapper
) {

    fun observeSubscriptions(): Flow<List<Subscription>> = subscriptionDao.observeAll()
        .map { rows -> rows.map(subscriptionEntityMapper::toDomain) }

    fun observeSubscription(id: Int): Flow<Subscription?> = subscriptionDao.observeById(id)
        .map { row -> row?.let(subscriptionEntityMapper::toDomain) }

    /** Snapshot, never a merge: a row the fresh list doesn't carry has been deleted server-side. */
    suspend fun replaceSubscriptions(subscriptions: List<Subscription>) {
        subscriptionDao.replaceAll(subscriptions.map(subscriptionEntityMapper::toEntity))
    }

    /** One row, from a detail refresh — an insert with `REPLACE`, so it upserts. */
    suspend fun putSubscription(subscription: Subscription) {
        subscriptionDao.insertAll(listOf(subscriptionEntityMapper.toEntity(subscription)))
    }

    suspend fun replaceCurrencies(currencies: List<Currency>) {
        currencyDao.replaceAll(currencies.map(currencyEntityMapper::toEntity))
    }

    /** The resolution table a price needs, empty until a list refresh has filled the table. */
    suspend fun currencySymbols(): Map<Int, String> =
        currencyDao.getAll().map(currencyEntityMapper::toDomain).associate { it.id to it.symbol }

    fun observeCurrencies(): Flow<List<Currency>> = currencyDao.observeAll()
        .map { rows -> rows.map(currencyEntityMapper::toDomain) }

    fun observePriceConversion(): Flow<PriceConversion> = priceConversionDao.observe()
        .map(priceConversionEntityMapper::toDomain)

    /** The default until a refresh has written one — nothing converted (3.11). */
    suspend fun priceConversion(): PriceConversion = priceConversionEntityMapper.toDomain(priceConversionDao.get())

    suspend fun putPriceConversion(conversion: PriceConversion) {
        priceConversionDao.put(priceConversionEntityMapper.toEntity(conversion))
    }
}

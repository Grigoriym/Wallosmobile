package com.grappim.wallosmobile.feature.subscriptions.data

import com.grappim.wallosmobile.core.asynckmp.IoDispatcher
import com.grappim.wallosmobile.core.domain.resultOf
import com.grappim.wallosmobile.feature.subscriptions.domain.model.Currency
import com.grappim.wallosmobile.feature.subscriptions.domain.model.Subscription
import com.grappim.wallosmobile.feature.subscriptions.domain.repo.SubscriptionsRepository
import com.grappim.wallosmobile.feature.subscriptions.mapper.CurrencyMapper
import com.grappim.wallosmobile.feature.subscriptions.mapper.SubscriptionMapper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

/**
 * Offline-first (3.4): reads come off the cache, the network only writes to it.
 *
 * The consequence worth stating is what a failure now means. Before the cache, a failed load left
 * the screen with nothing and so it cleared the list; now the last successful snapshot is still
 * there and still true as of when it was taken, so a refresh that fails changes **nothing** —
 * the error travels on its own and the rows stay.
 */
@Single(binds = [SubscriptionsRepository::class])
internal class SubscriptionsRepositoryImpl(
    private val api: SubscriptionsApi,
    private val cache: SubscriptionsCache,
    private val subscriptionMapper: SubscriptionMapper,
    private val currencyMapper: CurrencyMapper,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher
) : SubscriptionsRepository {

    override fun observeSubscriptions(): Flow<List<Subscription>> = cache.observeSubscriptions().flowOn(dispatcher)

    override fun observeSubscription(id: Int): Flow<Subscription?> = cache.observeSubscription(id).flowOn(dispatcher)

    /**
     * The full sync, and the only thing that fills the currency table: subscriptions first, so a
     * failure on the resource the user actually asked for short-circuits before the second round
     * trip (plan §7.1). Both tables are written only once both calls have answered — a half-
     * written cache would be worse than the stale one it replaced.
     */
    override suspend fun refreshSubscriptions(): Result<Unit> = resultOf {
        withContext(dispatcher) {
            val dtos = api.getSubscriptions()
            val currencies = fetchCurrencies()
            val symbols = currencies.associate { it.id to it.symbol }

            cache.replaceCurrencies(currencies)
            cache.replaceSubscriptions(dtos.map { subscriptionMapper.toDomain(it, symbols.symbolFor(it.currencyId)) })
        }
    }

    /**
     * One round trip where 2.5 needed two: the symbol comes from the currency table the last list
     * refresh cached. The fallback covers the one case where it can't — a cache emptied by a
     * disconnect, then a detail opened before any list refresh has succeeded.
     */
    override suspend fun refreshSubscription(id: Int): Result<Unit> = resultOf {
        withContext(dispatcher) {
            val dto = api.getSubscription(id)
            val symbols = cache.currencySymbols().ifEmpty {
                val currencies = fetchCurrencies()
                cache.replaceCurrencies(currencies)
                currencies.associate { it.id to it.symbol }
            }

            cache.putSubscription(subscriptionMapper.toDomain(dto, symbols.symbolFor(dto.currencyId)))
        }
    }

    private suspend fun fetchCurrencies(): List<Currency> = api.getCurrencies().map(currencyMapper::toDomain)

    /**
     * Blank for a `currency_id` the instance's own currency list doesn't contain — deleting a
     * currency that a subscription still references shouldn't cost the user the whole screen, and
     * a bare number is a better answer than someone else's sign.
     */
    private fun Map<Int, String>.symbolFor(currencyId: Int): String = this[currencyId].orEmpty()
}

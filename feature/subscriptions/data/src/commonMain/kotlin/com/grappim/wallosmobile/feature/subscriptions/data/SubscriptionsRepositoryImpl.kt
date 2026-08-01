package com.grappim.wallosmobile.feature.subscriptions.data

import com.grappim.wallosmobile.core.asynckmp.IoDispatcher
import com.grappim.wallosmobile.core.domain.resultOf
import com.grappim.wallosmobile.feature.subscriptions.domain.model.Subscription
import com.grappim.wallosmobile.feature.subscriptions.domain.repo.SubscriptionsRepository
import com.grappim.wallosmobile.feature.subscriptions.mapper.CurrencyMapper
import com.grappim.wallosmobile.feature.subscriptions.mapper.SubscriptionMapper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

@Single(binds = [SubscriptionsRepository::class])
internal class SubscriptionsRepositoryImpl(
    private val api: SubscriptionsApi,
    private val subscriptionMapper: SubscriptionMapper,
    private val currencyMapper: CurrencyMapper,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher
) : SubscriptionsRepository {

    override suspend fun getSubscriptions(): Result<List<Subscription>> = resultOf {
        withContext(dispatcher) {
            val subscriptions = api.getSubscriptions()
            val symbols = currencySymbols()
            subscriptions.map { subscriptionMapper.toDomain(it, symbols.symbolFor(it.currencyId)) }
        }
    }

    override suspend fun getSubscription(id: Int): Result<Subscription> = resultOf {
        withContext(dispatcher) {
            val dto = api.getSubscription(id)
            val symbols = currencySymbols()
            subscriptionMapper.toDomain(dto, symbols.symbolFor(dto.currencyId))
        }
    }

    /**
     * The join plan §7.1 calls the list's non-obvious dependency: `price` arrives with a
     * `currency_id` and the symbol lives in a different endpoint, so every screen that renders a
     * price is two calls. Fetched after the subscriptions, so a failure on the resource the user
     * actually asked for short-circuits before the second round trip.
     *
     * No cache: v1 fetches on demand (plan §7.2), which means this list is re-read per call.
     */
    private suspend fun currencySymbols(): Map<Int, String> =
        api.getCurrencies().map(currencyMapper::toDomain).associate { it.id to it.symbol }

    /**
     * Blank for a `currency_id` the instance's own currency list doesn't contain — deleting a
     * currency that a subscription still references shouldn't cost the user the whole screen, and
     * a bare number is a better answer than someone else's sign.
     */
    private fun Map<Int, String>.symbolFor(currencyId: Int): String = this[currencyId].orEmpty()
}

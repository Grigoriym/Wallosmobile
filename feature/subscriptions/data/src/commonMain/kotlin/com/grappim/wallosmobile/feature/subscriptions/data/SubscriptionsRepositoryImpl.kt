package com.grappim.wallosmobile.feature.subscriptions.data

import com.grappim.wallosmobile.core.asynckmp.IoDispatcher
import com.grappim.wallosmobile.core.domain.resultOf
import com.grappim.wallosmobile.core.logger.LogPriority
import com.grappim.wallosmobile.core.logger.logcat
import com.grappim.wallosmobile.feature.subscriptions.domain.model.PriceConversion
import com.grappim.wallosmobile.feature.subscriptions.domain.model.Subscription
import com.grappim.wallosmobile.feature.subscriptions.domain.repo.SubscriptionsRepository
import com.grappim.wallosmobile.feature.subscriptions.dto.CurrencyDTO
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
 *
 * Since 3.11 it also decides what a cached price *means*. `convert_currency` is sent when the
 * instance's own setting asks for it, and the server then rewrites `price` while leaving
 * `currency_id` pointing at the currency it converted from — so the symbol a row is stored with is
 * this class's decision, not the response's.
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

    override fun observePriceConversion(): Flow<PriceConversion> = cache.observePriceConversion().flowOn(dispatcher)

    /**
     * The full sync, and the only thing that fills the currency table: subscriptions first, so a
     * failure on the resource the user actually asked for short-circuits before the second round
     * trip (plan §7.1). All three tables are written only once both calls have answered — a half-
     * written cache would be worse than the stale one it replaced.
     *
     * The settings read comes ahead of both because the flag shapes the subscriptions request, but
     * it cannot fail the refresh — see [isConversionEnabled], which is why "subscriptions first"
     * still describes the failure behaviour.
     */
    override suspend fun refreshSubscriptions(): Result<Unit> = resultOf {
        withContext(dispatcher) {
            val isEnabled = isConversionEnabled()
            val dtos = api.getSubscriptions(convertCurrency = isEnabled)
            val payload = api.getCurrencies()
            val currencies = payload.currencies.map(currencyMapper::toDomain)
            val conversion = PriceConversion(
                isEnabled = isEnabled,
                mainCurrencyId = payload.mainCurrencyId,
                hasRates = payload.currencies.haveRates()
            )
            val symbols = currencies.associate { it.id to it.symbol }

            cache.replaceCurrencies(currencies)
            cache.putPriceConversion(conversion)
            cache.replaceSubscriptions(
                dtos.map { subscriptionMapper.toDomain(it, symbols.symbolFor(it.currencyId, conversion)) }
            )
        }
    }

    /**
     * One round trip where 2.5 needed two: the symbol comes from the currency table the last list
     * refresh cached, and the conversion state from the row beside it — the same flag the list was
     * fetched with, so one row cannot disagree with the list it sits in.
     *
     * A cache with neither is a detail opened before any list refresh has succeeded. It fetches the
     * currencies it needs and converts nothing: the conversion default is "off", which is the one
     * answer that cannot label a price with the wrong currency.
     */
    override suspend fun refreshSubscription(id: Int): Result<Unit> = resultOf {
        withContext(dispatcher) {
            val conversion = cache.priceConversion()
            val dto = api.getSubscription(id, convertCurrency = conversion.isEnabled)
            val symbols = cache.currencySymbols().ifEmpty {
                val payload = api.getCurrencies()
                val currencies = payload.currencies.map(currencyMapper::toDomain)
                cache.replaceCurrencies(currencies)
                currencies.associate { it.id to it.symbol }
            }

            cache.putSubscription(
                subscriptionMapper.toDomain(dto, symbols.symbolFor(dto.currencyId, conversion))
            )
        }
    }

    /**
     * A preference, not data: an instance too old to have `api/settings/get_settings.php` answers
     * 404, and that must not cost the user their list. So this degrades to "don't convert" — which
     * is also the reading that cannot mislabel a price — and says so in the log rather than
     * swallowing it.
     */
    private suspend fun isConversionEnabled(): Boolean = resultOf { api.isCurrencyConversionEnabled() }
        .getOrElse { throwable ->
            logcat(priority = LogPriority.WARN, throwable = throwable) {
                "Reading convert_currency failed; treating conversion as off"
            }
            false
        }

    /**
     * Whether the instance has ever fetched exchange rates, which is what Wallos silently gates
     * conversion on (API doc §5.5). The flag it really reads is a `last_exchange_update` row that
     * no endpoint exposes, so the rate table stands in for it: a fresh install seeds **every** rate
     * at exactly `1` and only an exchange update writes both the rates and that row, so a rate that
     * isn't 1 is the one observable proof an update has run.
     *
     * Where the two could disagree they cost nothing: a genuine rate of `1` divides the price by 1,
     * so the amount is the same in either currency and either label is true of it.
     *
     * A rate that doesn't parse is not evidence of anything and is ignored rather than counted.
     */
    private fun List<CurrencyDTO>.haveRates(): Boolean = any { dto ->
        val rate = dto.rate.toDoubleOrNull()
        rate != null && rate != NO_RATE
    }

    /**
     * The symbol the price in hand is actually denominated in. A converted row carries the amount
     * in the main currency and the `currency_id` of the one it was converted *from* (3.11), so
     * taking the row's own symbol would put the wrong sign in front of a real number — the one
     * failure mode worth more than the conversion itself.
     *
     * Blank for a `currency_id` the instance's own currency list doesn't contain — deleting a
     * currency that a subscription still references shouldn't cost the user the whole screen, and
     * a bare number is a better answer than someone else's sign.
     */
    private fun Map<Int, String>.symbolFor(currencyId: Int, conversion: PriceConversion): String {
        val denominatedIn = if (conversion.converts(currencyId)) conversion.mainCurrencyId else currencyId
        return this[denominatedIn].orEmpty()
    }

    private companion object {
        const val NO_RATE = 1.0
    }
}

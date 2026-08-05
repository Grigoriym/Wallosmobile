package com.grappim.wallosmobile.feature.subscriptions.domain.repo

import com.grappim.wallosmobile.feature.subscriptions.domain.model.Currency
import com.grappim.wallosmobile.feature.subscriptions.domain.model.PriceConversion
import com.grappim.wallosmobile.feature.subscriptions.domain.model.Subscription
import kotlinx.coroutines.flow.Flow

/**
 * Subscriptions, already joined to the currency symbol their price needs (plan §7.1).
 *
 * **Offline-first since 3.4**: the cache is the only thing anyone reads, and the network only ever
 * writes to it. So the two halves are split — `observe*` is the data, always available and never
 * failing, and `refresh*` is the round trip, which either updates the cache or fails leaving the
 * last known rows exactly where they were. A caller that wants both does both.
 *
 * A [Result] failure is always a
 * [com.grappim.wallosmobile.core.domain.WallosError] — everything leaving `core:api` is one.
 */
interface SubscriptionsRepository {

    /** The cached list, re-emitting on every refresh that changes it. Empty until one succeeds. */
    fun observeSubscriptions(): Flow<List<Subscription>>

    /**
     * What the cached prices are denominated in (3.11) — cached alongside them, because a response
     * cannot be read back for it and a cold offline start has no response at all. Every field is
     * `false`/`null` until a refresh has answered, which reads as "these are the instance's own
     * currencies" and is the assumption that cannot mislabel a price.
     */
    fun observePriceConversion(): Flow<PriceConversion>

    /**
     * The instance's currency list, cached by the same refresh (2.3). A price already carries the
     * symbol it is rendered with, so this is for the *other* question a converted row raises —
     * which currency the amount was converted **from**, which only an id and this table can name
     * (5.4). Empty until a refresh has answered.
     */
    fun observeCurrencies(): Flow<List<Currency>>

    /** Replaces the cached list with the instance's, wholesale — a row it no longer sends is gone. */
    suspend fun refreshSubscriptions(): Result<Unit>

    /** The cached row, or `null` while the cache has never seen [id]. */
    fun observeSubscription(id: Int): Flow<Subscription?>

    /**
     * Re-reads one row. One round trip, not two: the symbol comes from the cached currency list
     * that [refreshSubscriptions] left behind (plan §4.7).
     *
     * Wallos scopes this to the caller's own subscriptions, so an id belonging to someone else is
     * a `NotFound`-style failure. It leaves the cached row alone — that answer is ambiguous
     * enough (API doc §3.3) that it must not be read as "deleted".
     */
    suspend fun refreshSubscription(id: Int): Result<Unit>
}

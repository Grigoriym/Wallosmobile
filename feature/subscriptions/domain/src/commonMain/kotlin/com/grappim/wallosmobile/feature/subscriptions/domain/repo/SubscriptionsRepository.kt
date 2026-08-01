package com.grappim.wallosmobile.feature.subscriptions.domain.repo

import com.grappim.wallosmobile.feature.subscriptions.domain.model.Subscription

/**
 * Reads subscriptions off the instance, already joined to the currency symbol their price needs
 * (plan §7.1). Fetch-on-demand: there is no cache in v1, so every call is a live round trip and
 * the screen owns whatever it decides to keep.
 *
 * A [Result] failure is always a
 * [com.grappim.wallosmobile.core.domain.WallosError] — everything leaving `core:api` is one.
 */
interface SubscriptionsRepository {

    suspend fun getSubscriptions(): Result<List<Subscription>>

    /**
     * A single row by [id]. Wallos scopes this to the caller's own subscriptions, so an id
     * belonging to someone else is a `NotFound`-style failure, not an empty success.
     */
    suspend fun getSubscription(id: Int): Result<Subscription>
}

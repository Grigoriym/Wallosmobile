package com.grappim.wallosmobile.core.storage.subscriptions

import kotlinx.coroutines.flow.Flow

/**
 * The subscriptions list's filter and sort, kept across launches. `core:storage` cannot name the
 * list's own types (they live in `feature:subscriptions:ui`, a layer above), so the value is the
 * encoded form that module writes and reads back; its shape is that module's business.
 *
 * Account state, not device state: the payers, categories and payment methods it names belong to
 * one instance's data, so `ApiKeyStorage.clear()` drops it along with the cache.
 */
interface SubscriptionCriteriaStorage {

    /** `null` until something has been stored, and again after a disconnect. */
    val criteria: Flow<String?>

    suspend fun setCriteria(value: String)
}

package com.grappim.wallosmobile.feature.subscriptions.ui.list

import com.grappim.wallosmobile.feature.subscriptions.domain.model.Subscription
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

/** API doc §3.2's `state`: `0` is active, `1` is inactive, and an empty value means both. */
enum class SubscriptionStatusFilter {
    ALL,
    ACTIVE,
    INACTIVE;

    fun matches(isActive: Boolean): Boolean = when (this) {
        ALL -> true
        ACTIVE -> isActive
        INACTIVE -> !isActive
    }
}

/**
 * What the user picked in the filter sheet, and — since this is all client-side over the cache —
 * the predicate itself. The three multi-selects mirror §3.2's comma-separated `member`, `category`
 * and `payment`, except that they hold the **resolved names** the rows carry (2.1) rather than ids:
 * the option sets are built from the rows on screen, so no catalog endpoint is needed.
 *
 * An empty set means "every one of them", exactly as an omitted parameter does server-side.
 */
data class SubscriptionFilter(
    val payers: ImmutableSet<String> = persistentSetOf(),
    val categories: ImmutableSet<String> = persistentSetOf(),
    val paymentMethods: ImmutableSet<String> = persistentSetOf(),
    val status: SubscriptionStatusFilter = SubscriptionStatusFilter.ALL
) {

    /** Whether anything is narrowed at all — the difference between an empty list and no matches. */
    val isActive: Boolean
        get() = payers.isNotEmpty() ||
            categories.isNotEmpty() ||
            paymentMethods.isNotEmpty() ||
            status != SubscriptionStatusFilter.ALL

    fun matches(subscription: Subscription): Boolean = payers.allows(subscription.payerName) &&
        categories.allows(subscription.categoryName) &&
        paymentMethods.allows(subscription.paymentMethodName) &&
        status.matches(subscription.isActive)

    private fun Set<String>.allows(value: String): Boolean = isEmpty() || value in this
}

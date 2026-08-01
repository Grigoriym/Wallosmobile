package com.grappim.wallosmobile.feature.subscriptions.ui.list

import com.grappim.wallosmobile.feature.subscriptions.domain.model.BillingCycle
import com.grappim.wallosmobile.utils.ui.NativeText
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * One card. Everything a resource loader isn't needed for is already a string: the ViewModel owns
 * the money and date formatting, so the composable only resolves what needs the plural rules.
 *
 * @param logoUrl the full URL, or blank when the row has no logo *or* no server URL is known —
 *   the card then draws its own placeholder instead of asking Coil to load nothing.
 * @param cycle kept as the enum rather than as text: "every 6 months" is a plural, and a plural
 *   can only be resolved where `pluralStringResource` can be called. `null` when the instance
 *   sent a code this build doesn't know, and the card then shows no cycle line at all.
 */
data class SubscriptionUiItem(
    val id: Int,
    val name: String,
    val logoUrl: String,
    val price: String,
    val nextPayment: String,
    val cycle: BillingCycle?,
    val frequency: Int,
    val isActive: Boolean
)

/**
 * [isLoading] is the first load and owns the whole screen; [isRefreshing] is the pull-to-refresh
 * gesture and leaves the list on screen under the indicator.
 *
 * A failure clears [items]: with no cache there is nothing behind the error worth keeping, and a
 * stale list under a "couldn't reach the server" message is the shape that lies to the user.
 */
data class SubscriptionsUiState(
    val items: ImmutableList<SubscriptionUiItem> = persistentListOf(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: NativeText = NativeText.Empty,
    val onRefresh: () -> Unit = {},
    val onRetryClick: () -> Unit = {}
) {

    /** Empty is a state of its own, not "no items" — a load in flight and a failure are not it. */
    val isEmpty: Boolean
        get() = items.isEmpty() && !isLoading && error.isEmpty()
}

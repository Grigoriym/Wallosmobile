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
 * [isLoading] is the first load *into an empty cache* and owns the whole screen; [isRefreshing] is
 * the pull-to-refresh gesture and leaves the list on screen under the indicator.
 *
 * A failure no longer clears [items] (3.4): the cached rows behind the error are real and are
 * still true as of the last refresh that worked. [isStale] and [isFailed] split what the error then
 * means on screen (3.5) — a banner over the rows, or the whole screen when there are none.
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

    /** Rows the last refresh couldn't confirm: a banner over them, never instead of them. */
    val isStale: Boolean
        get() = error.isNotEmpty() && items.isNotEmpty()

    /** The error owns the screen only when there is nothing behind it left to show. */
    val isFailed: Boolean
        get() = error.isNotEmpty() && items.isEmpty()
}

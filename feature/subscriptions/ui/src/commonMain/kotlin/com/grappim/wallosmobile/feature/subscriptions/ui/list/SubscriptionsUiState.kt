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
 * The filter sheet (3.6). The option sets are the distinct values of the rows *the cache holds*,
 * not of [SubscriptionsUiState.items] — narrowing to one category must not take the others out of
 * the sheet the user narrowed them with.
 */
data class SubscriptionsFilterUiState(
    val filter: SubscriptionFilter = SubscriptionFilter(),
    val sort: SubscriptionSort = SubscriptionSort.NEXT_PAYMENT,
    val payers: ImmutableList<String> = persistentListOf(),
    val categories: ImmutableList<String> = persistentListOf(),
    val paymentMethods: ImmutableList<String> = persistentListOf(),
    val isVisible: Boolean = false,
    val onFilterChange: (SubscriptionFilter) -> Unit = {},
    val onSortChange: (SubscriptionSort) -> Unit = {},
    val onOpen: () -> Unit = {},
    val onDismiss: () -> Unit = {},
    val onClear: () -> Unit = {}
)

/**
 * [isLoading] is the first load *into an empty cache* and owns the whole screen; [isRefreshing] is
 * the pull-to-refresh gesture and leaves the list on screen under the indicator.
 *
 * A failure no longer clears [items] (3.4): the cached rows behind the error are real and are
 * still true as of the last refresh that worked. [isStale] and [isFailed] split what the error then
 * means on screen (3.5) — a banner over the rows, or the whole screen when there are none.
 *
 * @param items what the list draws: the cached rows **after** [filters] has narrowed and ordered
 *   them (3.6). So an empty [items] no longer means an empty cache, which is what [hasCachedRows]
 *   is for — every state below that used to ask "are there rows?" has to ask it of the cache, or a
 *   filter matching nothing reads as a server that answered with nothing.
 * @param isConversionUnavailable the silent failure 3.11 makes visible: the instance is set to show
 *   prices in one currency, its exchange rates have never been fetched, and the rows on screen are
 *   in more than one currency as a result. A stored field for the same reason [hasCachedRows] is —
 *   [items] carries `price` as already-formatted text, so nothing derivable from this state can
 *   count the currencies in it. It is about the *visible* rows, so a filter narrowed to one
 *   currency puts it away: what it warns about is comparing them.
 */
data class SubscriptionsUiState(
    val items: ImmutableList<SubscriptionUiItem> = persistentListOf(),
    val hasCachedRows: Boolean = false,
    val isConversionUnavailable: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: NativeText = NativeText.Empty,
    val filters: SubscriptionsFilterUiState = SubscriptionsFilterUiState(),
    val onRefresh: () -> Unit = {},
    val onRetryClick: () -> Unit = {}
) {

    /** Empty is a state of its own, not "no items" — a load in flight and a failure are not it. */
    val isEmpty: Boolean
        get() = !hasCachedRows && !isLoading && error.isEmpty()

    /** The other empty screen: rows exist, the filter excluded all of them, and that is undoable. */
    val isNoMatch: Boolean
        get() = hasCachedRows && items.isEmpty() && !isLoading

    /** Rows the last refresh couldn't confirm: a banner over them, never instead of them. */
    val isStale: Boolean
        get() = error.isNotEmpty() && hasCachedRows

    /** The error owns the screen only when there is nothing behind it left to show. */
    val isFailed: Boolean
        get() = error.isNotEmpty() && !hasCachedRows
}

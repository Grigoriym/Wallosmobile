package com.grappim.wallosmobile.feature.subscriptions.ui.list

import com.grappim.wallosmobile.feature.subscriptions.domain.model.Subscription
import org.koin.core.annotation.Single

/**
 * API doc §3.2's sort table, client-side: **the direction is fixed by the field**, not chosen by
 * the user — `price` and `id` descend, everything else ascends. Copying that here is the point of
 * the class: it is a table, and a table is worth a test rather than a `sortedWith` in the ViewModel.
 *
 * `sortedWith` is stable, so rows that tie keep the cache's own order (`ORDER BY id`, 3.3) — which
 * is what the server does too, having no tiebreaker of its own.
 *
 * Two deliberate departures from the SQL, both because this runs on the rows the app already has:
 * [SubscriptionSort.NAME] compares case-insensitively (SQLite's default collation does not), and
 * a missing `next_payment` sorts last rather than first — the server never sees one, since a blank
 * date only becomes `null` in this app's mapper (2.1).
 */
@Single
class SubscriptionSorter {

    fun sort(subscriptions: List<Subscription>, sort: SubscriptionSort): List<Subscription> =
        subscriptions.sortedWith(comparatorFor(sort))

    private fun comparatorFor(sort: SubscriptionSort): Comparator<Subscription> = when (sort) {
        SubscriptionSort.NEXT_PAYMENT -> compareBy(nullsLast()) { it.nextPayment }
        SubscriptionSort.NAME -> compareBy { it.name.lowercase() }
        SubscriptionSort.PRICE -> compareByDescending { it.price }
        SubscriptionSort.ID -> compareByDescending { it.id }
        SubscriptionSort.PAYER -> compareBy { it.payerName.lowercase() }
        SubscriptionSort.CATEGORY -> compareBy { it.categoryName.lowercase() }
        SubscriptionSort.PAYMENT_METHOD -> compareBy { it.paymentMethodName.lowercase() }
        SubscriptionSort.INACTIVE -> compareBy { !it.isActive }
    }
}

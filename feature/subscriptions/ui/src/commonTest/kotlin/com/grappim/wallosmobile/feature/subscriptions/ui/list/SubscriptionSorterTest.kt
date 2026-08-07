package com.grappim.wallosmobile.feature.subscriptions.ui.list

import com.grappim.wallosmobile.feature.subscriptions.domain.model.BillingCycle
import com.grappim.wallosmobile.feature.subscriptions.domain.model.Subscription
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * API doc §3.2's table, one test per row: the direction belongs to the *field*, and it is the kind
 * of fact that reads as arbitrary and gets "tidied" into a uniform ascending sort by the next
 * person through. `price` and `id` descend; everything else ascends.
 */
class SubscriptionSorterTest {

    private val sut = SubscriptionSorter()

    private fun ids(sort: SubscriptionSort) = sut.sort(all, sort).map { it.id }

    @Test
    fun `next payment ascends, and a row without one goes last`() {
        assertEquals(listOf(2, 1, 3, 4), ids(SubscriptionSort.NEXT_PAYMENT))
    }

    @Test
    fun `name ascends, case-insensitively`() {
        assertEquals(listOf(4, 1, 3, 2), ids(SubscriptionSort.NAME))
    }

    @Test
    fun `price descends`() {
        assertEquals(listOf(3, 2, 4, 1), ids(SubscriptionSort.PRICE))
    }

    @Test
    fun `id descends, so the newest row is on top`() {
        assertEquals(listOf(4, 3, 2, 1), ids(SubscriptionSort.ID))
    }

    @Test
    fun `payer ascends by the resolved name, the id never reaching this app`() {
        assertEquals(listOf(2, 4, 1, 3), ids(SubscriptionSort.PAYER))
    }

    @Test
    fun `category ascends by the resolved name`() {
        assertEquals(listOf(1, 4, 3, 2), ids(SubscriptionSort.CATEGORY))
    }

    @Test
    fun `payment method ascends by the resolved name`() {
        assertEquals(listOf(1, 3, 2, 4), ids(SubscriptionSort.PAYMENT_METHOD))
    }

    @Test
    fun `the inactive flag ascends, which puts the live rows first`() {
        assertEquals(listOf(1, 2, 4, 3), ids(SubscriptionSort.INACTIVE))
    }

    /** Ties keep the order they arrived in — the cache's, since the server has no tiebreaker either. */
    @Test
    fun `rows that tie are left in the order the cache gave them`() {
        val tied = listOf(subscription(id = 7), subscription(id = 3), subscription(id = 5))

        assertEquals(listOf(7, 3, 5), sut.sort(tied, SubscriptionSort.CATEGORY).map { it.id })
    }

    @Test
    fun `an empty cache sorts to an empty list rather than throwing`() {
        assertEquals(emptyList(), sut.sort(emptyList(), SubscriptionSort.PRICE))
    }

    private val all = listOf(
        subscription(id = 1).copy(
            name = "Disney+",
            price = 8.99,
            nextPayment = LocalDate(2026, 3, 10),
            categoryName = "Entertainment",
            paymentMethodName = "Direct Debit",
            payerName = "gregorz"
        ),
        subscription(id = 2).copy(
            name = "netflix",
            price = 18.0,
            nextPayment = LocalDate(2026, 2, 12),
            categoryName = "Streaming",
            paymentMethodName = "PayPal",
            payerName = "alice"
        ),
        subscription(id = 3).copy(
            name = "Fiton",
            price = 31.99,
            nextPayment = LocalDate(2026, 4, 1),
            isActive = false,
            categoryName = "Health",
            paymentMethodName = "Direct Debit",
            payerName = "zoe"
        ),
        subscription(id = 4).copy(
            name = "1&1 Telekom",
            price = 12.0,
            nextPayment = null,
            categoryName = "Fitness",
            paymentMethodName = "Wire Transfer",
            payerName = "Bob"
        )
    )

    private fun subscription(id: Int) = Subscription(
        id = id,
        name = "",
        logo = "",
        price = 0.0,
        currencyId = 1,
        currencySymbol = "€",
        cycle = BillingCycle.MONTHS,
        frequency = 1,
        nextPayment = null,
        startDate = null,
        isActive = true,
        notes = "",
        url = "",
        categoryName = "",
        paymentMethodName = "",
        payerName = "",
        categoryId = null,
        paymentMethodId = null,
        payerUserId = null,
        autoRenew = true,
        notify = false,
        notifyDaysBefore = null
    )
}

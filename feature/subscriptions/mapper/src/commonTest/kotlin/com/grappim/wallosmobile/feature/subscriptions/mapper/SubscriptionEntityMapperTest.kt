package com.grappim.wallosmobile.feature.subscriptions.mapper

import com.grappim.wallosmobile.core.storage.db.SubscriptionEntity
import com.grappim.wallosmobile.feature.subscriptions.domain.model.BillingCycle
import com.grappim.wallosmobile.feature.subscriptions.domain.model.Subscription
import com.grappim.wallosmobile.utils.formatter.datetime.DateFormatter
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SubscriptionEntityMapperTest {

    private val mapper = SubscriptionEntityMapper(DateFormatter())

    /** The property that matters: what the cache stores is what the screen gets back. */
    @Test
    fun `a row survives the round trip unchanged`() {
        val subscription = subscription()

        assertEquals(subscription, mapper.toDomain(mapper.toEntity(subscription)))
    }

    @Test
    fun `dates are stored as the ISO text the column holds`() {
        val entity = mapper.toEntity(subscription())

        assertEquals("2026-03-10", entity.nextPayment)
        assertEquals("2024-03-05", entity.startDate)
    }

    @Test
    fun `the cycle is stored as its wire code, so the column needs no converter`() {
        assertEquals(3, mapper.toEntity(subscription()).cycleCode)
        assertEquals(BillingCycle.MONTHS, mapper.toDomain(entity(cycleCode = 3)).cycle)
    }

    @Test
    fun `an absent date is null in both directions`() {
        val entity = mapper.toEntity(subscription(nextPayment = null, startDate = null))

        assertNull(entity.nextPayment)
        assertNull(entity.startDate)
        assertNull(mapper.toDomain(entity).nextPayment)
        assertNull(mapper.toDomain(entity).startDate)
    }

    /** A code this build doesn't know: `BillingCycle.fromCode` is nullable for exactly this (2.1). */
    @Test
    fun `a cycle code from a newer instance reads as no cycle`() {
        assertNull(mapper.toDomain(entity(cycleCode = 99)).cycle)
    }

    @Test
    fun `an unknown cycle stores nothing rather than a wrong code`() {
        assertNull(mapper.toEntity(subscription(cycle = null)).cycleCode)
    }

    /** A date some older instance wrote in another shape can't sink the list it arrived in. */
    @Test
    fun `an unparseable stored date reads as absent`() {
        assertNull(mapper.toDomain(entity(nextPayment = "03/10/2026")).nextPayment)
    }

    private fun subscription(
        cycle: BillingCycle? = BillingCycle.MONTHS,
        nextPayment: LocalDate? = LocalDate(2026, 3, 10),
        startDate: LocalDate? = LocalDate(2024, 3, 5)
    ) = Subscription(
        id = 1,
        name = "1&1 Telekom",
        logo = "1732563509-Disney.png",
        price = 8.99,
        currencyId = 1,
        currencySymbol = "€",
        cycle = cycle,
        frequency = 6,
        nextPayment = nextPayment,
        startDate = startDate,
        isActive = true,
        notes = "Family plan",
        url = "https://disneyplus.com",
        categoryName = "Health & Fitness",
        paymentMethodName = "Direct Debit",
        payerName = "gregorz"
    )

    private fun entity(cycleCode: Int? = 3, nextPayment: String? = "2026-03-10") = SubscriptionEntity(
        id = 1,
        name = "Disney+",
        logo = "",
        price = 8.99,
        currencyId = 1,
        currencySymbol = "€",
        cycleCode = cycleCode,
        frequency = 1,
        nextPayment = nextPayment,
        startDate = null,
        isActive = true,
        notes = "",
        url = "",
        categoryName = "Entertainment",
        paymentMethodName = "Direct Debit",
        payerName = "gregorz"
    )
}

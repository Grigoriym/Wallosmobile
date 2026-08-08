package com.grappim.wallosmobile.feature.dashboard.domain.calculator

import com.grappim.wallosmobile.feature.subscriptions.domain.model.BillingCycle
import com.grappim.wallosmobile.feature.subscriptions.domain.model.Subscription
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [UpcomingPaymentsCalculator.calculate] mirrors two web queries in one pass (`index.php:76`
 * Upcoming, `:85` Overdue): a past-due, auto-renewing row rolls forward onto the upcoming side
 * (this app's own compensation for a cache the web doesn't have, since the server's own cron —
 * `updatenextpayment.php` — would already have advanced it); a past-due, non-auto-renewing row
 * lands on the overdue side instead, unrolled, since there is no "next" occurrence to invent.
 */
class UpcomingPaymentsCalculatorTest {

    private val sut = UpcomingPaymentsCalculator()
    private val today = LocalDate(2026, 8, 8)

    @Test
    fun `a future next payment passes through unchanged on the upcoming side`() {
        val future = LocalDate(2026, 8, 18)

        val result = sut.calculate(listOf(subscription(id = 1).copy(nextPayment = future)), today)

        assertEquals(future, result.upcoming.single().nextPayment)
        assertEquals(emptyList(), result.overdue)
    }

    @Test
    fun `a past-due day cycle rolls forward by whole cycles until it reaches today`() {
        val sub = subscription(id = 1).copy(
            cycle = BillingCycle.DAYS,
            frequency = 2,
            nextPayment = LocalDate(2026, 8, 3)
        )

        val result = sut.calculate(listOf(sub), today)

        assertEquals(LocalDate(2026, 8, 9), result.upcoming.single().nextPayment)
        assertEquals(emptyList(), result.overdue)
    }

    @Test
    fun `a past-due week cycle rolls forward by whole cycles until it reaches today`() {
        val sub = subscription(id = 1).copy(
            cycle = BillingCycle.WEEKS,
            frequency = 1,
            nextPayment = LocalDate(2026, 7, 18)
        )

        val result = sut.calculate(listOf(sub), today)

        assertEquals(LocalDate(2026, 8, 8), result.upcoming.single().nextPayment)
    }

    @Test
    fun `a past-due month cycle rolls forward by the frequency multiplier`() {
        val sub = subscription(id = 1).copy(
            cycle = BillingCycle.MONTHS,
            frequency = 3,
            nextPayment = LocalDate(2025, 11, 8)
        )

        val result = sut.calculate(listOf(sub), today)

        assertEquals(LocalDate(2026, 8, 8), result.upcoming.single().nextPayment)
    }

    @Test
    fun `a past-due year cycle rolls forward by whole cycles until it reaches today`() {
        val sub = subscription(id = 1).copy(
            cycle = BillingCycle.YEARS,
            frequency = 1,
            nextPayment = LocalDate(2024, 8, 8)
        )

        val result = sut.calculate(listOf(sub), today)

        assertEquals(LocalDate(2026, 8, 8), result.upcoming.single().nextPayment)
    }

    @Test
    fun `an inactive row is excluded from both lists even with a past-due next payment`() {
        val sub = subscription(id = 1).copy(isActive = false, autoRenew = false, nextPayment = LocalDate(2026, 8, 1))

        val result = sut.calculate(listOf(sub), today)

        assertEquals(emptyList(), result.upcoming)
        assertEquals(emptyList(), result.overdue)
    }

    @Test
    fun `a null next payment is excluded from both lists`() {
        val sub = subscription(id = 1).copy(nextPayment = null)

        val result = sut.calculate(listOf(sub), today)

        assertEquals(emptyList(), result.upcoming)
        assertEquals(emptyList(), result.overdue)
    }

    @Test
    fun `an unrecognised cycle is excluded from both lists even with a future next payment`() {
        val sub = subscription(id = 1).copy(cycle = null, nextPayment = LocalDate(2026, 9, 1))

        val result = sut.calculate(listOf(sub), today)

        assertEquals(emptyList(), result.upcoming)
        assertEquals(emptyList(), result.overdue)
    }

    @Test
    fun `a past-due row with auto-renew off lands in overdue rather than rolled forward`() {
        val sub = subscription(id = 1).copy(autoRenew = false, nextPayment = LocalDate(2026, 8, 1))

        val result = sut.calculate(listOf(sub), today)

        assertEquals(emptyList(), result.upcoming)
        assertEquals(LocalDate(2026, 8, 1), result.overdue.single().nextPayment)
    }

    @Test
    fun `a past-due one-time row is excluded from both lists, having no periodicity to roll by`() {
        val sub = subscription(id = 1).copy(
            cycle = BillingCycle.ONE_TIME,
            autoRenew = false,
            nextPayment = LocalDate(2026, 8, 1)
        )

        val result = sut.calculate(listOf(sub), today)

        assertEquals(emptyList(), result.upcoming)
        assertEquals(emptyList(), result.overdue)
    }

    @Test
    fun `a future one-time row is excluded from upcoming, unlike a future recurring row`() {
        val sub = subscription(id = 1).copy(cycle = BillingCycle.ONE_TIME, nextPayment = LocalDate(2026, 9, 1))

        val result = sut.calculate(listOf(sub), today)

        assertEquals(emptyList(), result.upcoming)
        assertEquals(emptyList(), result.overdue)
    }

    @Test
    fun `a non-positive frequency is excluded rather than looping forever`() {
        val sub = subscription(id = 1).copy(frequency = 0, nextPayment = LocalDate(2026, 8, 1))

        val result = sut.calculate(listOf(sub), today)

        assertEquals(emptyList(), result.upcoming)
        assertEquals(emptyList(), result.overdue)
    }

    @Test
    fun `upcoming is sorted ascending by the resolved next payment`() {
        val rows = listOf(
            subscription(id = 1).copy(nextPayment = LocalDate(2026, 9, 1)),
            subscription(id = 2).copy(nextPayment = LocalDate(2026, 8, 20)),
            subscription(id = 3).copy(cycle = BillingCycle.DAYS, frequency = 1, nextPayment = LocalDate(2026, 8, 7))
        )

        val result = sut.calculate(rows, today)

        assertEquals(listOf(3, 2, 1), result.upcoming.map { it.id })
    }

    @Test
    fun `more than 3 eligible upcoming rows yields exactly the 3 soonest`() {
        val rows = (1..5).map { id ->
            subscription(id = id).copy(nextPayment = LocalDate(2026, 8, 8 + id))
        }

        val result = sut.calculate(rows, today)

        assertEquals(listOf(1, 2, 3), result.upcoming.map { it.id })
    }

    @Test
    fun `overdue is unlimited and sorted ascending, unlike upcoming`() {
        val rows = (1..5).map { id ->
            subscription(id = id).copy(autoRenew = false, nextPayment = LocalDate(2026, 8, id))
        }

        val result = sut.calculate(rows, today)

        assertEquals(listOf(1, 2, 3, 4, 5), result.overdue.map { it.id })
        assertEquals(emptyList(), result.upcoming)
    }

    @Test
    fun `an empty cache calculates to two empty lists rather than throwing`() {
        val result = sut.calculate(emptyList(), today)

        assertEquals(emptyList(), result.upcoming)
        assertEquals(emptyList(), result.overdue)
    }

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

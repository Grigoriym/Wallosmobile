package com.grappim.wallosmobile.feature.dashboard.domain.calculator

import com.grappim.wallosmobile.feature.subscriptions.domain.model.BillingCycle
import com.grappim.wallosmobile.feature.subscriptions.domain.model.Subscription
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [UpcomingPaymentsCalculator.calculate] mirrors the server's own
 * `endpoints/cronjobs/updatenextpayment.php`: it only ever advances a row that cron would advance
 * (`isActive` + `autoRenew`), and drops one it wouldn't rather than inventing a date the server
 * itself never computes.
 */
class UpcomingPaymentsCalculatorTest {

    private val sut = UpcomingPaymentsCalculator()
    private val today = LocalDate(2026, 8, 8)

    @Test
    fun `a future next payment passes through unchanged`() {
        val future = LocalDate(2026, 8, 18)

        val result = sut.calculate(listOf(subscription(id = 1).copy(nextPayment = future)), today)

        assertEquals(future, result.single().nextPayment)
    }

    @Test
    fun `a past-due day cycle rolls forward by whole cycles until it reaches today`() {
        val sub = subscription(id = 1).copy(
            cycle = BillingCycle.DAYS,
            frequency = 2,
            nextPayment = LocalDate(2026, 8, 3)
        )

        val result = sut.calculate(listOf(sub), today)

        assertEquals(LocalDate(2026, 8, 9), result.single().nextPayment)
    }

    @Test
    fun `a past-due week cycle rolls forward by whole cycles until it reaches today`() {
        val sub = subscription(id = 1).copy(
            cycle = BillingCycle.WEEKS,
            frequency = 1,
            nextPayment = LocalDate(2026, 7, 18)
        )

        val result = sut.calculate(listOf(sub), today)

        assertEquals(LocalDate(2026, 8, 8), result.single().nextPayment)
    }

    @Test
    fun `a past-due month cycle rolls forward by the frequency multiplier`() {
        val sub = subscription(id = 1).copy(
            cycle = BillingCycle.MONTHS,
            frequency = 3,
            nextPayment = LocalDate(2025, 11, 8)
        )

        val result = sut.calculate(listOf(sub), today)

        assertEquals(LocalDate(2026, 8, 8), result.single().nextPayment)
    }

    @Test
    fun `a past-due year cycle rolls forward by whole cycles until it reaches today`() {
        val sub = subscription(id = 1).copy(
            cycle = BillingCycle.YEARS,
            frequency = 1,
            nextPayment = LocalDate(2024, 8, 8)
        )

        val result = sut.calculate(listOf(sub), today)

        assertEquals(LocalDate(2026, 8, 8), result.single().nextPayment)
    }

    @Test
    fun `an inactive row is excluded even with a future next payment`() {
        val sub = subscription(id = 1).copy(isActive = false, nextPayment = LocalDate(2026, 9, 1))

        assertEquals(emptyList(), sut.calculate(listOf(sub), today))
    }

    @Test
    fun `a null next payment is excluded`() {
        val sub = subscription(id = 1).copy(nextPayment = null)

        assertEquals(emptyList(), sut.calculate(listOf(sub), today))
    }

    @Test
    fun `an unrecognised cycle is excluded even with a future next payment`() {
        val sub = subscription(id = 1).copy(cycle = null, nextPayment = LocalDate(2026, 9, 1))

        assertEquals(emptyList(), sut.calculate(listOf(sub), today))
    }

    /**
     * The server's cron only advances `auto_renew = 1` rows (`updatenextpayment.php`'s own
     * `WHERE` clause) — a non-renewing row stays stuck in the past there too, so there is no real
     * "next" occurrence to invent client-side.
     */
    @Test
    fun `a past-due row with auto-renew off is excluded rather than rolled forward`() {
        val sub = subscription(id = 1).copy(autoRenew = false, nextPayment = LocalDate(2026, 8, 1))

        assertEquals(emptyList(), sut.calculate(listOf(sub), today))
    }

    @Test
    fun `a past-due one-time row is excluded, having no periodicity to roll by`() {
        val sub = subscription(id = 1).copy(cycle = BillingCycle.ONE_TIME, nextPayment = LocalDate(2026, 8, 1))

        assertEquals(emptyList(), sut.calculate(listOf(sub), today))
    }

    @Test
    fun `a future one-time row passes through unchanged`() {
        val future = LocalDate(2026, 9, 1)
        val sub = subscription(id = 1).copy(cycle = BillingCycle.ONE_TIME, nextPayment = future)

        assertEquals(future, sut.calculate(listOf(sub), today).single().nextPayment)
    }

    @Test
    fun `a non-positive frequency is excluded rather than looping forever`() {
        val sub = subscription(id = 1).copy(frequency = 0, nextPayment = LocalDate(2026, 8, 1))

        assertEquals(emptyList(), sut.calculate(listOf(sub), today))
    }

    @Test
    fun `output is sorted ascending by the resolved next payment`() {
        val rows = listOf(
            subscription(id = 1).copy(nextPayment = LocalDate(2026, 9, 1)),
            subscription(id = 2).copy(nextPayment = LocalDate(2026, 8, 20)),
            subscription(id = 3).copy(cycle = BillingCycle.DAYS, frequency = 1, nextPayment = LocalDate(2026, 8, 7))
        )

        val result = sut.calculate(rows, today)

        assertEquals(listOf(3, 2, 1), result.map { it.id })
    }

    @Test
    fun `an empty cache calculates to an empty list rather than throwing`() {
        assertEquals(emptyList(), sut.calculate(emptyList(), today))
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

package com.grappim.wallosmobile.feature.dashboard.domain.calculator

import com.grappim.wallosmobile.feature.subscriptions.domain.model.BillingCycle
import com.grappim.wallosmobile.feature.subscriptions.domain.model.Subscription
import kotlin.test.Test
import kotlin.test.assertEquals

class SubscriptionStatsCalculatorTest {

    private val sut = SubscriptionStatsCalculator()

    @Test
    fun `an empty cache calculates to zeroed stats rather than throwing`() {
        val result = sut.calculate(emptyList(), monthlyCost = 0.0)

        assertEquals(0, result.yourSubscriptions.activeCount)
        assertEquals(0.0, result.yourSubscriptions.monthlyCost)
        assertEquals(0.0, result.yourSubscriptions.yearlyCost)
        assertEquals(0, result.yourSavings.inactiveCount)
        assertEquals(0.0, result.yourSavings.savingsPerMonth)
    }

    @Test
    fun `active count excludes one-time rows even though they are active`() {
        val rows = listOf(
            subscription(id = 1, isActive = true, cycle = BillingCycle.MONTHS),
            subscription(id = 2, isActive = true, cycle = BillingCycle.ONE_TIME)
        )

        val result = sut.calculate(rows, monthlyCost = 42.0)

        assertEquals(1, result.yourSubscriptions.activeCount)
    }

    @Test
    fun `monthly cost is the fetched total unchanged, and yearly cost is it times 12`() {
        val result = sut.calculate(emptyList(), monthlyCost = 10.0)

        assertEquals(10.0, result.yourSubscriptions.monthlyCost)
        assertEquals(120.0, result.yourSubscriptions.yearlyCost)
    }

    @Test
    fun `inactive count includes a one-time row unlike active count`() {
        val rows = listOf(subscription(id = 1, isActive = false, cycle = BillingCycle.ONE_TIME))

        val result = sut.calculate(rows, monthlyCost = 0.0)

        assertEquals(1, result.yourSavings.inactiveCount)
    }

    @Test
    fun `an inactive one-time row is counted but contributes nothing to savings`() {
        val rows = listOf(
            subscription(id = 1, isActive = false, cycle = BillingCycle.ONE_TIME, price = 100.0)
        )

        val result = sut.calculate(rows, monthlyCost = 0.0)

        assertEquals(1, result.yourSavings.inactiveCount)
        assertEquals(0.0, result.yourSavings.savingsPerMonth)
    }

    @Test
    fun `an inactive day-cycle row converts to a per-month price`() {
        val rows = listOf(
            subscription(id = 1, isActive = false, cycle = BillingCycle.DAYS, frequency = 1, price = 1.0)
        )

        val result = sut.calculate(rows, monthlyCost = 0.0)

        assertEquals(30.0, result.yourSavings.savingsPerMonth)
    }

    @Test
    fun `an inactive week-cycle row converts to a per-month price`() {
        val rows = listOf(
            subscription(id = 1, isActive = false, cycle = BillingCycle.WEEKS, frequency = 1, price = 10.0)
        )

        val result = sut.calculate(rows, monthlyCost = 0.0)

        assertEquals(43.5, result.yourSavings.savingsPerMonth)
    }

    @Test
    fun `an inactive month-cycle row divides by its frequency`() {
        val rows = listOf(
            subscription(id = 1, isActive = false, cycle = BillingCycle.MONTHS, frequency = 2, price = 20.0)
        )

        val result = sut.calculate(rows, monthlyCost = 0.0)

        assertEquals(10.0, result.yourSavings.savingsPerMonth)
    }

    @Test
    fun `an inactive year-cycle row divides by 12 times its frequency`() {
        val rows = listOf(
            subscription(id = 1, isActive = false, cycle = BillingCycle.YEARS, frequency = 1, price = 120.0)
        )

        val result = sut.calculate(rows, monthlyCost = 0.0)

        assertEquals(10.0, result.yourSavings.savingsPerMonth)
    }

    @Test
    fun `an inactive row with an unrecognised cycle contributes nothing to savings`() {
        val rows = listOf(subscription(id = 1, isActive = false, cycle = null, price = 100.0))

        val result = sut.calculate(rows, monthlyCost = 0.0)

        assertEquals(1, result.yourSavings.inactiveCount)
        assertEquals(0.0, result.yourSavings.savingsPerMonth)
    }

    @Test
    fun `a non-positive frequency contributes nothing rather than dividing by zero`() {
        val rows = listOf(
            subscription(id = 1, isActive = false, cycle = BillingCycle.MONTHS, frequency = 0, price = 100.0)
        )

        val result = sut.calculate(rows, monthlyCost = 0.0)

        assertEquals(0.0, result.yourSavings.savingsPerMonth)
    }

    @Test
    fun `savings sum across multiple inactive rows`() {
        val rows = listOf(
            subscription(id = 1, isActive = false, cycle = BillingCycle.MONTHS, frequency = 1, price = 10.0),
            subscription(id = 2, isActive = false, cycle = BillingCycle.YEARS, frequency = 1, price = 120.0)
        )

        val result = sut.calculate(rows, monthlyCost = 0.0)

        assertEquals(20.0, result.yourSavings.savingsPerMonth)
    }

    private fun subscription(
        id: Int,
        isActive: Boolean,
        cycle: BillingCycle?,
        frequency: Int = 1,
        price: Double = 0.0
    ) = Subscription(
        id = id,
        name = "",
        logo = "",
        price = price,
        currencyId = 1,
        currencySymbol = "€",
        cycle = cycle,
        frequency = frequency,
        nextPayment = null,
        startDate = null,
        isActive = isActive,
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

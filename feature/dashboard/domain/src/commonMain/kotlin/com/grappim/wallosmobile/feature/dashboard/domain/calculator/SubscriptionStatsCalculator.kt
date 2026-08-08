package com.grappim.wallosmobile.feature.dashboard.domain.calculator

import com.grappim.wallosmobile.feature.dashboard.domain.model.YourSavings
import com.grappim.wallosmobile.feature.dashboard.domain.model.YourSubscriptions
import com.grappim.wallosmobile.feature.subscriptions.domain.model.BillingCycle
import com.grappim.wallosmobile.feature.subscriptions.domain.model.Subscription

private const val MONTHS_PER_YEAR = 12
private const val DAYS_PER_MONTH = 30.0
private const val WEEKS_PER_MONTH = 4.35

/**
 * The web's "Your Subscriptions" and "Your Savings" cards (`index.php:373`/`:411`,
 * `stats_calculations.php:195-262`) — a single pass over the same cached subscription list
 * [UpcomingPaymentsCalculator] already reads, no new endpoint. [calculate]'s [monthlyCost]
 * parameter is the dashboard's already-fetched total (not resummed here); only
 * [YourSubscriptions.yearlyCost] is derived from it (`× 12`, `stats_calculations.php`'s own
 * `$totalCostPerYear`).
 *
 * [YourSubscriptions.activeCount] mirrors `stats_calculations.php`'s own count exactly: an active,
 * one-time (`cycle = 5`) row is *not* counted, though it still contributes to the fetched
 * [monthlyCost] total the way the server computes it. [YourSavings.inactiveCount] has no such
 * filter — every inactive row counts, one-time or not — but [pricePerMonth] returns 0 for a
 * one-time row, so it can't move [YourSavings.savingsPerMonth] either way.
 */
class SubscriptionStatsCalculator {

    fun calculate(subscriptions: List<Subscription>, monthlyCost: Double): SubscriptionStats {
        val activeCount = subscriptions.count { it.isActive && it.cycle != BillingCycle.ONE_TIME }
        val inactiveRows = subscriptions.filter { !it.isActive }

        return SubscriptionStats(
            yourSubscriptions = YourSubscriptions(
                activeCount = activeCount,
                monthlyCost = monthlyCost,
                yearlyCost = monthlyCost * MONTHS_PER_YEAR
            ),
            yourSavings = YourSavings(
                inactiveCount = inactiveRows.size,
                savingsPerMonth = inactiveRows.sumOf { pricePerMonth(it.cycle, it.frequency, it.price) }
            )
        )
    }

    /** Mirrors `stats_calculations.php`'s own `getPricePerMonth` exactly. */
    private fun pricePerMonth(cycle: BillingCycle?, frequency: Int, price: Double): Double {
        if (frequency <= 0) return 0.0
        return when (cycle) {
            BillingCycle.DAYS -> price * (DAYS_PER_MONTH / frequency)
            BillingCycle.WEEKS -> price * (WEEKS_PER_MONTH / frequency)
            BillingCycle.MONTHS -> price / frequency
            BillingCycle.YEARS -> price / (MONTHS_PER_YEAR * frequency)
            BillingCycle.ONE_TIME, null -> 0.0
        }
    }
}

data class SubscriptionStats(val yourSubscriptions: YourSubscriptions, val yourSavings: YourSavings)

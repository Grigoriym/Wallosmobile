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
 * [UpcomingPaymentsCalculator] already reads, no new endpoint. [YourSubscriptions.monthlyCost] is
 * summed locally from active rows' own [pricePerMonth] (`stats_calculations.php`'s own
 * `$totalCostPerMonth`) rather than taking the dashboard's separately-fetched `get_monthly_cost`
 * total (10.8) — that endpoint sums billing *occurrences* landing in a calendar month, a different
 * metric the web itself never renders. [YourSubscriptions.yearlyCost] is that sum `× 12`
 * (`$totalCostPerYear`).
 *
 * [YourSubscriptions.activeCount] mirrors `stats_calculations.php`'s own count exactly: an active,
 * one-time (`cycle = 5`) row is *not* counted, though [pricePerMonth] returning 0 for it means it
 * can't move [YourSubscriptions.monthlyCost] either way. [YourSavings.inactiveCount] has no such
 * filter — every inactive row counts, one-time or not — but [pricePerMonth] returns 0 for a
 * one-time row, so it can't move [YourSavings.savingsPerMonth] either way.
 */
class SubscriptionStatsCalculator {

    fun calculate(subscriptions: List<Subscription>): SubscriptionStats {
        val activeRows = subscriptions.filter { it.isActive }
        val activeCount = activeRows.count { it.cycle != BillingCycle.ONE_TIME }
        val monthlyCost = activeRows.sumOf { pricePerMonth(it.cycle, it.frequency, it.price) }
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

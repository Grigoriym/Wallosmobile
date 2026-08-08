package com.grappim.wallosmobile.feature.dashboard.domain.model

/**
 * The web's "Your Subscriptions" card (`index.php:373`, `stats_calculations.php`'s
 * `$activeSubscriptions`/`$totalCostPerYear`), shown only when [activeCount] is greater than 0.
 * [monthlyCost] is the dashboard's already-fetched [MonthlyCost.amount], not resummed —
 * [SubscriptionStatsCalculator][com.grappim.wallosmobile.feature.dashboard.domain.calculator.SubscriptionStatsCalculator]
 * only derives [yearlyCost] from it.
 */
data class YourSubscriptions(val activeCount: Int, val monthlyCost: Double, val yearlyCost: Double)

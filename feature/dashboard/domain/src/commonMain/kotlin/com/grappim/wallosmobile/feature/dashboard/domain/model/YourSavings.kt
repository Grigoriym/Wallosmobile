package com.grappim.wallosmobile.feature.dashboard.domain.model

/**
 * The web's "Your Savings" card (`index.php:411`, `stats_calculations.php`'s
 * `$inactiveSubscriptions`/`$totalSavingsPerMonth`), shown only when [inactiveCount] is greater
 * than 0. [savingsPerMonth] is the simpler sum of every inactive row's per-month price — the web
 * nets this against the per-month cost of any `replacement_subscription_id`
 * (`stats_calculations.php:242-262`), a field `Subscription` (domain) doesn't carry (2.1's trim),
 * so this figure is real but not identical to the web's when a replacement exists.
 */
data class YourSavings(val inactiveCount: Int, val savingsPerMonth: Double)

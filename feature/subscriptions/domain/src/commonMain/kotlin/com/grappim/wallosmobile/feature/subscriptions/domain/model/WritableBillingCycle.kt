package com.grappim.wallosmobile.feature.subscriptions.domain.model

/**
 * The four cycles `set_subscriptions.php` actually accepts (`WALLOS_API.md` §3.4) — a subset of
 * [BillingCycle] with [BillingCycle.ONE_TIME] left out entirely, so a write cannot name the one
 * cycle the server rejects. [code] is the same wire value [BillingCycle] uses.
 */
enum class WritableBillingCycle(val code: Int) {
    DAYS(1),
    WEEKS(2),
    MONTHS(3),
    YEARS(4)
}

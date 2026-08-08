package com.grappim.wallosmobile.feature.dashboard.domain.calculator

import com.grappim.wallosmobile.feature.subscriptions.domain.model.BillingCycle
import com.grappim.wallosmobile.feature.subscriptions.domain.model.Subscription
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

private const val UPCOMING_LIMIT = 3

/**
 * The cached `next_payment` the app has can be stale: the server's own cron
 * (`endpoints/cronjobs/updatenextpayment.php`) is what normally rolls it forward each cycle, and
 * only runs against rows where `auto_renew = 1 AND inactive = 0` — a row this app hasn't refreshed
 * since that cron last ran can show a `nextPayment` that has already passed.
 *
 * [calculate] mirrors two web queries in one pass (`index.php:76` Upcoming, `:85` Overdue): both
 * drop inactive rows and [BillingCycle.ONE_TIME] (`cycle != 5`) entirely. A past-due,
 * auto-renewing row is rolled forward — this app's own compensation for a cache the web doesn't
 * have, since the server's cron would already have advanced it — and lands on the *upcoming* side
 * with its rolled date; a past-due, non-auto-renewing row is never rolled (there is no "next"
 * occurrence to invent, since the server's own cron would never advance it either) and lands on
 * the *overdue* side instead, at its original date. Upcoming is capped at 3 after sorting
 * (`index.php:76`'s own `LIMIT 3`); Overdue is unlimited (`index.php:85` has none). A `frequency`
 * that isn't positive can't advance the date at all and is dropped from both.
 */
class UpcomingPaymentsCalculator {

    fun calculate(subscriptions: List<Subscription>, today: LocalDate): UpcomingAndOverdue {
        val eligible = subscriptions.mapNotNull { subscription -> toEligible(subscription) }

        val upcoming = mutableListOf<Subscription>()
        val overdue = mutableListOf<Subscription>()

        eligible.forEach { (subscription, nextPayment, cycle) ->
            when {
                nextPayment >= today -> upcoming += subscription

                subscription.autoRenew -> {
                    val rolled = rollForward(nextPayment, cycle, subscription.frequency, today)
                    if (rolled != null) upcoming += subscription.copy(nextPayment = rolled)
                }

                else -> overdue += subscription
            }
        }

        return UpcomingAndOverdue(
            upcoming = upcoming.sortedBy { it.nextPayment }.take(UPCOMING_LIMIT),
            overdue = overdue.sortedBy { it.nextPayment }
        )
    }

    private fun toEligible(subscription: Subscription): Eligible? {
        if (!subscription.isActive) return null
        val nextPayment = subscription.nextPayment ?: return null
        val cycle = subscription.cycle ?: return null
        if (cycle == BillingCycle.ONE_TIME) return null
        return Eligible(subscription, nextPayment, cycle)
    }

    private fun rollForward(nextPayment: LocalDate, cycle: BillingCycle, frequency: Int, today: LocalDate): LocalDate? {
        val unit = cycle.toDateTimeUnit() ?: return null
        if (frequency <= 0) return null

        var date = nextPayment
        while (date < today) {
            date = date.plus(frequency, unit)
        }
        return date
    }

    private fun BillingCycle.toDateTimeUnit(): DateTimeUnit.DateBased? = when (this) {
        BillingCycle.DAYS -> DateTimeUnit.DAY
        BillingCycle.WEEKS -> DateTimeUnit.WEEK
        BillingCycle.MONTHS -> DateTimeUnit.MONTH
        BillingCycle.YEARS -> DateTimeUnit.YEAR
        BillingCycle.ONE_TIME -> null
    }

    private data class Eligible(val subscription: Subscription, val nextPayment: LocalDate, val cycle: BillingCycle)
}

/**
 * [UpcomingPaymentsCalculator.calculate]'s two outputs — Upcoming Payments and Overdue Renewals
 * are shown as separate dashboard sections, never merged.
 */
data class UpcomingAndOverdue(val upcoming: List<Subscription>, val overdue: List<Subscription>)

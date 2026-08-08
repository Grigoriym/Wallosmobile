package com.grappim.wallosmobile.feature.dashboard.domain.calculator

import com.grappim.wallosmobile.feature.subscriptions.domain.model.BillingCycle
import com.grappim.wallosmobile.feature.subscriptions.domain.model.Subscription
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/**
 * The cached `next_payment` the app has can be stale: the server's own cron
 * (`endpoints/cronjobs/updatenextpayment.php`) is what normally rolls it forward each cycle, and
 * only runs against rows where `auto_renew = 1 AND inactive = 0` — a row this app hasn't refreshed
 * since that cron last ran can show a `nextPayment` that has already passed.
 *
 * Confirmed against that same cron source rather than guessed: it only ever advances a row the
 * server itself would advance, so [calculate] mirrors its two gates precisely — inactive rows are
 * dropped, and a past-due row with `autoRenew == false` is dropped rather than rolled forward,
 * because the server never advances that date either (there both is and never will be a "next"
 * occurrence to show). [BillingCycle.ONE_TIME] has no periodicity to roll by for the same reason a
 * one-time row is never auto-renewing in practice, so a past-due one is dropped too. A `frequency`
 * that isn't positive can't advance the date at all and is dropped rather than looping forever.
 */
class UpcomingPaymentsCalculator {

    fun calculate(subscriptions: List<Subscription>, today: LocalDate): List<Subscription> = subscriptions
        .asSequence()
        .filter { it.isActive }
        .mapNotNull { subscription -> resolve(subscription, today) }
        .sortedBy { it.nextPayment }
        .toList()

    private fun resolve(subscription: Subscription, today: LocalDate): Subscription? {
        val nextPayment = subscription.nextPayment ?: return null
        val cycle = subscription.cycle ?: return null

        if (nextPayment >= today) return subscription
        if (!subscription.autoRenew) return null

        val rolled = rollForward(nextPayment, cycle, subscription.frequency, today) ?: return null
        return subscription.copy(nextPayment = rolled)
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
}

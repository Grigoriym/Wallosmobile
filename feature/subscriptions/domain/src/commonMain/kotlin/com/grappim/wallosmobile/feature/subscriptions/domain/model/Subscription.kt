package com.grappim.wallosmobile.feature.subscriptions.domain.model

import kotlinx.datetime.LocalDate

/**
 * One subscription, as the list and detail screens need it (plan §7.1).
 *
 * Narrower than `SubscriptionDTO` on purpose: the notification, replacement and household-id
 * columns exist on the wire but nothing in v1 renders them, so they arrive with the screen
 * that wants them.
 *
 * @param logo the bare filename the server stores, empty when there is none. The full URL is
 *   `{base}/images/uploads/logos/{logo}` (API doc §4) and needs the instance URL, which this
 *   module has no business knowing.
 * @param currencySymbol resolved from [currencyId] against `get_currencies.php` by the repository,
 *   because a price is unrenderable without it (plan §7.1). Blank when the instance's currency
 *   list has no such id — the screen then shows the bare number rather than the wrong sign.
 * @param cycle `null` when the instance sent a code this build doesn't know — see
 *   [BillingCycle.fromCode].
 * @param nextPayment nullable like the other dates because the server sends `""` for an unset
 *   one and a malformed date must not sink the whole list.
 * @param categoryName resolved server-side, and never blank in practice: unmatched ids come
 *   back as `"No category"` / `"Unknown member"` / `"Unknown payment method"`.
 * @param categoryId the raw id `categoryName` was resolved from, `null` when unset. Added in 7.7
 *   purely so the editor can pre-fill its category picker from the row the detail screen already
 *   has loaded, without a second round trip (2.1's "domain model only what the screen renders",
 *   now that a screen renders it) — [paymentMethodId], [payerUserId], [autoRenew], [notify] and
 *   [notifyDaysBefore] exist for the same reason and no other.
 */
data class Subscription(
    val id: Int,
    val name: String,
    val logo: String,
    val price: Double,
    val currencyId: Int,
    val currencySymbol: String,
    val cycle: BillingCycle?,
    val frequency: Int,
    val nextPayment: LocalDate?,
    val startDate: LocalDate?,
    val isActive: Boolean,
    val notes: String,
    val url: String,
    val categoryName: String,
    val paymentMethodName: String,
    val payerName: String,
    val categoryId: Int?,
    val paymentMethodId: Int?,
    val payerUserId: Int?,
    val autoRenew: Boolean,
    val notify: Boolean,
    val notifyDaysBefore: Int?
)

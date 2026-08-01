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
 * @param cycle `null` when the instance sent a code this build doesn't know — see
 *   [BillingCycle.fromCode].
 * @param nextPayment nullable like the other dates because the server sends `""` for an unset
 *   one and a malformed date must not sink the whole list.
 * @param categoryName resolved server-side, and never blank in practice: unmatched ids come
 *   back as `"No category"` / `"Unknown member"` / `"Unknown payment method"`.
 */
data class Subscription(
    val id: Int,
    val name: String,
    val logo: String,
    val price: Double,
    val currencyId: Int,
    val cycle: BillingCycle?,
    val frequency: Int,
    val nextPayment: LocalDate?,
    val startDate: LocalDate?,
    val isActive: Boolean,
    val notes: String,
    val url: String,
    val categoryName: String,
    val paymentMethodName: String,
    val payerName: String
)

package com.grappim.wallosmobile.feature.subscriptions.domain.model

import kotlinx.datetime.LocalDate

/**
 * What `set_subscriptions.php`'s `action=add` needs (`WALLOS_API.md` §3.4). Only the six fields
 * the server itself refuses to add without are non-nullable; [autoRenew] and [inactive] mirror
 * the server's own defaults (`1` and `0`) rather than being optional, since the request always
 * carries a value for them either way.
 */
data class AddSubscriptionParams(
    val name: String,
    val price: Double,
    val currencyId: Int,
    val cycle: WritableBillingCycle,
    val frequency: Int,
    val nextPayment: LocalDate,
    val startDate: LocalDate? = null,
    val autoRenew: Boolean = true,
    val paymentMethodId: Int? = null,
    val payerUserId: Int? = null,
    val categoryId: Int? = null,
    val notes: String? = null,
    val url: String? = null,
    val notify: Boolean? = null,
    val notifyDaysBefore: Int? = null,
    val inactive: Boolean = false,
    val cancellationDate: LocalDate? = null,
    val replacementSubscriptionId: Int? = null,
    val logoUrl: String? = null
)

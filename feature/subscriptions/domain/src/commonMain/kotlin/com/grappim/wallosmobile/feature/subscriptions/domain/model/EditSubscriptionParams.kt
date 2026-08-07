package com.grappim.wallosmobile.feature.subscriptions.domain.model

import kotlinx.datetime.LocalDate

/**
 * What `set_subscriptions.php`'s `action=edit` accepts — every field optional, since the server
 * keeps a subscription's current value for anything the request omits (`WALLOS_API.md` §3.4).
 */
data class EditSubscriptionParams(
    val name: String? = null,
    val price: Double? = null,
    val currencyId: Int? = null,
    val cycle: WritableBillingCycle? = null,
    val frequency: Int? = null,
    val nextPayment: LocalDate? = null,
    val startDate: LocalDate? = null,
    val autoRenew: Boolean? = null,
    val paymentMethodId: Int? = null,
    val payerUserId: Int? = null,
    val categoryId: Int? = null,
    val notes: String? = null,
    val url: String? = null,
    val notify: Boolean? = null,
    val notifyDaysBefore: Int? = null,
    val inactive: Boolean? = null,
    val cancellationDate: LocalDate? = null,
    val replacementSubscriptionId: Int? = null,
    val logoUrl: String? = null,
    val logoFile: LogoFile? = null
)

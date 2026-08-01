package com.grappim.wallosmobile.feature.subscriptions.ui.detail

import com.grappim.wallosmobile.feature.subscriptions.domain.model.BillingCycle
import com.grappim.wallosmobile.utils.ui.NativeText

/**
 * Every field is already the string the screen draws, except [cycle]/[frequency] — "every 6
 * months" is a plural and can only be resolved where `pluralStringResource` can be called, so the
 * enum travels instead of the text (same reason as on the list, 2.4).
 *
 * A blank field means the instance has nothing for it, and the screen leaves the row out rather
 * than showing an empty label. That is the only reason the resolved `*Name` fields are plain
 * strings: Wallos answers `"No category"` itself for an unmatched id, so there is no "unset" to
 * describe.
 */
data class SubscriptionDetailUiItem(
    val name: String,
    val logoUrl: String,
    val price: String,
    val cycle: BillingCycle?,
    val frequency: Int,
    val nextPayment: String,
    val startDate: String,
    val categoryName: String,
    val paymentMethodName: String,
    val payerName: String,
    val notes: String,
    val url: String,
    val isActive: Boolean
)

/**
 * [subscription] is null until the row arrives and again after a failure — with no cache there is
 * nothing behind the error worth keeping (2.4), and here it also keeps the top bar from holding
 * the name of a row the screen can no longer show.
 */
data class SubscriptionDetailUiState(
    val subscription: SubscriptionDetailUiItem? = null,
    val isLoading: Boolean = false,
    val error: NativeText = NativeText.Empty,
    val onRetryClick: () -> Unit = {}
)

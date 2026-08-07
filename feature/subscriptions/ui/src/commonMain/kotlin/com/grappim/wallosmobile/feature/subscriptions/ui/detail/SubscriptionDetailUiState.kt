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
 *
 * @param convertedFrom names the currency [price] was converted **from**, blank when it wasn't
 *   converted at all (5.4). The amount itself is unrecoverable — the server overwrites `price` and
 *   keeps the source `currency_id` (3.11) — so this is a label, never a second number.
 * @param logoRefreshToken bumped by every refresh that succeeds (5.6) — same reason as the list's
 *   `SubscriptionUiItem.logoRefreshToken`.
 */
data class SubscriptionDetailUiItem(
    val name: String,
    val logoUrl: String,
    val price: String,
    val convertedFrom: String,
    val cycle: BillingCycle?,
    val frequency: Int,
    val nextPayment: String,
    val startDate: String,
    val categoryName: String,
    val paymentMethodName: String,
    val payerName: String,
    val notes: String,
    val url: String,
    val isActive: Boolean,
    val logoRefreshToken: Int = 0
)

/**
 * [subscription] is whatever the cache holds for this id (3.4): null until a refresh has put the
 * row there, and null again if a later list refresh dropped it. A *failed* refresh no longer
 * clears it — the cached row is real, which is what 2.5 had nothing of.
 *
 * The delete flow (7.7) gets its own [deleteError] rather than reusing [error]: the two can be
 * true at once (a stale row behind a failed delete attempt), and [isStale]/[isFailed] would
 * misread a delete failure as a load failure if they shared one field.
 */
data class SubscriptionDetailUiState(
    val subscription: SubscriptionDetailUiItem? = null,
    val isLoading: Boolean = false,
    val error: NativeText = NativeText.Empty,
    val onRetryClick: () -> Unit = {},
    val isDeleteDialogOpen: Boolean = false,
    val isDeleting: Boolean = false,
    val deleteError: NativeText = NativeText.Empty,
    val onDeleteClick: () -> Unit = {},
    val onDeleteDialogDismiss: () -> Unit = {},
    val onDeleteConfirm: () -> Unit = {}
) {

    /** A row the last refresh couldn't confirm: a banner over it, never instead of it (3.5). */
    val isStale: Boolean
        get() = error.isNotEmpty() && subscription != null

    /** The error owns the screen only when there is no cached row behind it. */
    val isFailed: Boolean
        get() = error.isNotEmpty() && subscription == null
}

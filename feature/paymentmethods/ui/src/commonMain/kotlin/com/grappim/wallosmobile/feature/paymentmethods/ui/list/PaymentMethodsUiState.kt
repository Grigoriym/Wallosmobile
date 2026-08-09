package com.grappim.wallosmobile.feature.paymentmethods.ui.list

import com.grappim.wallosmobile.utils.ui.NativeText
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * One row. [iconUrl] is already the full URL (`BaseUrlProvider.toIconUrl`), blank when the method
 * has no icon; `inUse` never reaches here — it belongs to the delete flow (the editor), not the
 * list.
 */
data class PaymentMethodUiItem(val id: Int, val name: String, val iconUrl: String, val enabled: Boolean)

/**
 * No cache behind this list (this milestone's preamble, mirroring `HouseholdUiState`'s own
 * reasoning) — every round trip is live, so there is no "stale behind cached rows" state, only
 * [isLoading] versus a plain [error].
 */
data class PaymentMethodsUiState(
    val items: ImmutableList<PaymentMethodUiItem> = persistentListOf(),
    val isLoading: Boolean = false,
    val error: NativeText = NativeText.Empty,
    val onRetryClick: () -> Unit = {}
) {

    /** The error owns the screen only when there is nothing loaded to show behind it. */
    val isFailed: Boolean
        get() = error.isNotEmpty() && items.isEmpty()

    val isEmpty: Boolean
        get() = !isLoading && error.isEmpty() && items.isEmpty()
}

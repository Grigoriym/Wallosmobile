package com.grappim.wallosmobile.feature.household.ui.list

import com.grappim.wallosmobile.utils.ui.NativeText
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** One row: `inUse` never reaches here — it belongs to the delete flow (the editor), not the list. */
data class HouseholdMemberUiItem(val id: Int, val name: String, val email: String)

/**
 * No cache behind this list (this milestone's preamble, mirroring `CategoriesUiState`'s own
 * reasoning) — every round trip is live, so there is no "stale behind cached rows" state, only
 * [isLoading] versus a plain [error].
 */
data class HouseholdUiState(
    val items: ImmutableList<HouseholdMemberUiItem> = persistentListOf(),
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

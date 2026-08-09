package com.grappim.wallosmobile.feature.categories.ui.list

import com.grappim.wallosmobile.utils.ui.NativeText
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** One row: `inUse` never reaches here — it belongs to the delete flow (the editor), not the list. */
data class CategoryUiItem(val id: Int, val name: String)

/**
 * No cache behind this list (this milestone's preamble, mirroring 7.2's own reasoning for the
 * editor's pickers) — every round trip is live, so unlike `SubscriptionsUiState` there is no
 * "stale behind cached rows" state, only [isLoading] versus a plain [error].
 */
data class CategoriesUiState(
    val items: ImmutableList<CategoryUiItem> = persistentListOf(),
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

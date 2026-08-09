package com.grappim.wallosmobile.feature.currencies.ui.list

import com.grappim.wallosmobile.utils.ui.NativeText
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * One row. `inUse` never reaches here — it belongs to the delete flow (the editor), not the list,
 * same as `CategoryUiItem`/`HouseholdMemberUiItem`. [isMain] does reach here: this step's own text
 * calls out marking the main currency in the list as the one real choice for this screen, unlike
 * the other three catalog resources.
 */
data class CurrencyUiItem(
    val id: Int,
    val name: String,
    val symbol: String,
    val code: String,
    val rate: Double,
    val isMain: Boolean
)

/**
 * No cache behind this list (this milestone's preamble, mirroring `CategoriesUiState`'s own
 * reasoning) — every round trip is live, so there is no "stale behind cached rows" state, only
 * [isLoading] versus a plain [error].
 */
data class CurrenciesUiState(
    val items: ImmutableList<CurrencyUiItem> = persistentListOf(),
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

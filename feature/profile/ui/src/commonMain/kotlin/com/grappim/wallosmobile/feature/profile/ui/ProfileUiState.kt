package com.grappim.wallosmobile.feature.profile.ui

import com.grappim.wallosmobile.utils.ui.NativeText

/**
 * No cache behind `get_user`/`set_budget` (mirrors the four M9 catalog screens' own reasoning) —
 * [isLoading]/[error] cover the fetch, [isSaving]/[saveError] the write; kept apart because a save
 * failure must never blank out fields the fetch already filled in. [budget]/[periodBudget] are
 * `String`s, the same shape `CurrencyEditorUiState.rate` uses for a decimal field — parsed to a
 * `Double` only on save. `budgetPeriodType`/`budgetPeriodAnchorDate` aren't in this state at all:
 * this step's own text is "budget and period budget, editable" only, so the period fields the API
 * requires alongside them ride through the ViewModel unchanged rather than becoming a second form.
 */
data class ProfileUiState(
    val isLoading: Boolean = false,
    val error: NativeText = NativeText.Empty,
    val onRetryClick: () -> Unit = {},
    val budget: String = "",
    val onBudgetChange: (String) -> Unit = {},
    val periodBudget: String = "",
    val onPeriodBudgetChange: (String) -> Unit = {},
    val isSaving: Boolean = false,
    val saveError: NativeText = NativeText.Empty,
    val onSaveClick: () -> Unit = {}
) {

    /** The error owns the screen only when there is nothing loaded to show behind it. */
    val isFailed: Boolean
        get() = error.isNotEmpty() && budget.isEmpty()
}

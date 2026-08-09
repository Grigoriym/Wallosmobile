package com.grappim.wallosmobile.feature.categories.ui.editor

import com.grappim.wallosmobile.utils.ui.NativeText

/**
 * The add/edit form and its delete flow together — a category is one field, so unlike
 * `SubscriptionEditorUiState`/`SubscriptionDetailUiState` there is no reason to split them across
 * two screens. [deleteError] is separate from [error] for the same reason 7.7's detail screen keeps
 * them apart: a save failure and a delete failure can't both be read off one field.
 */
data class CategoryEditorUiState(
    val name: String = "",
    val onNameChange: (String) -> Unit = {},
    val isSaving: Boolean = false,
    val error: NativeText = NativeText.Empty,
    val onSaveClick: () -> Unit = {},
    val isDeleteDialogOpen: Boolean = false,
    val isDeleting: Boolean = false,
    val deleteError: NativeText = NativeText.Empty,
    val onDeleteClick: () -> Unit = {},
    val onDeleteDialogDismiss: () -> Unit = {},
    val onDeleteConfirm: () -> Unit = {}
)

package com.grappim.wallosmobile.feature.household.ui.editor

import com.grappim.wallosmobile.utils.ui.NativeText

/**
 * The add/edit form and its delete flow together — mirroring `CategoryEditorUiState`. [deleteError]
 * is separate from [error] for the same reason: a save failure and a delete failure can't both be
 * read off one field.
 */
data class HouseholdMemberEditorUiState(
    val name: String = "",
    val onNameChange: (String) -> Unit = {},
    val email: String = "",
    val onEmailChange: (String) -> Unit = {},
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

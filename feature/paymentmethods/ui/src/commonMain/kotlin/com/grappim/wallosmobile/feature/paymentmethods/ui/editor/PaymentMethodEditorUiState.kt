package com.grappim.wallosmobile.feature.paymentmethods.ui.editor

import com.grappim.wallosmobile.utils.ui.NativeText

/**
 * The add/edit form and its delete flow together — mirroring `HouseholdMemberEditorUiState`.
 * [deleteError] is separate from [error] for the same reason: a save failure and a delete failure
 * can't both be read off one field.
 */
data class PaymentMethodEditorUiState(
    val name: String = "",
    val onNameChange: (String) -> Unit = {},
    val enabled: Boolean = true,
    val onEnabledChange: (Boolean) -> Unit = {},
    val iconUrl: String = "",
    val onIconUrlChange: (String) -> Unit = {},
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

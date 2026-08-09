package com.grappim.wallosmobile.feature.household.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.wallosmobile.core.logger.LogPriority
import com.grappim.wallosmobile.core.logger.logcat
import com.grappim.wallosmobile.feature.household.domain.repo.HouseholdRepository
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.member_editor_error_invalid
import com.grappim.wallosmobile.utils.ui.NativeText
import com.grappim.wallosmobile.utils.ui.getErrorMessage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

/**
 * Add- or edit- depending on [memberId] (`null` from the list's FAB, set from a row tap) — see
 * [HouseholdMemberEditorRoute]. [initialName]/[initialEmail] arrive from the route rather than a
 * fetch: `getMembers()` is the only read this repository has, and the list screen the user just
 * came from already holds the row.
 */
@KoinViewModel
class HouseholdMemberEditorViewModel(
    @InjectedParam private val memberId: Int?,
    @InjectedParam private val initialName: String,
    @InjectedParam private val initialEmail: String,
    private val householdRepository: HouseholdRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HouseholdMemberEditorUiState(
            name = initialName,
            onNameChange = ::onNameChange,
            email = initialEmail,
            onEmailChange = ::onEmailChange,
            onSaveClick = ::onSaveClick,
            onDeleteClick = ::onDeleteClick,
            onDeleteDialogDismiss = ::onDeleteDialogDismiss,
            onDeleteConfirm = ::onDeleteConfirm
        )
    )
    val uiState: StateFlow<HouseholdMemberEditorUiState> = _uiState.asStateFlow()

    /** One-off, per plan: a successful save is a signal the screen acts on, never UI state. */
    private val _saved = Channel<Unit>()
    val saved = _saved.receiveAsFlow()

    /** One-off, per plan: a successful delete is a signal the screen acts on, never UI state. */
    private val _deleted = Channel<Unit>()
    val deleted = _deleted.receiveAsFlow()

    private fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value) }
    }

    private fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value) }
    }

    private fun onSaveClick() {
        val name = _uiState.value.name.trim()
        if (name.isBlank()) {
            _uiState.update { it.copy(error = NativeText.Resource(RString.member_editor_error_invalid)) }
            return
        }
        val email = _uiState.value.email.trim()

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = NativeText.Empty) }

            val result = memberId?.let { id -> householdRepository.editMember(id, name, email) }
                ?: householdRepository.addMember(name, email).map { }

            result.onSuccess {
                _uiState.update { it.copy(isSaving = false) }
                _saved.send(Unit)
            }.onFailure { throwable ->
                val action = if (memberId != null) "Editing" else "Adding"
                logcat(priority = LogPriority.WARN, throwable = throwable) { "$action household member failed" }
                _uiState.update { it.copy(isSaving = false, error = getErrorMessage(throwable)) }
            }
        }
    }

    private fun onDeleteClick() {
        _uiState.update { it.copy(isDeleteDialogOpen = true) }
    }

    private fun onDeleteDialogDismiss() {
        _uiState.update { it.copy(isDeleteDialogOpen = false) }
    }

    private fun onDeleteConfirm() {
        val id = memberId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, deleteError = NativeText.Empty) }

            householdRepository.deleteMember(id)
                .onSuccess {
                    _uiState.update { it.copy(isDeleting = false, isDeleteDialogOpen = false) }
                    _deleted.send(Unit)
                }
                .onFailure { throwable ->
                    logcat(priority = LogPriority.WARN, throwable = throwable) {
                        "Deleting household member $id failed"
                    }
                    _uiState.update { it.copy(isDeleting = false, deleteError = getErrorMessage(throwable)) }
                }
        }
    }
}

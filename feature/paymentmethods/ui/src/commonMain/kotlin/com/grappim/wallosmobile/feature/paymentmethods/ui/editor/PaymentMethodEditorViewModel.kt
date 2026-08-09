package com.grappim.wallosmobile.feature.paymentmethods.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.wallosmobile.core.logger.LogPriority
import com.grappim.wallosmobile.core.logger.logcat
import com.grappim.wallosmobile.feature.paymentmethods.domain.model.IconFile
import com.grappim.wallosmobile.feature.paymentmethods.domain.repo.PaymentMethodsRepository
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.payment_method_editor_error_invalid
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
 * Add- or edit- depending on [paymentMethodId] (`null` from the list's FAB, set from a row tap) —
 * see [PaymentMethodEditorRoute]. [initialName]/[initialEnabled] arrive from the route rather than
 * a fetch: `getPaymentMethods()` is the only read this repository has, and the list screen the
 * user just came from already holds the row. [iconUrl][PaymentMethodEditorUiState.iconUrl] always
 * starts blank — see the route's own doc comment for why that's correct on both add and edit.
 */
@KoinViewModel
class PaymentMethodEditorViewModel(
    @InjectedParam private val paymentMethodId: Int?,
    @InjectedParam private val initialName: String,
    @InjectedParam private val initialEnabled: Boolean,
    private val paymentMethodsRepository: PaymentMethodsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        PaymentMethodEditorUiState(
            name = initialName,
            onNameChange = ::onNameChange,
            enabled = initialEnabled,
            onEnabledChange = ::onEnabledChange,
            onIconUrlChange = ::onIconUrlChange,
            onIconFilePick = ::onIconFilePick,
            onSaveClick = ::onSaveClick,
            onDeleteClick = ::onDeleteClick,
            onDeleteDialogDismiss = ::onDeleteDialogDismiss,
            onDeleteConfirm = ::onDeleteConfirm
        )
    )
    val uiState: StateFlow<PaymentMethodEditorUiState> = _uiState.asStateFlow()

    /** One-off, per plan: a successful save is a signal the screen acts on, never UI state. */
    private val _saved = Channel<Unit>()
    val saved = _saved.receiveAsFlow()

    /** One-off, per plan: a successful delete is a signal the screen acts on, never UI state. */
    private val _deleted = Channel<Unit>()
    val deleted = _deleted.receiveAsFlow()

    private fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value) }
    }

    private fun onEnabledChange(value: Boolean) {
        _uiState.update { it.copy(enabled = value) }
    }

    private fun onIconUrlChange(value: String) {
        _uiState.update { it.copy(iconUrl = value) }
    }

    private fun onIconFilePick(file: IconFile) {
        _uiState.update { it.copy(iconFile = file) }
    }

    private fun onSaveClick() {
        val name = _uiState.value.name.trim()
        if (name.isBlank()) {
            _uiState.update { it.copy(error = NativeText.Resource(RString.payment_method_editor_error_invalid)) }
            return
        }
        val enabled = _uiState.value.enabled
        val iconUrl = _uiState.value.iconUrl.trim().ifBlank { null }
        val iconFile = _uiState.value.iconFile

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = NativeText.Empty) }

            val result = paymentMethodId?.let { id ->
                paymentMethodsRepository.editPaymentMethod(id, name, enabled, iconUrl, iconFile)
            } ?: paymentMethodsRepository.addPaymentMethod(name, enabled, iconUrl, iconFile).map { }

            result.onSuccess {
                _uiState.update { it.copy(isSaving = false) }
                _saved.send(Unit)
            }.onFailure { throwable ->
                val action = if (paymentMethodId != null) "Editing" else "Adding"
                logcat(priority = LogPriority.WARN, throwable = throwable) { "$action payment method failed" }
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
        val id = paymentMethodId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, deleteError = NativeText.Empty) }

            paymentMethodsRepository.deletePaymentMethod(id)
                .onSuccess {
                    _uiState.update { it.copy(isDeleting = false, isDeleteDialogOpen = false) }
                    _deleted.send(Unit)
                }
                .onFailure { throwable ->
                    logcat(priority = LogPriority.WARN, throwable = throwable) {
                        "Deleting payment method $id failed"
                    }
                    _uiState.update { it.copy(isDeleting = false, deleteError = getErrorMessage(throwable)) }
                }
        }
    }
}

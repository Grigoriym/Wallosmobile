package com.grappim.wallosmobile.feature.currencies.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.wallosmobile.core.logger.LogPriority
import com.grappim.wallosmobile.core.logger.logcat
import com.grappim.wallosmobile.feature.currencies.domain.repo.CurrenciesRepository
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.currency_editor_error_invalid
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
 * Add- or edit- depending on [currencyId] (`null` from the list's FAB, set from a row tap) — see
 * [CurrencyEditorRoute]. [initialName]/[initialSymbol]/[initialCode]/[initialRate] arrive from the
 * route rather than a fetch: `getCurrencies()` is the only read this repository has, and the list
 * screen the user just came from already holds the row.
 */
@KoinViewModel
class CurrencyEditorViewModel(
    @InjectedParam private val currencyId: Int?,
    @InjectedParam private val initialName: String,
    @InjectedParam private val initialSymbol: String,
    @InjectedParam private val initialCode: String,
    @InjectedParam private val initialRate: Double,
    private val currenciesRepository: CurrenciesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CurrencyEditorUiState(
            name = initialName,
            onNameChange = ::onNameChange,
            symbol = initialSymbol,
            onSymbolChange = ::onSymbolChange,
            code = initialCode,
            onCodeChange = ::onCodeChange,
            rate = initialRate.toString(),
            onRateChange = ::onRateChange,
            onSaveClick = ::onSaveClick,
            onDeleteClick = ::onDeleteClick,
            onDeleteDialogDismiss = ::onDeleteDialogDismiss,
            onDeleteConfirm = ::onDeleteConfirm
        )
    )
    val uiState: StateFlow<CurrencyEditorUiState> = _uiState.asStateFlow()

    /** One-off, per plan: a successful save is a signal the screen acts on, never UI state. */
    private val _saved = Channel<Unit>()
    val saved = _saved.receiveAsFlow()

    /** One-off, per plan: a successful delete is a signal the screen acts on, never UI state. */
    private val _deleted = Channel<Unit>()
    val deleted = _deleted.receiveAsFlow()

    private fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value) }
    }

    private fun onSymbolChange(value: String) {
        _uiState.update { it.copy(symbol = value) }
    }

    private fun onCodeChange(value: String) {
        _uiState.update { it.copy(code = value) }
    }

    private fun onRateChange(value: String) {
        _uiState.update { it.copy(rate = value) }
    }

    private fun onSaveClick() {
        val state = _uiState.value
        val name = state.name.trim()
        val symbol = state.symbol.trim()
        val code = state.code.trim()
        val rate = state.rate.toDoubleOrNull()

        if (name.isBlank() || symbol.isBlank() || code.isBlank() || rate == null) {
            _uiState.update { it.copy(error = NativeText.Resource(RString.currency_editor_error_invalid)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = NativeText.Empty) }

            val result = currencyId?.let { id -> currenciesRepository.editCurrency(id, name, symbol, code, rate) }
                ?: currenciesRepository.addCurrency(name, symbol, code, rate).map { }

            result.onSuccess {
                _uiState.update { it.copy(isSaving = false) }
                _saved.send(Unit)
            }.onFailure { throwable ->
                val action = if (currencyId != null) "Editing" else "Adding"
                logcat(priority = LogPriority.WARN, throwable = throwable) { "$action currency failed" }
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
        val id = currencyId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, deleteError = NativeText.Empty) }

            currenciesRepository.deleteCurrency(id)
                .onSuccess {
                    _uiState.update { it.copy(isDeleting = false, isDeleteDialogOpen = false) }
                    _deleted.send(Unit)
                }
                .onFailure { throwable ->
                    logcat(priority = LogPriority.WARN, throwable = throwable) { "Deleting currency $id failed" }
                    _uiState.update { it.copy(isDeleting = false, deleteError = getErrorMessage(throwable)) }
                }
        }
    }
}

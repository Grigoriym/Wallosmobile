package com.grappim.wallosmobile.feature.currencies.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.wallosmobile.core.logger.LogPriority
import com.grappim.wallosmobile.core.logger.logcat
import com.grappim.wallosmobile.feature.currencies.domain.repo.CurrenciesRepository
import com.grappim.wallosmobile.utils.ui.NativeText
import com.grappim.wallosmobile.utils.ui.getErrorMessage
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

/**
 * Deliberately no `init { load() }`, mirroring `CategoriesViewModel`/`HouseholdViewModel`: this
 * list has no cache to fall back on (this milestone's preamble), and it needs to reload after a
 * return trip from [com.grappim.wallosmobile.feature.currencies.ui.editor.CurrencyEditorRoute] as
 * much as on first open. Nav3 disposes a covered entry's composition and restarts it once it's on
 * top again, so [CurrenciesUiState.onRetryClick], fired from the screen's own `LaunchedEffect`, is
 * the single load path for both — see `CurrenciesScreen`.
 */
@KoinViewModel
class CurrenciesViewModel(private val currenciesRepository: CurrenciesRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CurrenciesUiState(onRetryClick = ::load))
    val uiState: StateFlow<CurrenciesUiState> = _uiState.asStateFlow()

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = NativeText.Empty) }

            currenciesRepository.getCurrencies()
                .onSuccess { currencies ->
                    val items = currencies.map {
                        CurrencyUiItem(
                            id = it.id,
                            name = it.name,
                            symbol = it.symbol,
                            code = it.code,
                            rate = it.rate,
                            isMain = it.isMain
                        )
                    }
                    _uiState.update { it.copy(isLoading = false, items = items.toPersistentList()) }
                }
                .onFailure { throwable ->
                    logcat(priority = LogPriority.WARN, throwable = throwable) { "Loading currencies failed" }
                    _uiState.update { it.copy(isLoading = false, error = getErrorMessage(throwable)) }
                }
        }
    }
}

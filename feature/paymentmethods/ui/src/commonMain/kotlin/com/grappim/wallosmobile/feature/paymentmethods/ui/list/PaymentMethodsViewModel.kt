package com.grappim.wallosmobile.feature.paymentmethods.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.wallosmobile.core.api.BaseUrlProvider
import com.grappim.wallosmobile.core.logger.LogPriority
import com.grappim.wallosmobile.core.logger.logcat
import com.grappim.wallosmobile.feature.paymentmethods.domain.repo.PaymentMethodsRepository
import com.grappim.wallosmobile.feature.paymentmethods.ui.toIconUrl
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
 * Deliberately no `init { load() }`, mirroring `HouseholdViewModel`: this list has no cache to
 * fall back on (this milestone's preamble), and it needs to reload after a return trip from
 * [com.grappim.wallosmobile.feature.paymentmethods.ui.editor.PaymentMethodEditorRoute] as much as
 * on first open. Nav3 disposes a covered entry's composition and restarts it once it's on top
 * again, so [PaymentMethodsUiState.onRetryClick], fired from the screen's own `LaunchedEffect`, is
 * the single load path for both — see `PaymentMethodsScreen`.
 */
@KoinViewModel
class PaymentMethodsViewModel(
    private val paymentMethodsRepository: PaymentMethodsRepository,
    private val baseUrlProvider: BaseUrlProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentMethodsUiState(onRetryClick = ::load))
    val uiState: StateFlow<PaymentMethodsUiState> = _uiState.asStateFlow()

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = NativeText.Empty) }

            paymentMethodsRepository.getPaymentMethods()
                .onSuccess { methods ->
                    val items = methods.map { method ->
                        PaymentMethodUiItem(
                            id = method.id,
                            name = method.name,
                            iconUrl = baseUrlProvider.toIconUrl(method.icon),
                            enabled = method.enabled
                        )
                    }
                    _uiState.update { it.copy(isLoading = false, items = items.toPersistentList()) }
                }
                .onFailure { throwable ->
                    logcat(priority = LogPriority.WARN, throwable = throwable) { "Loading payment methods failed" }
                    _uiState.update { it.copy(isLoading = false, error = getErrorMessage(throwable)) }
                }
        }
    }
}

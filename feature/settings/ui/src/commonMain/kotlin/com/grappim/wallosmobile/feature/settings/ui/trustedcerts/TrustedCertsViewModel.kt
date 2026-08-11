package com.grappim.wallosmobile.feature.settings.ui.trustedcerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.wallosmobile.core.domain.PendingCertTrust
import com.grappim.wallosmobile.core.domain.resultOf
import com.grappim.wallosmobile.core.logger.LogPriority
import com.grappim.wallosmobile.core.logger.logcat
import com.grappim.wallosmobile.core.storage.cert.TrustedCertStorage
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

/**
 * Takes [TrustedCertStorage] straight, with no repository between — same single-seam case as
 * `StartDestinationViewModel`.
 */
@KoinViewModel
class TrustedCertsViewModel(private val trustedCertStorage: TrustedCertStorage) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TrustedCertsUiState(
            onRevokeClick = ::onRevokeClick,
            onRevokeConfirm = ::onRevokeConfirm,
            onRevokeDialogDismiss = ::onRevokeDialogDismiss
        )
    )
    val uiState: StateFlow<TrustedCertsUiState> = _uiState.asStateFlow()

    init {
        trustedCertStorage.getAllFlow()
            .onEach { certs -> _uiState.update { it.copy(certs = certs.toPersistentList()) } }
            .launchIn(viewModelScope)
    }

    private fun onRevokeClick(cert: PendingCertTrust) {
        _uiState.update { it.copy(certPendingRevoke = cert) }
    }

    private fun onRevokeDialogDismiss() {
        _uiState.update { it.copy(certPendingRevoke = null) }
    }

    private fun onRevokeConfirm() {
        val cert = _uiState.value.certPendingRevoke ?: return
        viewModelScope.launch {
            // A failed revoke leaves the pin in the list, which is the honest outcome — the flow
            // above never emits, so the screen keeps showing what is actually stored.
            resultOf { trustedCertStorage.untrust(cert.host, cert.sha256Fingerprint) }
                .onFailure {
                    logcat(priority = LogPriority.WARN, throwable = it) { "Revoking a trusted certificate failed" }
                }
            _uiState.update { it.copy(certPendingRevoke = null) }
        }
    }
}

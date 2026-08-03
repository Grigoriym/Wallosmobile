package com.grappim.wallosmobile.feature.setup.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.wallosmobile.core.domain.PendingCertTrust
import com.grappim.wallosmobile.core.domain.findPendingCertTrust
import com.grappim.wallosmobile.core.logger.LogPriority
import com.grappim.wallosmobile.core.logger.logcat
import com.grappim.wallosmobile.feature.setup.domain.model.ApiKeyNotFound
import com.grappim.wallosmobile.feature.setup.domain.model.LoginOutcome
import com.grappim.wallosmobile.feature.setup.domain.repo.SetupRepository
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.login_error_api_key_missing
import com.grappim.wallosmobile.strings.generated.resources.login_error_cert_not_trusted
import com.grappim.wallosmobile.strings.generated.resources.login_error_invalid_credentials
import com.grappim.wallosmobile.strings.generated.resources.login_error_invalid_totp
import com.grappim.wallosmobile.strings.generated.resources.login_error_totp_session_expired
import com.grappim.wallosmobile.utils.ui.NativeText
import com.grappim.wallosmobile.utils.ui.getErrorMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.koin.core.annotation.KoinViewModel

/**
 * The repository is a `@Factory` (plan §1.1) — it owns the web session transitively — so this
 * ViewModel holds one onboarding session for as long as the screen lives, and a new attempt after
 * a process death gets a clean cookie jar.
 */
@KoinViewModel
class LoginViewModel(private val setupRepository: SetupRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LoginUiState(
            onServerUrlChange = ::onServerUrlChange,
            onUsernameChange = ::onUsernameChange,
            onPasswordChange = ::onPasswordChange,
            onApiKeyChange = ::onApiKeyChange,
            onTotpCodeChange = ::onTotpCodeChange,
            onPasswordVisibilityChange = ::onPasswordVisibilityChange,
            onApiKeyModeChange = ::onApiKeyModeChange,
            onConnectClick = ::onConnectClick,
            onCertTrustConfirm = ::onCertTrustConfirm,
            onCertTrustDismiss = ::onCertTrustDismiss
        )
    )
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            setupRepository.getStoredServerUrl()
                .onSuccess(::onStoredServerUrl)
                // No prefill is a worse screen, not a broken one — the user types the URL as before.
                .onFailure { logcat(priority = LogPriority.WARN, throwable = it) { "No stored server URL" } }
        }
    }

    /**
     * Disconnect keeps the URL (plan §4.7) and this is what finally offers it back. The read lands
     * a beat after construction, so anything the user has already typed wins over it.
     */
    private fun onStoredServerUrl(url: String) {
        if (url.isEmpty()) return
        _uiState.update { if (it.serverUrl.isEmpty()) it.copy(serverUrl = url) else it }
    }

    private fun onServerUrlChange(url: String) {
        _uiState.update { it.copy(serverUrl = url, error = NativeText.Empty) }
    }

    private fun onUsernameChange(username: String) {
        _uiState.update { it.copy(username = username, error = NativeText.Empty) }
    }

    private fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, error = NativeText.Empty) }
    }

    private fun onApiKeyChange(apiKey: String) {
        _uiState.update { it.copy(apiKey = apiKey, error = NativeText.Empty) }
    }

    private fun onTotpCodeChange(code: String) {
        _uiState.update { it.copy(totpCode = code, error = NativeText.Empty) }
    }

    private fun onPasswordVisibilityChange(isVisible: Boolean) {
        _uiState.update { it.copy(isPasswordVisible = isVisible) }
    }

    /**
     * Switching paths clears the error: it was attributed to fields the user can no longer see.
     * It also abandons a pending TOTP challenge — the key path never answers one, and leaving the
     * flag set would put the code field back the moment the user switched away again.
     */
    private fun onApiKeyModeChange(isApiKeyMode: Boolean) {
        _uiState.update {
            it.copy(
                isApiKeyMode = isApiKeyMode,
                isTotpRequired = false,
                totpCode = "",
                error = NativeText.Empty
            )
        }
    }

    private fun onConnectClick() {
        val state = _uiState.value
        if (!state.canConnect) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = NativeText.Empty) }

            when {
                state.isApiKeyMode ->
                    setupRepository.connectWithApiKey(state.serverUrl, state.apiKey)
                        .onSuccess { onConnected() }
                        .onFailure(::onFailure)

                // The session this answers belongs to the repository, so there is nothing to pass
                // it but the code — see `SetupRepository.submitTotpCode`.
                state.isTotpRequired ->
                    setupRepository.submitTotpCode(state.totpCode)
                        .onSuccess { onOutcome(it) }
                        .onFailure(::onFailure)

                else ->
                    setupRepository.loginWithPassword(state.serverUrl, state.username, state.password)
                        .onSuccess { onOutcome(it) }
                        .onFailure(::onFailure)
            }
        }
    }

    private fun onOutcome(outcome: LoginOutcome) {
        when (outcome) {
            LoginOutcome.Connected -> onConnected()

            LoginOutcome.NeedsTotp -> onTotpRequired()

            LoginOutcome.InvalidCredentials -> showError(RString.login_error_invalid_credentials)

            // The challenge stands, so the field stays: the next code off the authenticator is a
            // working answer to the session that just refused this one.
            LoginOutcome.InvalidTotpCode -> showError(RString.login_error_invalid_totp)

            LoginOutcome.TotpSessionExpired -> onTotpSessionExpired()
        }
    }

    /**
     * The password has already been accepted and is of no further use — the session carries the
     * challenge from here — so it goes now rather than waiting for [onConnected].
     */
    private fun onTotpRequired() {
        _uiState.update { it.copy(isLoading = false, isTotpRequired = true, password = "") }
    }

    /** No code can complete this attempt, so the screen has to go back to where one can start. */
    private fun onTotpSessionExpired() {
        _uiState.update { it.copy(isTotpRequired = false, totpCode = "") }
        showError(RString.login_error_totp_session_expired)
    }

    /**
     * The key is persisted by now, so `ApiKeyStorage.isConnected` has already flipped and the
     * shell is on its way in — this screen only has to stop looking busy and forget the secrets.
     */
    private fun onConnected() {
        _uiState.update { it.copy(isLoading = false, password = "", totpCode = "") }
    }

    private fun onFailure(throwable: Throwable) {
        // Nothing catches `UntrustedCertificateException` — it rides down the cause chain of
        // whatever the TLS stack threw, and this is where it is asked for (plan §4.5).
        val pendingCertTrust = throwable.findPendingCertTrust()
        if (pendingCertTrust != null) {
            onUntrustedCertificate(pendingCertTrust)
            return
        }

        // The login succeeded and the account simply has no key yet — a `getErrorMessage` about
        // the URL or the key would send the user to fix the one thing that was right.
        val message = if (throwable is ApiKeyNotFound) {
            NativeText.Resource(RString.login_error_api_key_missing)
        } else {
            getErrorMessage(throwable)
        }
        _uiState.update { it.copy(isLoading = false, error = message) }
    }

    /**
     * The prompt replaces the error rather than joining it: `getErrorMessage` would say "check the
     * URL and your connection", and the URL and the connection were both right.
     */
    private fun onUntrustedCertificate(pendingCertTrust: PendingCertTrust) {
        _uiState.update { it.copy(isLoading = false, pendingCertTrust = pendingCertTrust) }
    }

    /**
     * The retry is [onConnectClick] itself, not a captured lambda: both paths are driven from this
     * state, the dialog is modal so none of it can have changed, and the failed attempt stored
     * nothing — so re-reading the state re-runs exactly the request that raised the prompt.
     */
    private fun onCertTrustConfirm() {
        val pendingCertTrust = _uiState.value.pendingCertTrust ?: return
        _uiState.update { it.copy(pendingCertTrust = null) }

        viewModelScope.launch {
            setupRepository.trustCertificate(pendingCertTrust)
                .onSuccess { onConnectClick() }
                .onFailure {
                    // Retrying without the pin would only raise the same prompt again.
                    logcat(priority = LogPriority.WARN, throwable = it) { "Could not store the trust" }
                    showError(RString.login_error_cert_not_trusted)
                }
        }
    }

    /** Declining is a decision, not a non-event — Connect did nothing and has to say why. */
    private fun onCertTrustDismiss() {
        _uiState.update { it.copy(pendingCertTrust = null) }
        showError(RString.login_error_cert_not_trusted)
    }

    private fun showError(resource: StringResource) {
        _uiState.update { it.copy(isLoading = false, error = NativeText.Resource(resource)) }
    }
}

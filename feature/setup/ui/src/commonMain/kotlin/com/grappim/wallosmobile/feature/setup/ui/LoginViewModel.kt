package com.grappim.wallosmobile.feature.setup.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.wallosmobile.feature.setup.domain.model.ApiKeyNotFound
import com.grappim.wallosmobile.feature.setup.domain.model.LoginOutcome
import com.grappim.wallosmobile.feature.setup.domain.repo.SetupRepository
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.login_error_api_key_missing
import com.grappim.wallosmobile.strings.generated.resources.login_error_invalid_credentials
import com.grappim.wallosmobile.strings.generated.resources.login_error_needs_totp
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
            onPasswordVisibilityChange = ::onPasswordVisibilityChange,
            onApiKeyModeChange = ::onApiKeyModeChange,
            onConnectClick = ::onConnectClick
        )
    )
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

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

    private fun onPasswordVisibilityChange(isVisible: Boolean) {
        _uiState.update { it.copy(isPasswordVisible = isVisible) }
    }

    /** Switching paths clears the error: it was attributed to fields the user can no longer see. */
    private fun onApiKeyModeChange(isApiKeyMode: Boolean) {
        _uiState.update { it.copy(isApiKeyMode = isApiKeyMode, error = NativeText.Empty) }
    }

    private fun onConnectClick() {
        val state = _uiState.value
        if (!state.canConnect) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = NativeText.Empty) }

            if (state.isApiKeyMode) {
                setupRepository.connectWithApiKey(state.serverUrl, state.apiKey)
                    .onSuccess { onConnected() }
                    .onFailure(::onFailure)
            } else {
                setupRepository.loginWithPassword(state.serverUrl, state.username, state.password)
                    .onSuccess { onOutcome(it) }
                    .onFailure(::onFailure)
            }
        }
    }

    private fun onOutcome(outcome: LoginOutcome) {
        when (outcome) {
            LoginOutcome.Connected -> onConnected()

            // v1 doesn't drive `totp.php` (plan §7.2); the message sends the user to the key field,
            // which is why the reveal is on this screen and not behind a settings toggle.
            LoginOutcome.NeedsTotp -> showError(RString.login_error_needs_totp)

            LoginOutcome.InvalidCredentials -> showError(RString.login_error_invalid_credentials)
        }
    }

    /**
     * The key is persisted by now, so `ApiKeyStorage.isConnected` has already flipped and the
     * shell is on its way in — this screen only has to stop looking busy and forget the password.
     */
    private fun onConnected() {
        _uiState.update { it.copy(isLoading = false, password = "") }
    }

    private fun onFailure(throwable: Throwable) {
        // The login succeeded and the account simply has no key yet — a `getErrorMessage` about
        // the URL or the key would send the user to fix the one thing that was right.
        val message = if (throwable is ApiKeyNotFound) {
            NativeText.Resource(RString.login_error_api_key_missing)
        } else {
            getErrorMessage(throwable)
        }
        _uiState.update { it.copy(isLoading = false, error = message) }
    }

    private fun showError(resource: StringResource) {
        _uiState.update { it.copy(isLoading = false, error = NativeText.Resource(resource)) }
    }
}

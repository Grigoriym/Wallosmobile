package com.grappim.wallosmobile.feature.setup.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.kit.domain.PendingCertTrust
import com.grappim.kit.domain.findPendingCertTrust
import com.grappim.kit.logger.LogPriority
import com.grappim.kit.logger.logcat
import com.grappim.kit.uikit.NativeText
import com.grappim.wallosmobile.core.storage.customheaders.CustomHeader
import com.grappim.wallosmobile.core.storage.customheaders.CustomHeaderValidator
import com.grappim.wallosmobile.core.storage.customheaders.CustomHeadersStorage
import com.grappim.wallosmobile.core.storage.customheaders.isBlank
import com.grappim.wallosmobile.feature.setup.domain.model.ApiKeyNotFound
import com.grappim.wallosmobile.feature.setup.domain.model.LoginOutcome
import com.grappim.wallosmobile.feature.setup.domain.model.PasswordLoginAvailability
import com.grappim.wallosmobile.feature.setup.domain.repo.SetupRepository
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.login_error_api_key_missing
import com.grappim.wallosmobile.strings.generated.resources.login_error_cert_not_trusted
import com.grappim.wallosmobile.strings.generated.resources.login_error_invalid_credentials
import com.grappim.wallosmobile.strings.generated.resources.login_error_invalid_totp
import com.grappim.wallosmobile.strings.generated.resources.login_error_totp_session_expired
import com.grappim.wallosmobile.utils.ui.getErrorMessage
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * The repository is a `@Factory` (plan §1.1) — it owns the web session transitively — so this
 * ViewModel holds one onboarding session for as long as the screen lives, and a new attempt after
 * a process death gets a clean cookie jar.
 */
@KoinViewModel
class LoginViewModel(
    private val setupRepository: SetupRepository,
    private val customHeadersStorage: CustomHeadersStorage,
    private val customHeaderValidator: CustomHeaderValidator
) : ViewModel() {

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
            onCertTrustDismiss = ::onCertTrustDismiss,
            onCustomHeadersExpandedChange = ::onCustomHeadersExpandedChange,
            onCustomHeaderAdd = ::onCustomHeaderAdd,
            onCustomHeaderRemove = ::onCustomHeaderRemove,
            onCustomHeaderNameChange = ::onCustomHeaderNameChange,
            onCustomHeaderValueChange = ::onCustomHeaderValueChange
        )
    )
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /**
     * Beside the state rather than read out of it, so the probe sees every URL the user settles on
     * — including the prefilled one — without a second copy of the field to keep in step (3.6).
     */
    private val serverUrlInput = MutableStateFlow("")

    /**
     * The headers the rows would send, beside the state for the same reason as [serverUrlInput].
     * `null` while there is nothing sendable to probe with: before the stored ones have been
     * offered back, and while a row doesn't validate.
     */
    private val customHeadersInput = MutableStateFlow<List<CustomHeader>?>(null)

    init {
        observeServerUrl()
        viewModelScope.launch { onStoredCustomHeaders(customHeadersStorage.headers.first()) }
        viewModelScope.launch {
            setupRepository.getStoredServerUrl()
                .onSuccess(::onStoredServerUrl)
                // No prefill is a worse screen, not a broken one — the user types the URL as before.
                .onFailure { logcat(priority = LogPriority.WARN, throwable = it) { "No stored server URL" } }
        }
    }

    /**
     * The probe runs off the URL field, not off Connect: its whole point is to be earlier than the
     * password. Debounced because it is fired by typing, and filtered to an explicit scheme
     * because the app infers none anywhere else — half a URL is not an instance to ask.
     *
     * The headers ride along because a proxy in front of the instance rejects the probe without
     * them (plan §4.5b) — so a change to them is a new question for the same URL.
     */
    @OptIn(FlowPreview::class)
    private fun observeServerUrl() {
        combine(serverUrlInput, customHeadersInput) { url, headers -> headers?.let { url.trim() to it } }
            .debounce(PROBE_DEBOUNCE)
            .mapNotNull { it }
            .filter { (url, _) -> SCHEMES.any { url.startsWith(it, ignoreCase = true) } }
            .distinctUntilChanged()
            .onEach { (url, headers) -> probePasswordLogin(url, headers) }
            .launchIn(viewModelScope)
    }

    /**
     * Silent on failure, deliberately: this is an affordance and it fires while the user is still
     * typing, so an unreachable host here is not news — and raising the trust prompt off a
     * keystroke would be a modal dialog nobody asked for. Connect still reports both.
     */
    private suspend fun probePasswordLogin(serverUrl: String, customHeaders: List<CustomHeader>) {
        // `login.php` clears the session's pending TOTP challenge as it renders, so a probe here
        // would turn the next code into `TotpSessionExpired` (`SetupRepository.probePasswordLogin`).
        if (_uiState.value.isTotpRequired) return

        // The HTTP clients read the headers from storage, not from here — so they are saved first.
        customHeadersStorage.setHeaders(customHeaders)
        setupRepository.probePasswordLogin(serverUrl)
            .onSuccess(::onPasswordLoginAvailability)
            .onFailure { logcat(priority = LogPriority.WARN, throwable = it) { "Could not probe $serverUrl" } }
    }

    /**
     * Only [PasswordLoginAvailability.Disabled] moves anything: it hides a path, so it takes the
     * one answer that is evidence. The mode is forced rather than merely offered — there is
     * nothing behind the other one on this instance.
     */
    private fun onPasswordLoginAvailability(availability: PasswordLoginAvailability) {
        val isDisabled = availability == PasswordLoginAvailability.Disabled
        _uiState.update {
            it.copy(
                isPasswordLoginDisabled = isDisabled,
                isApiKeyMode = it.isApiKeyMode || isDisabled
            )
        }
    }

    /**
     * Disconnect keeps the URL (plan §4.7) and this is what finally offers it back. The read lands
     * a beat after construction, so anything the user has already typed wins over it.
     */
    private fun onStoredServerUrl(url: String) {
        if (url.isEmpty()) return
        _uiState.update { if (it.serverUrl.isEmpty()) it.copy(serverUrl = url) else it }
        serverUrlInput.value = _uiState.value.serverUrl
    }

    /**
     * They survive disconnect with the URL (plan §4.5b), and Connect saves what the rows hold — so
     * a screen that didn't offer them back would delete them. Rows the user has already started win.
     */
    private fun onStoredCustomHeaders(headers: List<CustomHeader>) {
        if (_uiState.value.customHeaders.isNotEmpty()) return
        _uiState.update {
            it.copy(
                customHeaders = headers.map { header -> CustomHeaderRow(header.name, header.value) }.toImmutableList(),
                // Headers being sent shouldn't be hidden behind a collapsed section.
                isCustomHeadersExpanded = headers.isNotEmpty()
            )
        }
        customHeadersInput.value = headers
    }

    private fun onCustomHeadersExpandedChange(isExpanded: Boolean) {
        _uiState.update { it.copy(isCustomHeadersExpanded = isExpanded) }
    }

    private fun onCustomHeaderAdd() {
        updateCustomHeaders { it + CustomHeaderRow() }
    }

    private fun onCustomHeaderRemove(index: Int) {
        updateCustomHeaders { rows -> rows.filterIndexed { i, _ -> i != index } }
    }

    private fun onCustomHeaderNameChange(index: Int, name: String) {
        updateCustomHeaders { rows -> rows.mapIndexed { i, row -> if (i == index) row.copy(name = name) else row } }
    }

    private fun onCustomHeaderValueChange(index: Int, value: String) {
        updateCustomHeaders { rows -> rows.mapIndexed { i, row -> if (i == index) row.copy(value = value) else row } }
    }

    /** Re-validates every row, since a duplicate is a property of the list, not of one row. */
    private fun updateCustomHeaders(transform: (List<CustomHeaderRow>) -> List<CustomHeaderRow>) {
        val rows = transform(_uiState.value.customHeaders)
        val headers = rows.toCustomHeaders()
        val errors = customHeaderValidator.validate(headers)
        _uiState.update { state ->
            state.copy(
                customHeaders = rows.mapIndexed { i, row -> row.copy(error = errors[i]) }.toImmutableList(),
                error = NativeText.Empty
            )
        }
        customHeadersInput.value = headers.filterNot { it.isBlank() }.takeIf { errors.all { it == null } }
    }

    private fun onServerUrlChange(url: String) {
        _uiState.update { it.copy(serverUrl = url, error = NativeText.Empty) }
        serverUrlInput.value = url
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
            _uiState.update { it.copy(isLoading = true, throttleWaitSeconds = 0, error = NativeText.Empty) }

            // The challenge belongs to a session that already went through with the saved ones.
            if (!state.isTotpRequired) {
                customHeadersStorage.setHeaders(state.customHeaders.toCustomHeaders().filterNot { it.isBlank() })
            }

            when {
                state.isApiKeyMode ->
                    setupRepository.connectWithApiKey(state.serverUrl, state.apiKey)
                        .onSuccess { onConnected() }
                        .onFailure(::onFailure)

                // The session this answers belongs to the repository, so there is nothing to pass
                // it but the code — see `SetupRepository.submitTotpCode`.
                state.isTotpRequired ->
                    setupRepository.submitTotpCode(state.totpCode, ::onThrottleWait)
                        .onSuccess { onOutcome(it) }
                        .onFailure(::onFailure)

                else ->
                    setupRepository.loginWithPassword(
                        serverUrl = state.serverUrl,
                        username = state.username,
                        password = state.password,
                        onThrottleWait = ::onThrottleWait
                    )
                        .onSuccess { onOutcome(it) }
                        .onFailure(::onFailure)
            }

            // The attempt is over however it went, so whatever it waited for is no longer news.
            // One place rather than each terminal branch: the notice belongs to the call, not to
            // any of the outcomes.
            _uiState.update { it.copy(throttleWaitSeconds = 0) }
        }
    }

    /**
     * The backoff is spent inside the call, under the spinner that was already there (3.10), so
     * without this the login simply gets slower and says nothing (5.5). It arrives off the IO
     * dispatcher the repository moved to, which a `MutableStateFlow` is fine with.
     */
    private fun onThrottleWait(wait: Duration) {
        _uiState.update { it.copy(throttleWaitSeconds = wait.inWholeSeconds.toInt()) }
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
     * The prompt replaces the error rather than joining it: `getErrorMessage` names the
     * certificate and sends the user to Settings, which is the route for a screen that has no
     * trust surface — this one does, and it is right here.
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

    private fun List<CustomHeaderRow>.toCustomHeaders(): List<CustomHeader> =
        map { CustomHeader(it.name.trim(), it.value.trim()) }

    private companion object {
        val PROBE_DEBOUNCE = 700.milliseconds

        /** The app infers no scheme, here or anywhere else — so neither does the probe. */
        val SCHEMES = listOf("http://", "https://")
    }
}

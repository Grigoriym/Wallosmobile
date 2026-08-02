package com.grappim.wallosmobile.feature.setup.ui

import com.grappim.wallosmobile.utils.ui.NativeText

/**
 * Both onboarding paths live in one state (plan §1.1): [isApiKeyMode] `false` drives the web login
 * with [username] + [password], `true` takes a [apiKey] the user already has. [serverUrl] is
 * common to both — it is the one field that is wrong when the failure is a transport one.
 */
data class LoginUiState(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val apiKey: String = "",
    val isPasswordVisible: Boolean = false,
    val isApiKeyMode: Boolean = false,
    val isLoading: Boolean = false,
    val error: NativeText = NativeText.Empty,
    val onServerUrlChange: (String) -> Unit = {},
    val onUsernameChange: (String) -> Unit = {},
    val onPasswordChange: (String) -> Unit = {},
    val onApiKeyChange: (String) -> Unit = {},
    val onPasswordVisibilityChange: (Boolean) -> Unit = {},
    val onApiKeyModeChange: (Boolean) -> Unit = {},
    val onConnectClick: () -> Unit = {}
) {

    /** Only the visible path's fields count — the hidden one keeps whatever the user typed. */
    val canConnect: Boolean
        get() = !isLoading && serverUrl.isNotBlank() &&
            if (isApiKeyMode) {
                apiKey.isNotBlank()
            } else {
                username.isNotBlank() && password.isNotBlank()
            }

    /**
     * Plan §9: a password POSTed over cleartext is materially worse than a key the user pastes in,
     * so an `http://` instance gets a warning that steers to Path B — and the warning goes away
     * once it has been taken, because there is then nothing left for the user to act on.
     *
     * It never disables anything: a self-hosted LAN instance on plain HTTP is the normal case
     * (the manifest carries `usesCleartextTraffic`, 1.11), and blocking it would block the only
     * instance this project can test against. Only an explicit `http://` counts — the app does no
     * scheme inference anywhere else either.
     */
    val isCleartextWarningVisible: Boolean
        get() = !isApiKeyMode && serverUrl.trim().startsWith(CLEARTEXT_SCHEME, ignoreCase = true)

    private companion object {
        const val CLEARTEXT_SCHEME = "http://"
    }
}

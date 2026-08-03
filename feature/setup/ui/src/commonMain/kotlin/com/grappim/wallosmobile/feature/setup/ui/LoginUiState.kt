package com.grappim.wallosmobile.feature.setup.ui

import com.grappim.wallosmobile.core.domain.PendingCertTrust
import com.grappim.wallosmobile.utils.ui.NativeText

/**
 * Both onboarding paths live in one state (plan §1.1): [isApiKeyMode] `false` drives the web login
 * with [username] + [password], `true` takes a [apiKey] the user already has. [serverUrl] is
 * common to both — it is the one field that is wrong when the failure is a transport one.
 *
 * [pendingCertTrust] is the trust prompt: a non-null one *is* the dialog being up, so there is no
 * visibility flag beside it to disagree with (3.5's rule about a second copy of a fact).
 *
 * [isTotpRequired] is the second step of the password path, not a third path: it is reached from
 * [isApiKeyMode] `false` and it replaces those fields with [totpCode] until the challenge is
 * answered or abandoned.
 */
data class LoginUiState(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val apiKey: String = "",
    val totpCode: String = "",
    val isPasswordVisible: Boolean = false,
    val isApiKeyMode: Boolean = false,
    val isTotpRequired: Boolean = false,
    val isLoading: Boolean = false,
    val error: NativeText = NativeText.Empty,
    val pendingCertTrust: PendingCertTrust? = null,
    val onServerUrlChange: (String) -> Unit = {},
    val onUsernameChange: (String) -> Unit = {},
    val onPasswordChange: (String) -> Unit = {},
    val onApiKeyChange: (String) -> Unit = {},
    val onTotpCodeChange: (String) -> Unit = {},
    val onPasswordVisibilityChange: (Boolean) -> Unit = {},
    val onApiKeyModeChange: (Boolean) -> Unit = {},
    val onConnectClick: () -> Unit = {},
    val onCertTrustConfirm: () -> Unit = {},
    val onCertTrustDismiss: () -> Unit = {}
) {

    /** Only the visible path's fields count — the hidden ones keep whatever the user typed. */
    val canConnect: Boolean
        get() = !isLoading && serverUrl.isNotBlank() &&
            when {
                isApiKeyMode -> apiKey.isNotBlank()
                isTotpRequired -> totpCode.isNotBlank()
                else -> username.isNotBlank() && password.isNotBlank()
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
     *
     * [isTotpRequired] retires it for the same reason [isApiKeyMode] does: by then the password
     * has already crossed the wire, so the warning is describing a decision that has been made.
     */
    val isCleartextWarningVisible: Boolean
        get() = !isApiKeyMode && !isTotpRequired &&
            serverUrl.trim().startsWith(CLEARTEXT_SCHEME, ignoreCase = true)

    private companion object {
        const val CLEARTEXT_SCHEME = "http://"
    }
}

package com.grappim.wallosmobile.feature.setup.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grappim.wallosmobile.core.domain.PendingCertTrust
import com.grappim.wallosmobile.strings.RPlurals
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.login_api_key_hint
import com.grappim.wallosmobile.strings.generated.resources.login_api_key_label
import com.grappim.wallosmobile.strings.generated.resources.login_cert_trust_cancel
import com.grappim.wallosmobile.strings.generated.resources.login_cert_trust_confirm
import com.grappim.wallosmobile.strings.generated.resources.login_cert_trust_fingerprint
import com.grappim.wallosmobile.strings.generated.resources.login_cert_trust_issuer
import com.grappim.wallosmobile.strings.generated.resources.login_cert_trust_message
import com.grappim.wallosmobile.strings.generated.resources.login_cert_trust_subject
import com.grappim.wallosmobile.strings.generated.resources.login_cert_trust_title
import com.grappim.wallosmobile.strings.generated.resources.login_cert_trust_validity
import com.grappim.wallosmobile.strings.generated.resources.login_cert_trust_validity_range
import com.grappim.wallosmobile.strings.generated.resources.login_cleartext_warning
import com.grappim.wallosmobile.strings.generated.resources.login_connect
import com.grappim.wallosmobile.strings.generated.resources.login_password_hide
import com.grappim.wallosmobile.strings.generated.resources.login_password_label
import com.grappim.wallosmobile.strings.generated.resources.login_password_login_disabled
import com.grappim.wallosmobile.strings.generated.resources.login_password_show
import com.grappim.wallosmobile.strings.generated.resources.login_server_url_label
import com.grappim.wallosmobile.strings.generated.resources.login_server_url_placeholder
import com.grappim.wallosmobile.strings.generated.resources.login_throttle_wait
import com.grappim.wallosmobile.strings.generated.resources.login_title
import com.grappim.wallosmobile.strings.generated.resources.login_totp_hint
import com.grappim.wallosmobile.strings.generated.resources.login_totp_label
import com.grappim.wallosmobile.strings.generated.resources.login_totp_message
import com.grappim.wallosmobile.strings.generated.resources.login_use_api_key
import com.grappim.wallosmobile.strings.generated.resources.login_use_password
import com.grappim.wallosmobile.strings.generated.resources.login_username_label
import com.grappim.wallosmobile.uikit.RDrawable
import com.grappim.wallosmobile.uikit.WallosMobilePreviewTheme
import com.grappim.wallosmobile.uikit.generated.resources.wallosmobile_logo
import com.grappim.wallosmobile.uikit.utils.PreviewWallosDarkLight
import com.grappim.wallosmobile.utils.ui.NativeText
import com.grappim.wallosmobile.utils.ui.asString
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * There is no success callback: a stored key *is* the signal, and the app's startup branch is
 * driven by `ApiKeyStorage.isConnected`. Reporting it a second time would give the same fact two
 * owners, and the one that disconnect (2.6) also has to flip is the flow.
 */
@Composable
fun LoginScreen(viewModel: LoginViewModel = koinViewModel<LoginViewModel>()) {
    val uiState by viewModel.uiState.collectAsState()

    LoginContent(uiState = uiState)
}

@Composable
private fun LoginContent(uiState: LoginUiState, modifier: Modifier = Modifier) {
    val focusManager = LocalFocusManager.current

    val pendingCertTrust = uiState.pendingCertTrust
    if (pendingCertTrust != null) {
        CertTrustDialog(
            pendingCertTrust = pendingCertTrust,
            onConfirm = uiState.onCertTrustConfirm,
            onDismiss = uiState.onCertTrustDismiss
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(PADDING),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(FIELD_SPACING, Alignment.CenterVertically)
    ) {
        Image(
            painter = painterResource(RDrawable.wallosmobile_logo),
            contentDescription = null,
            modifier = Modifier.size(LOGO_SIZE)
        )

        Text(
            text = stringResource(RString.login_title),
            style = MaterialTheme.typography.headlineMedium
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = uiState.serverUrl,
            onValueChange = uiState.onServerUrlChange,
            label = { Text(stringResource(RString.login_server_url_label)) },
            placeholder = { Text(stringResource(RString.login_server_url_placeholder)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            enabled = !uiState.isLoading
        )

        // Plan §9's only mitigation: `usesCleartextTraffic` is on (1.11), so nothing in the
        // platform is holding this line. It warns and points at Path B; it never disables Connect.
        if (uiState.isCleartextWarningVisible) {
            Text(
                text = stringResource(RString.login_cleartext_warning),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        // The probe already knows this instance has password login off, so there is no toggle to
        // offer below — only the reason text (plan §1.1).
        if (uiState.isPasswordLoginDisabled) {
            Text(
                text = stringResource(RString.login_password_login_disabled),
                style = MaterialTheme.typography.bodySmall
            )
        }

        when {
            uiState.isApiKeyMode -> ApiKeyField(uiState = uiState, focusManager = focusManager)
            uiState.isTotpRequired -> TotpField(uiState = uiState, focusManager = focusManager)
            else -> CredentialFields(uiState = uiState, focusManager = focusManager)
        }

        if (uiState.error.isNotEmpty()) {
            Text(
                text = uiState.error.asString(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(PROGRESS_SIZE))

            // The wait is spent inside the call, so without this the spinner is the only thing the
            // backoff has to say for itself (5.5).
            if (uiState.isThrottled) {
                Text(
                    text = pluralStringResource(
                        RPlurals.login_throttle_wait,
                        uiState.throttleWaitSeconds,
                        uiState.throttleWaitSeconds
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Button(
                onClick = uiState.onConnectClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.canConnect
            ) {
                Text(stringResource(RString.login_connect))
            }
        }

        // Path B is permanent, not a fallback (plan §1.1) — the bridge scrapes markup, and this is
        // what still works the day that markup changes.
        if (!uiState.isPasswordLoginDisabled) {
            TextButton(
                onClick = { uiState.onApiKeyModeChange(!uiState.isApiKeyMode) },
                enabled = !uiState.isLoading
            ) {
                Text(
                    stringResource(
                        if (uiState.isApiKeyMode) RString.login_use_password else RString.login_use_api_key
                    )
                )
            }
        }
    }
}

@Composable
private fun CredentialFields(uiState: LoginUiState, focusManager: FocusManager) {
    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentType = ContentType.Username },
        value = uiState.username,
        onValueChange = uiState.onUsernameChange,
        label = { Text(stringResource(RString.login_username_label)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
        enabled = !uiState.isLoading
    )

    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentType = ContentType.Password },
        value = uiState.password,
        onValueChange = uiState.onPasswordChange,
        label = { Text(stringResource(RString.login_password_label)) },
        singleLine = true,
        visualTransformation = if (uiState.isPasswordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = {
                focusManager.clearFocus()
                uiState.onConnectClick()
            }
        ),
        enabled = !uiState.isLoading,
        // `material-icons-core` carries no `Visibility`/`VisibilityOff` (CLAUDE.md), and one
        // toggle isn't worth pulling `material-icons-extended` into every `ui` module.
        trailingIcon = {
            TextButton(onClick = { uiState.onPasswordVisibilityChange(!uiState.isPasswordVisible) }) {
                Text(
                    stringResource(
                        if (uiState.isPasswordVisible) RString.login_password_hide else RString.login_password_show
                    )
                )
            }
        }
    )
}

/**
 * The second step of the password path. Deliberately **not** a numeric keyboard: Wallos accepts a
 * backup code here as readily as a generated one, and a backup code is 20 hex characters
 * (`endpoints/user/enable_totp.php`) that a number pad cannot type.
 */
@Composable
private fun TotpField(uiState: LoginUiState, focusManager: FocusManager) {
    Text(
        text = stringResource(RString.login_totp_message),
        style = MaterialTheme.typography.bodyMedium
    )

    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = uiState.totpCode,
        onValueChange = uiState.onTotpCodeChange,
        label = { Text(stringResource(RString.login_totp_label)) },
        supportingText = { Text(stringResource(RString.login_totp_hint)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = {
                focusManager.clearFocus()
                uiState.onConnectClick()
            }
        ),
        enabled = !uiState.isLoading
    )
}

@Composable
private fun ApiKeyField(uiState: LoginUiState, focusManager: FocusManager) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = uiState.apiKey,
        onValueChange = uiState.onApiKeyChange,
        label = { Text(stringResource(RString.login_api_key_label)) },
        supportingText = { Text(stringResource(RString.login_api_key_hint)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = {
                focusManager.clearFocus()
                uiState.onConnectClick()
            }
        ),
        enabled = !uiState.isLoading
    )
}

/**
 * Trust-on-first-use, and the one screen in the app that asks the user to weigh a security
 * decision — so it shows what the decision is *about* rather than only naming the host. The
 * fingerprint is the part that can actually be compared against the server, which is why it is
 * monospaced and last.
 */
@Composable
private fun CertTrustDialog(pendingCertTrust: PendingCertTrust, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(RString.login_cert_trust_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(CERT_ROW_SPACING)
            ) {
                Text(
                    text = stringResource(RString.login_cert_trust_message, pendingCertTrust.host),
                    style = MaterialTheme.typography.bodyMedium
                )
                CertTrustRow(RString.login_cert_trust_subject, pendingCertTrust.subject)
                CertTrustRow(RString.login_cert_trust_issuer, pendingCertTrust.issuer)
                CertTrustRow(
                    label = RString.login_cert_trust_validity,
                    value = stringResource(
                        RString.login_cert_trust_validity_range,
                        pendingCertTrust.notBefore,
                        pendingCertTrust.notAfter
                    )
                )
                CertTrustRow(
                    label = RString.login_cert_trust_fingerprint,
                    value = pendingCertTrust.sha256Fingerprint,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(RString.login_cert_trust_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(RString.login_cert_trust_cancel)) }
        }
    )
}

@Composable
private fun CertTrustRow(label: StringResource, value: String, fontFamily: FontFamily? = null) {
    Column {
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = fontFamily
        )
    }
}

private val PADDING = 16.dp
private val FIELD_SPACING = 16.dp
private val PROGRESS_SIZE = 48.dp
private val CERT_ROW_SPACING = 12.dp
private val LOGO_SIZE = 96.dp

@PreviewWallosDarkLight
@Composable
private fun LoginContentPreview() = WallosMobilePreviewTheme {
    LoginContent(uiState = LoginUiState())
}

@PreviewWallosDarkLight
@Composable
private fun LoginContentFilledPreview() = WallosMobilePreviewTheme {
    LoginContent(
        uiState = LoginUiState(
            serverUrl = "https://wallos.example.com",
            username = "demo",
            password = "demo"
        )
    )
}

@PreviewWallosDarkLight
@Composable
private fun LoginContentLoadingPreview() = WallosMobilePreviewTheme {
    LoginContent(
        uiState = LoginUiState(
            serverUrl = "https://wallos.example.com",
            username = "demo",
            password = "demo",
            isLoading = true
        )
    )
}

/** The fourth refused attempt: the same spinner as above, now with a reason under it. */
@PreviewWallosDarkLight
@Composable
private fun LoginContentThrottledPreview() = WallosMobilePreviewTheme {
    LoginContent(
        uiState = LoginUiState(
            serverUrl = "https://wallos.example.com",
            username = "demo",
            password = "demo",
            isLoading = true,
            throttleWaitSeconds = 4
        )
    )
}

@PreviewWallosDarkLight
@Composable
private fun LoginContentErrorPreview() = WallosMobilePreviewTheme {
    LoginContent(
        uiState = LoginUiState(
            serverUrl = "https://wallos.example.com",
            username = "demo",
            password = "wrong",
            error = NativeText.Simple("Wallos didn't accept that username and password.")
        )
    )
}

@PreviewWallosDarkLight
@Composable
private fun LoginContentCleartextPreview() = WallosMobilePreviewTheme {
    LoginContent(
        uiState = LoginUiState(
            serverUrl = "http://wallos.lan:8282",
            username = "demo",
            password = "demo"
        )
    )
}

@PreviewWallosDarkLight
@Composable
private fun LoginContentCertTrustPreview() = WallosMobilePreviewTheme {
    LoginContent(
        uiState = LoginUiState(
            serverUrl = "https://wallos.lan:8443",
            username = "demo",
            password = "demo",
            pendingCertTrust = PendingCertTrust(
                host = "wallos.lan",
                subject = "CN=wallos.lan,O=Homelab",
                issuer = "CN=wallos.lan,O=Homelab",
                notBefore = "2026-01-04",
                notAfter = "2027-01-04",
                sha256Fingerprint = "3A:7B:1C:04:9E:22:F0:5D:8A:16:BB:71:C3:0F:49:E8:" +
                    "2D:65:90:AC:11:74:3E:52:D8:06:9B:CF:27:41:6A:E3"
            )
        )
    )
}

@PreviewWallosDarkLight
@Composable
private fun LoginContentTotpPreview() = WallosMobilePreviewTheme {
    LoginContent(
        uiState = LoginUiState(
            serverUrl = "https://wallos.example.com",
            username = "demo",
            totpCode = "123456",
            isTotpRequired = true
        )
    )
}

/** The probe came back `Disabled`: Path B, with the reason, and no toggle back to a dead path. */
@PreviewWallosDarkLight
@Composable
private fun LoginContentPasswordLoginDisabledPreview() = WallosMobilePreviewTheme {
    LoginContent(
        uiState = LoginUiState(
            serverUrl = "https://wallos.example.com",
            isApiKeyMode = true,
            isPasswordLoginDisabled = true
        )
    )
}

@PreviewWallosDarkLight
@Composable
private fun LoginContentApiKeyPreview() = WallosMobilePreviewTheme {
    LoginContent(
        uiState = LoginUiState(
            serverUrl = "https://wallos.example.com",
            apiKey = "5c1e0b2a9f",
            isApiKeyMode = true
        )
    )
}

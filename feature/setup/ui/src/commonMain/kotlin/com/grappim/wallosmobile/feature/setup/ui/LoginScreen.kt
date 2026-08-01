package com.grappim.wallosmobile.feature.setup.ui

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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.login_api_key_hint
import com.grappim.wallosmobile.strings.generated.resources.login_api_key_label
import com.grappim.wallosmobile.strings.generated.resources.login_connect
import com.grappim.wallosmobile.strings.generated.resources.login_password_hide
import com.grappim.wallosmobile.strings.generated.resources.login_password_label
import com.grappim.wallosmobile.strings.generated.resources.login_password_show
import com.grappim.wallosmobile.strings.generated.resources.login_server_url_label
import com.grappim.wallosmobile.strings.generated.resources.login_server_url_placeholder
import com.grappim.wallosmobile.strings.generated.resources.login_title
import com.grappim.wallosmobile.strings.generated.resources.login_use_api_key
import com.grappim.wallosmobile.strings.generated.resources.login_use_password
import com.grappim.wallosmobile.strings.generated.resources.login_username_label
import com.grappim.wallosmobile.uikit.WallosMobilePreviewTheme
import com.grappim.wallosmobile.uikit.utils.PreviewWallosDarkLight
import com.grappim.wallosmobile.utils.ui.NativeText
import com.grappim.wallosmobile.utils.ui.asString
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(PADDING),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(FIELD_SPACING, Alignment.CenterVertically)
    ) {
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

        if (uiState.isApiKeyMode) {
            ApiKeyField(uiState = uiState, focusManager = focusManager)
        } else {
            CredentialFields(uiState = uiState, focusManager = focusManager)
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

private val PADDING = 16.dp
private val FIELD_SPACING = 16.dp
private val PROGRESS_SIZE = 48.dp

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
private fun LoginContentApiKeyPreview() = WallosMobilePreviewTheme {
    LoginContent(
        uiState = LoginUiState(
            serverUrl = "https://wallos.example.com",
            apiKey = "5c1e0b2a9f",
            isApiKeyMode = true
        )
    )
}

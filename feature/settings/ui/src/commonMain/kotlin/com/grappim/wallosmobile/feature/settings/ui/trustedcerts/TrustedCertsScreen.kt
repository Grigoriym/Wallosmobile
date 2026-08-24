package com.grappim.wallosmobile.feature.settings.ui.trustedcerts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grappim.wallosmobile.core.domain.PendingCertTrust
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.settings_trusted_certificates
import com.grappim.wallosmobile.strings.generated.resources.trusted_cert_fingerprint
import com.grappim.wallosmobile.strings.generated.resources.trusted_cert_issuer
import com.grappim.wallosmobile.strings.generated.resources.trusted_cert_revoke
import com.grappim.wallosmobile.strings.generated.resources.trusted_cert_revoke_cancel
import com.grappim.wallosmobile.strings.generated.resources.trusted_cert_revoke_confirm
import com.grappim.wallosmobile.strings.generated.resources.trusted_cert_revoke_confirm_message
import com.grappim.wallosmobile.strings.generated.resources.trusted_cert_revoke_confirm_title
import com.grappim.wallosmobile.strings.generated.resources.trusted_cert_valid_until
import com.grappim.wallosmobile.strings.generated.resources.trusted_certs_empty
import com.grappim.wallosmobile.uikit.WallosMobilePreviewTheme
import com.grappim.wallosmobile.uikit.utils.PreviewWallosDarkLight
import com.grappim.wallosmobile.uikit.widgets.topappbar.LocalTopBarConfig
import com.grappim.wallosmobile.uikit.widgets.topappbar.NavigationIconConfig
import com.grappim.wallosmobile.uikit.widgets.topappbar.TopBarConfig
import com.grappim.wallosmobile.utils.ui.NativeText
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TrustedCertsScreen(
    viewModel: TrustedCertsViewModel = koinViewModel<TrustedCertsViewModel>(),
    onBackClick: () -> Unit
) {
    val topBarController = LocalTopBarConfig.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        topBarController.update(
            TopBarConfig(
                title = NativeText.Resource(RString.settings_trusted_certificates),
                navigationIcon = NavigationIconConfig.Back(onBackClick = onBackClick)
            )
        )
    }

    uiState.certPendingRevoke?.let { cert ->
        RevokeConfirmDialog(cert = cert, uiState = uiState)
    }

    TrustedCertsContent(uiState = uiState)
}

@Composable
private fun TrustedCertsContent(uiState: TrustedCertsUiState, modifier: Modifier = Modifier) {
    if (uiState.certs.isEmpty()) {
        EmptyState(modifier = modifier)
    } else {
        LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = LIST_PADDING)) {
            items(items = uiState.certs, key = { it.host + it.sha256Fingerprint }) { cert ->
                TrustedCertRow(cert = cert, onRevokeClick = uiState.onRevokeClick)
            }
        }
    }
}

@Composable
private fun TrustedCertRow(cert: PendingCertTrust, onRevokeClick: (PendingCertTrust) -> Unit) {
    val revokeContentDescription = stringResource(RString.trusted_cert_revoke)

    ListItem(
        headlineContent = { Text(cert.host) },
        supportingContent = {
            Column {
                Text(stringResource(RString.trusted_cert_issuer, cert.issuer))
                Text(stringResource(RString.trusted_cert_valid_until, cert.notAfter))
                Text(
                    text = stringResource(RString.trusted_cert_fingerprint, cert.sha256Fingerprint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        trailingContent = {
            IconButton(onClick = { onRevokeClick(cert) }) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = revokeContentDescription,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    )
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().padding(SCREEN_PADDING), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(RString.trusted_certs_empty),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RevokeConfirmDialog(cert: PendingCertTrust, uiState: TrustedCertsUiState, modifier: Modifier = Modifier) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = uiState.onRevokeDialogDismiss,
        title = { Text(stringResource(RString.trusted_cert_revoke_confirm_title)) },
        text = {
            Text(stringResource(RString.trusted_cert_revoke_confirm_message, cert.host))
        },
        confirmButton = {
            TextButton(onClick = uiState.onRevokeConfirm) {
                Text(stringResource(RString.trusted_cert_revoke_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = uiState.onRevokeDialogDismiss) {
                Text(stringResource(RString.trusted_cert_revoke_cancel))
            }
        }
    )
}

private val SCREEN_PADDING = 16.dp
private val LIST_PADDING = 8.dp

private fun previewCert(host: String, sha256Fingerprint: String) = PendingCertTrust(
    host = host,
    subject = "CN=$host",
    issuer = "CN=Home Lab Test CA",
    notBefore = "2026-01-01",
    notAfter = "2027-01-01",
    sha256Fingerprint = sha256Fingerprint
)

@PreviewWallosDarkLight
@Composable
private fun TrustedCertsContentPreview() = WallosMobilePreviewTheme {
    TrustedCertsContent(
        uiState = TrustedCertsUiState(
            certs = persistentListOf(
                previewCert(host = "wallos.example.com", sha256Fingerprint = "AA:BB:CC:DD"),
                previewCert(host = "192.168.1.50", sha256Fingerprint = "11:22:33:44")
            )
        )
    )
}

@PreviewWallosDarkLight
@Composable
private fun TrustedCertsContentEmptyPreview() = WallosMobilePreviewTheme {
    TrustedCertsContent(uiState = TrustedCertsUiState())
}

@PreviewWallosDarkLight
@Composable
private fun TrustedCertsContentRevokeDialogPreview() = WallosMobilePreviewTheme {
    val cert = previewCert(host = "wallos.example.com", sha256Fingerprint = "AA:BB:CC:DD")
    Box {
        TrustedCertsContent(uiState = TrustedCertsUiState(certs = persistentListOf(cert)))
        RevokeConfirmDialog(cert = cert, uiState = TrustedCertsUiState())
    }
}

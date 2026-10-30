package com.grappim.wallosmobile.composeapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grappim.wallosmobile.core.storage.ServerUrlStorage
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.local_network_banner_message
import com.grappim.wallosmobile.strings.generated.resources.local_network_banner_open_settings
import com.grappim.wallosmobile.uikit.WallosMobilePreviewTheme
import com.grappim.wallosmobile.uikit.permission.rememberIsLocalNetworkBlocked
import com.grappim.wallosmobile.uikit.permission.rememberLocalNetworkPermissionRequest
import com.grappim.wallosmobile.uikit.permission.rememberOpenAppSettings
import com.grappim.wallosmobile.uikit.utils.PreviewWallosDarkLight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * Sits above every screen in the shell, because a blocked server fails each of them the same way
 * and each would only report it as unreachable (plan §4.5c). Someone already connected never
 * passes through Connect, so this also asks once per run.
 */
@Composable
internal fun LocalNetworkBanner(modifier: Modifier = Modifier, serverUrlStorage: ServerUrlStorage = koinInject()) {
    val localNetworkPermission = rememberLocalNetworkPermissionRequest()
    val openAppSettings = rememberOpenAppSettings()
    val serverUrl by produceState("") {
        value = withContext(Dispatchers.Default) { serverUrlStorage.serverUrl }
    }
    var hasAsked by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(serverUrl) {
        if (serverUrl.isNotEmpty() && !hasAsked) {
            hasAsked = true
            localNetworkPermission.request(serverUrl = serverUrl, onAllowed = {}, onDenied = {})
        }
    }

    if (rememberIsLocalNetworkBlocked(serverUrl)) {
        LocalNetworkBannerContent(onOpenSettingsClick = openAppSettings, modifier = modifier)
    }
}

@Composable
private fun LocalNetworkBannerContent(onOpenSettingsClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(
            modifier = Modifier.padding(start = BANNER_PADDING),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BANNER_PADDING)
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(RString.local_network_banner_message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onOpenSettingsClick) {
                Text(stringResource(RString.local_network_banner_open_settings))
            }
        }
    }
}

private val BANNER_PADDING = 16.dp

@PreviewWallosDarkLight
@Composable
private fun LocalNetworkBannerContentPreview() = WallosMobilePreviewTheme {
    LocalNetworkBannerContent(onOpenSettingsClick = {})
}

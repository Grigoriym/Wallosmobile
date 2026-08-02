package com.grappim.wallosmobile.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.settings_disconnect
import com.grappim.wallosmobile.strings.generated.resources.settings_disconnect_description
import com.grappim.wallosmobile.strings.generated.resources.settings_title
import com.grappim.wallosmobile.uikit.WallosMobilePreviewTheme
import com.grappim.wallosmobile.uikit.utils.PreviewWallosDarkLight
import com.grappim.wallosmobile.uikit.widgets.topappbar.LocalTopBarConfig
import com.grappim.wallosmobile.uikit.widgets.topappbar.NavigationIconConfig
import com.grappim.wallosmobile.uikit.widgets.topappbar.TopBarConfig
import com.grappim.wallosmobile.utils.ui.NativeText
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = koinViewModel<SettingsViewModel>()) {
    val topBarController = LocalTopBarConfig.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        topBarController.update(
            TopBarConfig(
                title = NativeText.Resource(RString.settings_title),
                navigationIcon = NavigationIconConfig.Menu
            )
        )
    }

    SettingsContent(uiState = uiState)
}

@Composable
private fun SettingsContent(uiState: SettingsUiState, modifier: Modifier = Modifier) {
    // A fixed set of items, so a `Column` rather than a `LazyColumn`.
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(SCREEN_PADDING),
        verticalArrangement = Arrangement.spacedBy(ITEM_SPACING)
    ) {
        Text(
            text = stringResource(RString.settings_disconnect_description),
            style = MaterialTheme.typography.bodyMedium
        )

        Button(
            onClick = uiState.onDisconnectClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(RString.settings_disconnect))
        }
    }
}

private val SCREEN_PADDING = 16.dp
private val ITEM_SPACING = 16.dp

@PreviewWallosDarkLight
@Composable
private fun SettingsContentPreview() = WallosMobilePreviewTheme {
    SettingsContent(uiState = SettingsUiState())
}

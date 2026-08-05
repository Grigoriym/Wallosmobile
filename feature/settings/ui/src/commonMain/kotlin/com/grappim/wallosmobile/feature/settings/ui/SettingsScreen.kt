package com.grappim.wallosmobile.feature.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
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
import com.grappim.wallosmobile.strings.generated.resources.settings_interface
import com.grappim.wallosmobile.strings.generated.resources.settings_interface_description
import com.grappim.wallosmobile.strings.generated.resources.settings_title
import com.grappim.wallosmobile.uikit.WallosMobilePreviewTheme
import com.grappim.wallosmobile.uikit.utils.PreviewWallosDarkLight
import com.grappim.wallosmobile.uikit.widgets.topappbar.LocalTopBarConfig
import com.grappim.wallosmobile.uikit.widgets.topappbar.NavigationIconConfig
import com.grappim.wallosmobile.uikit.widgets.topappbar.TopBarConfig
import com.grappim.wallosmobile.utils.ui.NativeText
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = koinViewModel<SettingsViewModel>(), onInterfaceClick: () -> Unit) {
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

    SettingsContent(uiState = uiState, onInterfaceClick = onInterfaceClick)
}

@Composable
private fun SettingsContent(uiState: SettingsUiState, onInterfaceClick: () -> Unit, modifier: Modifier = Modifier) {
    // A fixed set of items, so a `Column` rather than a `LazyColumn`.
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(SCREEN_PADDING),
        verticalArrangement = Arrangement.spacedBy(ITEM_SPACING)
    ) {
        // A sub-screen rather than a section on this one: the theme is device state and the rest
        // of this screen is account state, and 4.4's About row lands beside it.
        SettingsRow(
            title = RString.settings_interface,
            subtitle = RString.settings_interface_description,
            onClick = onInterfaceClick
        )

        HorizontalDivider()

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

@Composable
private fun SettingsRow(
    title: StringResource,
    subtitle: StringResource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = ROW_PADDING)
    ) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = stringResource(subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private val SCREEN_PADDING = 16.dp
private val ITEM_SPACING = 16.dp
private val ROW_PADDING = 8.dp

@PreviewWallosDarkLight
@Composable
private fun SettingsContentPreview() = WallosMobilePreviewTheme {
    SettingsContent(uiState = SettingsUiState(), onInterfaceClick = {})
}

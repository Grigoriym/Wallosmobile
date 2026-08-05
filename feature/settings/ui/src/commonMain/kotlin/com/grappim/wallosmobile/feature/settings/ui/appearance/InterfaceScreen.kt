package com.grappim.wallosmobile.feature.settings.ui.appearance

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.grappim.wallosmobile.core.storage.theme.ThemeMode
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.settings_interface
import com.grappim.wallosmobile.strings.generated.resources.settings_theme
import com.grappim.wallosmobile.strings.generated.resources.settings_theme_dark
import com.grappim.wallosmobile.strings.generated.resources.settings_theme_light
import com.grappim.wallosmobile.strings.generated.resources.settings_theme_system
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
fun InterfaceScreen(viewModel: InterfaceViewModel = koinViewModel<InterfaceViewModel>(), onBackClick: () -> Unit) {
    val topBarController = LocalTopBarConfig.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        topBarController.update(
            TopBarConfig(
                title = NativeText.Resource(RString.settings_interface),
                navigationIcon = NavigationIconConfig.Back(onBackClick = onBackClick)
            )
        )
    }

    InterfaceContent(uiState = uiState)
}

@Composable
private fun InterfaceContent(uiState: InterfaceUiState, modifier: Modifier = Modifier) {
    // Three fixed options, so a `Column` rather than a `LazyColumn`.
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(SCREEN_PADDING)
    ) {
        Text(
            text = stringResource(RString.settings_theme),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = TITLE_SPACING)
        )

        Column(modifier = Modifier.selectableGroup()) {
            ThemeModeRow(
                mode = ThemeMode.System,
                selected = uiState.selectedMode == ThemeMode.System,
                onSelect = uiState.onModeSelect
            )
            ThemeModeRow(
                mode = ThemeMode.Light,
                selected = uiState.selectedMode == ThemeMode.Light,
                onSelect = uiState.onModeSelect
            )
            ThemeModeRow(
                mode = ThemeMode.Dark,
                selected = uiState.selectedMode == ThemeMode.Dark,
                onSelect = uiState.onModeSelect
            )
        }
    }
}

/**
 * The whole row is the target and the `RadioButton` takes `onClick = null`, so the button is drawn
 * by the row's `selectable` rather than being a second, smaller hit area over the same state.
 */
@Composable
private fun ThemeModeRow(
    mode: ThemeMode,
    selected: Boolean,
    onSelect: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = { onSelect(mode) },
                role = Role.RadioButton
            )
            .padding(vertical = ROW_PADDING),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)

        Text(
            text = stringResource(mode.label),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = LABEL_SPACING)
        )
    }
}

private val ThemeMode.label: StringResource
    get() = when (this) {
        ThemeMode.System -> RString.settings_theme_system
        ThemeMode.Light -> RString.settings_theme_light
        ThemeMode.Dark -> RString.settings_theme_dark
    }

private val SCREEN_PADDING = 16.dp
private val TITLE_SPACING = 8.dp
private val ROW_PADDING = 12.dp
private val LABEL_SPACING = 16.dp

@PreviewWallosDarkLight
@Composable
private fun InterfaceContentPreview() = WallosMobilePreviewTheme {
    InterfaceContent(uiState = InterfaceUiState())
}

@PreviewWallosDarkLight
@Composable
private fun InterfaceContentDarkSelectedPreview() = WallosMobilePreviewTheme {
    InterfaceContent(uiState = InterfaceUiState(selectedMode = ThemeMode.Dark))
}

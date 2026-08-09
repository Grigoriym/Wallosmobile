package com.grappim.wallosmobile.feature.settings.ui.startdestination

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
import com.grappim.wallosmobile.core.storage.startdestination.StartDestination
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.categories_title
import com.grappim.wallosmobile.strings.generated.resources.currencies_title
import com.grappim.wallosmobile.strings.generated.resources.dashboard_title
import com.grappim.wallosmobile.strings.generated.resources.household_title
import com.grappim.wallosmobile.strings.generated.resources.payment_methods_title
import com.grappim.wallosmobile.strings.generated.resources.settings_start_destination
import com.grappim.wallosmobile.strings.generated.resources.settings_title
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_title
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
fun StartDestinationScreen(
    viewModel: StartDestinationViewModel = koinViewModel<StartDestinationViewModel>(),
    onBackClick: () -> Unit
) {
    val topBarController = LocalTopBarConfig.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        topBarController.update(
            TopBarConfig(
                title = NativeText.Resource(RString.settings_start_destination),
                navigationIcon = NavigationIconConfig.Back(onBackClick = onBackClick)
            )
        )
    }

    StartDestinationContent(uiState = uiState)
}

@Composable
private fun StartDestinationContent(uiState: StartDestinationUiState, modifier: Modifier = Modifier) {
    // Seven fixed options, still fixed-item, so a `Column` rather than a `LazyColumn`.
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(SCREEN_PADDING)
    ) {
        Column(modifier = Modifier.selectableGroup()) {
            StartDestination.entries.forEach { destination ->
                StartDestinationRow(
                    destination = destination,
                    selected = uiState.selected == destination,
                    onSelect = uiState.onSelect
                )
            }
        }
    }
}

/**
 * The whole row is the target and the `RadioButton` takes `onClick = null`, so the button is drawn
 * by the row's `selectable` rather than being a second, smaller hit area over the same state.
 */
@Composable
private fun StartDestinationRow(
    destination: StartDestination,
    selected: Boolean,
    onSelect: (StartDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = { onSelect(destination) },
                role = Role.RadioButton
            )
            .padding(vertical = ROW_PADDING),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)

        Text(
            text = stringResource(destination.label),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = LABEL_SPACING)
        )
    }
}

private val StartDestination.label: StringResource
    get() = when (this) {
        StartDestination.Dashboard -> RString.dashboard_title
        StartDestination.Subscriptions -> RString.subscriptions_title
        StartDestination.Settings -> RString.settings_title
        StartDestination.Categories -> RString.categories_title
        StartDestination.Household -> RString.household_title
        StartDestination.PaymentMethods -> RString.payment_methods_title
        StartDestination.Currencies -> RString.currencies_title
    }

private val SCREEN_PADDING = 16.dp
private val ROW_PADDING = 12.dp
private val LABEL_SPACING = 16.dp

@PreviewWallosDarkLight
@Composable
private fun StartDestinationContentPreview() = WallosMobilePreviewTheme {
    StartDestinationContent(uiState = StartDestinationUiState())
}

@PreviewWallosDarkLight
@Composable
private fun StartDestinationContentSubscriptionsSelectedPreview() = WallosMobilePreviewTheme {
    StartDestinationContent(uiState = StartDestinationUiState(selected = StartDestination.Subscriptions))
}

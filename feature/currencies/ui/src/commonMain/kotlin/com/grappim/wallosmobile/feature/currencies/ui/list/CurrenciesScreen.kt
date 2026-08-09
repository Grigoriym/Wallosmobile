package com.grappim.wallosmobile.feature.currencies.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.currencies_empty
import com.grappim.wallosmobile.strings.generated.resources.currencies_main_label
import com.grappim.wallosmobile.strings.generated.resources.currencies_title
import com.grappim.wallosmobile.strings.generated.resources.currency_row_details
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_retry
import com.grappim.wallosmobile.uikit.WallosMobilePreviewTheme
import com.grappim.wallosmobile.uikit.utils.PreviewWallosDarkLight
import com.grappim.wallosmobile.uikit.widgets.topappbar.LocalTopBarConfig
import com.grappim.wallosmobile.uikit.widgets.topappbar.NavigationIconConfig
import com.grappim.wallosmobile.uikit.widgets.topappbar.TopBarConfig
import com.grappim.wallosmobile.utils.ui.NativeText
import com.grappim.wallosmobile.utils.ui.asString
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CurrenciesScreen(
    onCurrencyClick: (id: Int, name: String, symbol: String, code: String, rate: Double) -> Unit,
    viewModel: CurrenciesViewModel = koinViewModel()
) {
    val topBarController = LocalTopBarConfig.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        topBarController.update(
            TopBarConfig(
                title = NativeText.Resource(RString.currencies_title),
                navigationIcon = NavigationIconConfig.Menu
            )
        )
    }

    // No cache behind this list (`CurrenciesViewModel`'s own note): every fresh composition of this
    // screen — the first open and a return trip from the editor after an add/edit/delete — is the
    // refresh, since a covered Nav3 entry is disposed and this restarts on the way back to it.
    LaunchedEffect(Unit) { uiState.onRetryClick() }

    CurrenciesContent(uiState = uiState, onCurrencyClick = onCurrencyClick)
}

@Composable
private fun CurrenciesContent(
    uiState: CurrenciesUiState,
    onCurrencyClick: (id: Int, name: String, symbol: String, code: String, rate: Double) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        CurrenciesList(uiState = uiState, onCurrencyClick = onCurrencyClick, modifier = Modifier.fillMaxSize())

        when {
            uiState.isLoading -> LoadingState()
            uiState.isFailed -> ErrorState(uiState = uiState)
            uiState.isEmpty -> EmptyState()
        }
    }
}

@Composable
private fun CurrenciesList(
    uiState: CurrenciesUiState,
    onCurrencyClick: (id: Int, name: String, symbol: String, code: String, rate: Double) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(SCREEN_PADDING)) {
        items(items = uiState.items, key = { it.id }) { item ->
            CurrencyRow(
                item = item,
                onClick = { onCurrencyClick(item.id, item.name, item.symbol, item.code, item.rate) }
            )
        }
    }
}

@Composable
private fun CurrencyRow(item: CurrencyUiItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = ROW_PADDING),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(RString.currency_row_details, item.code, item.symbol, item.rate.toString()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (item.isMain) {
            Text(
                text = stringResource(RString.currencies_main_label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().padding(SCREEN_PADDING), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(RString.currencies_empty),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ErrorState(uiState: CurrenciesUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(SCREEN_PADDING),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = uiState.error.asString(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )

        Button(onClick = uiState.onRetryClick) {
            Text(stringResource(RString.subscriptions_retry))
        }
    }
}

private val SCREEN_PADDING = 16.dp
private val ROW_PADDING = 12.dp

private val previewItems = persistentListOf(
    CurrencyUiItem(id = 1, name = "US Dollar", symbol = "$", code = "USD", rate = 1.0, isMain = true),
    CurrencyUiItem(id = 2, name = "Euro", symbol = "€", code = "EUR", rate = 0.92, isMain = false)
)

@PreviewWallosDarkLight
@Composable
private fun CurrenciesContentPreview() = WallosMobilePreviewTheme {
    CurrenciesContent(uiState = CurrenciesUiState(items = previewItems), onCurrencyClick = { _, _, _, _, _ -> })
}

@PreviewWallosDarkLight
@Composable
private fun CurrenciesContentLoadingPreview() = WallosMobilePreviewTheme {
    CurrenciesContent(uiState = CurrenciesUiState(isLoading = true), onCurrencyClick = { _, _, _, _, _ -> })
}

@PreviewWallosDarkLight
@Composable
private fun CurrenciesContentEmptyPreview() = WallosMobilePreviewTheme {
    CurrenciesContent(uiState = CurrenciesUiState(), onCurrencyClick = { _, _, _, _, _ -> })
}

@PreviewWallosDarkLight
@Composable
private fun CurrenciesContentErrorPreview() = WallosMobilePreviewTheme {
    CurrenciesContent(
        uiState = CurrenciesUiState(
            error = NativeText.Simple("Couldn't reach that server. Check the URL and your connection.")
        ),
        onCurrencyClick = { _, _, _, _, _ -> }
    )
}

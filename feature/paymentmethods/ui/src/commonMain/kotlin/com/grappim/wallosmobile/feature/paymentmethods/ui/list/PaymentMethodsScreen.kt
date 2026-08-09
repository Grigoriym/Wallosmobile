package com.grappim.wallosmobile.feature.paymentmethods.ui.list

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
import com.grappim.wallosmobile.feature.paymentmethods.ui.widgets.PaymentMethodIcon
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.payment_methods_disabled
import com.grappim.wallosmobile.strings.generated.resources.payment_methods_empty
import com.grappim.wallosmobile.strings.generated.resources.payment_methods_title
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
fun PaymentMethodsScreen(
    onPaymentMethodClick: (id: Int, name: String, enabled: Boolean) -> Unit,
    viewModel: PaymentMethodsViewModel = koinViewModel()
) {
    val topBarController = LocalTopBarConfig.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        topBarController.update(
            TopBarConfig(
                title = NativeText.Resource(RString.payment_methods_title),
                navigationIcon = NavigationIconConfig.Menu
            )
        )
    }

    // No cache behind this list (`PaymentMethodsViewModel`'s own note): every fresh composition of
    // this screen — the first open and a return trip from the editor after an add/edit/delete — is
    // the refresh, since a covered Nav3 entry is disposed and this restarts on the way back to it.
    LaunchedEffect(Unit) { uiState.onRetryClick() }

    PaymentMethodsContent(uiState = uiState, onPaymentMethodClick = onPaymentMethodClick)
}

@Composable
private fun PaymentMethodsContent(
    uiState: PaymentMethodsUiState,
    onPaymentMethodClick: (id: Int, name: String, enabled: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        PaymentMethodsList(
            uiState = uiState,
            onPaymentMethodClick = onPaymentMethodClick,
            modifier = Modifier.fillMaxSize()
        )

        when {
            uiState.isLoading -> LoadingState()
            uiState.isFailed -> ErrorState(uiState = uiState)
            uiState.isEmpty -> EmptyState()
        }
    }
}

@Composable
private fun PaymentMethodsList(
    uiState: PaymentMethodsUiState,
    onPaymentMethodClick: (id: Int, name: String, enabled: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(SCREEN_PADDING)) {
        items(items = uiState.items, key = { it.id }) { item ->
            PaymentMethodRow(
                item = item,
                onClick = { onPaymentMethodClick(item.id, item.name, item.enabled) }
            )
        }
    }
}

@Composable
private fun PaymentMethodRow(item: PaymentMethodUiItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = ROW_PADDING),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ROW_SPACING)
    ) {
        PaymentMethodIcon(iconUrl = item.iconUrl, name = item.name, size = ICON_SIZE)

        Text(
            text = item.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )

        if (!item.enabled) {
            Text(
                text = stringResource(RString.payment_methods_disabled),
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
            text = stringResource(RString.payment_methods_empty),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ErrorState(uiState: PaymentMethodsUiState, modifier: Modifier = Modifier) {
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
private val ROW_PADDING = 8.dp
private val ROW_SPACING = 12.dp
private val ICON_SIZE = 40.dp

private val previewItems = persistentListOf(
    PaymentMethodUiItem(id = 1, name = "PayPal", iconUrl = "", enabled = true),
    PaymentMethodUiItem(id = 2, name = "Credit Card", iconUrl = "", enabled = false)
)

@PreviewWallosDarkLight
@Composable
private fun PaymentMethodsContentPreview() = WallosMobilePreviewTheme {
    PaymentMethodsContent(uiState = PaymentMethodsUiState(items = previewItems), onPaymentMethodClick = { _, _, _ -> })
}

@PreviewWallosDarkLight
@Composable
private fun PaymentMethodsContentLoadingPreview() = WallosMobilePreviewTheme {
    PaymentMethodsContent(uiState = PaymentMethodsUiState(isLoading = true), onPaymentMethodClick = { _, _, _ -> })
}

@PreviewWallosDarkLight
@Composable
private fun PaymentMethodsContentEmptyPreview() = WallosMobilePreviewTheme {
    PaymentMethodsContent(uiState = PaymentMethodsUiState(), onPaymentMethodClick = { _, _, _ -> })
}

@PreviewWallosDarkLight
@Composable
private fun PaymentMethodsContentErrorPreview() = WallosMobilePreviewTheme {
    PaymentMethodsContent(
        uiState = PaymentMethodsUiState(
            error = NativeText.Simple("Couldn't reach that server. Check the URL and your connection.")
        ),
        onPaymentMethodClick = { _, _, _ -> }
    )
}

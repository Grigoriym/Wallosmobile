@file:OptIn(ExperimentalMaterial3Api::class)

package com.grappim.wallosmobile.feature.subscriptions.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grappim.wallosmobile.feature.subscriptions.domain.model.BillingCycle
import com.grappim.wallosmobile.feature.subscriptions.ui.list.widgets.SubscriptionCard
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_empty
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_retry
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_title
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
fun SubscriptionsScreen(viewModel: SubscriptionsViewModel = koinViewModel<SubscriptionsViewModel>()) {
    val topBarController = LocalTopBarConfig.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        topBarController.update(
            TopBarConfig(
                title = NativeText.Resource(RString.subscriptions_title),
                navigationIcon = NavigationIconConfig.Menu
            )
        )
    }

    SubscriptionsContent(uiState = uiState)
}

@Composable
private fun SubscriptionsContent(uiState: SubscriptionsUiState, modifier: Modifier = Modifier) {
    PullToRefreshBox(
        modifier = modifier.fillMaxSize(),
        isRefreshing = uiState.isRefreshing,
        onRefresh = uiState.onRefresh
    ) {
        // The list is always composed so that pull-to-refresh has something to pull, even when
        // it is empty — the other states draw on top of it.
        SubscriptionsList(uiState = uiState)

        when {
            uiState.isLoading -> LoadingState()
            uiState.error.isNotEmpty() -> ErrorState(uiState = uiState)
            uiState.isEmpty -> EmptyState()
        }
    }
}

@Composable
private fun SubscriptionsList(uiState: SubscriptionsUiState, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(SCREEN_PADDING),
        verticalArrangement = Arrangement.spacedBy(ITEM_SPACING)
    ) {
        items(items = uiState.items, key = { it.id }) { item ->
            SubscriptionCard(item = item)
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
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(SCREEN_PADDING),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(RString.subscriptions_empty),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ErrorState(uiState: SubscriptionsUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(SCREEN_PADDING),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ITEM_SPACING, Alignment.CenterVertically)
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
private val ITEM_SPACING = 8.dp

private val previewItems = persistentListOf(
    SubscriptionUiItem(
        id = 1,
        name = "Disney+",
        logoUrl = "",
        price = "€8.99",
        nextPayment = "10 Mar 2026",
        cycle = BillingCycle.MONTHS,
        frequency = 1,
        isActive = true
    ),
    SubscriptionUiItem(
        id = 2,
        name = "Fiton",
        logoUrl = "",
        price = "€31.99",
        nextPayment = "31 Jan 2026",
        cycle = BillingCycle.YEARS,
        frequency = 1,
        isActive = false
    ),
    SubscriptionUiItem(
        id = 3,
        name = "1&1 Telekom",
        logoUrl = "",
        price = "€18.00",
        nextPayment = "12 Feb 2026",
        cycle = BillingCycle.MONTHS,
        frequency = 6,
        isActive = true
    )
)

@PreviewWallosDarkLight
@Composable
private fun SubscriptionsContentPreview() = WallosMobilePreviewTheme {
    SubscriptionsContent(uiState = SubscriptionsUiState(items = previewItems))
}

@PreviewWallosDarkLight
@Composable
private fun SubscriptionsContentLoadingPreview() = WallosMobilePreviewTheme {
    SubscriptionsContent(uiState = SubscriptionsUiState(isLoading = true))
}

@PreviewWallosDarkLight
@Composable
private fun SubscriptionsContentEmptyPreview() = WallosMobilePreviewTheme {
    SubscriptionsContent(uiState = SubscriptionsUiState())
}

@PreviewWallosDarkLight
@Composable
private fun SubscriptionsContentErrorPreview() = WallosMobilePreviewTheme {
    SubscriptionsContent(
        uiState = SubscriptionsUiState(
            error = NativeText.Simple("Couldn't reach that server. Check the URL and your connection.")
        )
    )
}

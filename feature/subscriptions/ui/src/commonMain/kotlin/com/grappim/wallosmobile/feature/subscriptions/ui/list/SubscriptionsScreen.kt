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
import com.grappim.wallosmobile.feature.subscriptions.ui.list.widgets.SubscriptionsFilterSheet
import com.grappim.wallosmobile.feature.subscriptions.ui.widgets.ConversionBanner
import com.grappim.wallosmobile.feature.subscriptions.ui.widgets.StaleBanner
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_empty
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_filter
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_filter_clear
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_filter_no_match
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_retry
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_title
import com.grappim.wallosmobile.uikit.WallosMobilePreviewTheme
import com.grappim.wallosmobile.uikit.utils.PreviewWallosDarkLight
import com.grappim.wallosmobile.uikit.widgets.topappbar.LocalTopBarConfig
import com.grappim.wallosmobile.uikit.widgets.topappbar.NavigationIconConfig
import com.grappim.wallosmobile.uikit.widgets.topappbar.TopBarActionTextButton
import com.grappim.wallosmobile.uikit.widgets.topappbar.TopBarConfig
import com.grappim.wallosmobile.utils.ui.NativeText
import com.grappim.wallosmobile.utils.ui.asString
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SubscriptionsScreen(
    onSubscriptionClick: (id: Int) -> Unit,
    viewModel: SubscriptionsViewModel = koinViewModel<SubscriptionsViewModel>()
) {
    val topBarController = LocalTopBarConfig.current
    val uiState by viewModel.uiState.collectAsState()

    // `uiState` is read once here on purpose: the sheet's callbacks are wired at construction and
    // never change, so the bar needs no re-declaration when the state does.
    val onFilterOpen = uiState.filters.onOpen

    LaunchedEffect(Unit) {
        topBarController.update(
            TopBarConfig(
                title = NativeText.Resource(RString.subscriptions_title),
                navigationIcon = NavigationIconConfig.Menu,
                // A text action, not `Icons.Default.FilterList` as plan §5.4 sketched: that icon is
                // not in `material-icons-core`, and one word costs less than growing the icon set.
                actions = persistentListOf(
                    TopBarActionTextButton(
                        text = NativeText.Resource(RString.subscriptions_filter),
                        onClick = onFilterOpen
                    )
                )
            )
        )
    }

    SubscriptionsContent(uiState = uiState, onSubscriptionClick = onSubscriptionClick)
}

@Composable
private fun SubscriptionsContent(
    uiState: SubscriptionsUiState,
    onSubscriptionClick: (id: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    PullToRefreshBox(
        modifier = modifier.fillMaxSize(),
        isRefreshing = uiState.isRefreshing,
        onRefresh = uiState.onRefresh
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // The banner pushes the rows down rather than covering them: what it says applies to
            // every one of them, and a failed refresh is not a reason to hide any (3.5). Retrying
            // from here is a refresh, so the pull indicator reports it.
            if (uiState.isStale) {
                StaleBanner(message = uiState.error, onRetryClick = uiState.onRefresh)
            }

            // Independent of the one above: a refresh can succeed and still leave prices in
            // currencies the instance was asked to convert and couldn't (3.11).
            if (uiState.isConversionUnavailable) {
                ConversionBanner()
            }

            // The list is always composed so that pull-to-refresh has something to pull, even when
            // it is empty — the states below draw on top of it.
            SubscriptionsList(
                uiState = uiState,
                onSubscriptionClick = onSubscriptionClick,
                modifier = Modifier.weight(1f)
            )
        }

        when {
            uiState.isLoading -> LoadingState()

            uiState.isFailed -> ErrorState(uiState = uiState)

            uiState.isEmpty -> EmptyState()

            // Rows exist and the filter excluded every one of them — which the user can undo, so
            // it says so and offers the undo rather than reading as an instance with nothing on it.
            uiState.isNoMatch -> NoMatchState(onClearClick = uiState.filters.onClear)
        }
    }

    if (uiState.filters.isVisible) {
        SubscriptionsFilterSheet(uiState = uiState.filters)
    }
}

@Composable
private fun SubscriptionsList(
    uiState: SubscriptionsUiState,
    onSubscriptionClick: (id: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(SCREEN_PADDING),
        verticalArrangement = Arrangement.spacedBy(ITEM_SPACING)
    ) {
        items(items = uiState.items, key = { it.id }) { item ->
            SubscriptionCard(item = item, onClick = { onSubscriptionClick(item.id) })
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
private fun NoMatchState(onClearClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(SCREEN_PADDING),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ITEM_SPACING, Alignment.CenterVertically)
    ) {
        Text(
            text = stringResource(RString.subscriptions_filter_no_match),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Button(onClick = onClearClick) {
            Text(stringResource(RString.subscriptions_filter_clear))
        }
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
    SubscriptionsContent(
        uiState = SubscriptionsUiState(items = previewItems, hasCachedRows = true),
        onSubscriptionClick = {}
    )
}

@PreviewWallosDarkLight
@Composable
private fun SubscriptionsContentNoMatchPreview() = WallosMobilePreviewTheme {
    SubscriptionsContent(uiState = SubscriptionsUiState(hasCachedRows = true), onSubscriptionClick = {})
}

@PreviewWallosDarkLight
@Composable
private fun SubscriptionsContentLoadingPreview() = WallosMobilePreviewTheme {
    SubscriptionsContent(uiState = SubscriptionsUiState(isLoading = true), onSubscriptionClick = {})
}

@PreviewWallosDarkLight
@Composable
private fun SubscriptionsContentEmptyPreview() = WallosMobilePreviewTheme {
    SubscriptionsContent(uiState = SubscriptionsUiState(), onSubscriptionClick = {})
}

@PreviewWallosDarkLight
@Composable
private fun SubscriptionsContentStalePreview() = WallosMobilePreviewTheme {
    SubscriptionsContent(
        uiState = SubscriptionsUiState(
            items = previewItems,
            hasCachedRows = true,
            error = NativeText.Simple("Couldn't reach that server. Check the URL and your connection.")
        ),
        onSubscriptionClick = {}
    )
}

@PreviewWallosDarkLight
@Composable
private fun SubscriptionsContentUnconvertedPreview() = WallosMobilePreviewTheme {
    SubscriptionsContent(
        uiState = SubscriptionsUiState(
            items = previewItems,
            hasCachedRows = true,
            isConversionUnavailable = true
        ),
        onSubscriptionClick = {}
    )
}

@PreviewWallosDarkLight
@Composable
private fun SubscriptionsContentErrorPreview() = WallosMobilePreviewTheme {
    SubscriptionsContent(
        uiState = SubscriptionsUiState(
            error = NativeText.Simple("Couldn't reach that server. Check the URL and your connection.")
        ),
        onSubscriptionClick = {}
    )
}

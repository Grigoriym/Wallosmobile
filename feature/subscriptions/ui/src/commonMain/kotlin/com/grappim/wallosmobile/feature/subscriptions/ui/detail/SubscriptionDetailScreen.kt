package com.grappim.wallosmobile.feature.subscriptions.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import com.grappim.wallosmobile.feature.subscriptions.domain.model.BillingCycle
import com.grappim.wallosmobile.feature.subscriptions.ui.widgets.InactiveBadge
import com.grappim.wallosmobile.feature.subscriptions.ui.widgets.SubscriptionLogo
import com.grappim.wallosmobile.feature.subscriptions.ui.widgets.cycleText
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.subscription_detail_category
import com.grappim.wallosmobile.strings.generated.resources.subscription_detail_cycle
import com.grappim.wallosmobile.strings.generated.resources.subscription_detail_next_payment
import com.grappim.wallosmobile.strings.generated.resources.subscription_detail_notes
import com.grappim.wallosmobile.strings.generated.resources.subscription_detail_payer
import com.grappim.wallosmobile.strings.generated.resources.subscription_detail_payment_method
import com.grappim.wallosmobile.strings.generated.resources.subscription_detail_start_date
import com.grappim.wallosmobile.strings.generated.resources.subscription_detail_url
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_retry
import com.grappim.wallosmobile.uikit.WallosMobilePreviewTheme
import com.grappim.wallosmobile.uikit.utils.PreviewWallosDarkLight
import com.grappim.wallosmobile.uikit.widgets.topappbar.LocalTopBarConfig
import com.grappim.wallosmobile.uikit.widgets.topappbar.NavigationIconConfig
import com.grappim.wallosmobile.uikit.widgets.topappbar.TopBarConfig
import com.grappim.wallosmobile.utils.ui.NativeText
import com.grappim.wallosmobile.utils.ui.asString
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun SubscriptionDetailScreen(
    subscriptionId: Int,
    onBackClick: () -> Unit,
    viewModel: SubscriptionDetailViewModel = koinViewModel { parametersOf(subscriptionId) }
) {
    val topBarController = LocalTopBarConfig.current
    val uiState by viewModel.uiState.collectAsState()
    val title = uiState.subscription?.name.orEmpty()

    // The title is the subscription's own name, so it can only be set once the row has loaded —
    // until then the bar shows the back arrow over an empty title.
    LaunchedEffect(title) {
        topBarController.update(
            TopBarConfig(
                title = NativeText.Simple(title),
                navigationIcon = NavigationIconConfig.Back(onBackClick)
            )
        )
    }

    SubscriptionDetailContent(uiState = uiState)
}

@Composable
private fun SubscriptionDetailContent(uiState: SubscriptionDetailUiState, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> LoadingState()
            uiState.error.isNotEmpty() -> ErrorState(uiState = uiState)
            uiState.subscription != null -> LoadedState(subscription = uiState.subscription)
        }
    }
}

@Composable
private fun LoadedState(subscription: SubscriptionDetailUiItem, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(SCREEN_PADDING),
        verticalArrangement = Arrangement.spacedBy(SECTION_SPACING)
    ) {
        Header(subscription = subscription)

        HorizontalDivider()

        DetailRow(
            label = RString.subscription_detail_cycle,
            value = cycleText(cycle = subscription.cycle, frequency = subscription.frequency)
        )
        DetailRow(label = RString.subscription_detail_next_payment, value = subscription.nextPayment)
        DetailRow(label = RString.subscription_detail_start_date, value = subscription.startDate)
        DetailRow(label = RString.subscription_detail_category, value = subscription.categoryName)
        DetailRow(
            label = RString.subscription_detail_payment_method,
            value = subscription.paymentMethodName
        )
        DetailRow(label = RString.subscription_detail_payer, value = subscription.payerName)
        DetailRow(label = RString.subscription_detail_url, value = subscription.url)
        DetailRow(label = RString.subscription_detail_notes, value = subscription.notes)
    }
}

@Composable
private fun Header(subscription: SubscriptionDetailUiItem, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HEADER_SPACING)
    ) {
        SubscriptionLogo(
            logoUrl = subscription.logoUrl,
            name = subscription.name,
            size = HEADER_LOGO_SIZE,
            placeholderTextStyle = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = subscription.name,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )

        Text(text = subscription.price, style = MaterialTheme.typography.titleLarge)

        if (!subscription.isActive) {
            InactiveBadge()
        }
    }
}

/** A row the instance has nothing for is left out entirely rather than drawn with a blank value. */
@Composable
private fun DetailRow(label: StringResource, value: String?, modifier: Modifier = Modifier) {
    if (!value.isNullOrBlank()) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(ROW_SPACING)
        ) {
            Text(
                text = stringResource(label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(text = value, style = MaterialTheme.typography.bodyLarge)
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
private fun ErrorState(uiState: SubscriptionDetailUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(SCREEN_PADDING),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SECTION_SPACING, Alignment.CenterVertically)
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
private val SECTION_SPACING = 16.dp
private val HEADER_SPACING = 8.dp
private val ROW_SPACING = 2.dp
private val HEADER_LOGO_SIZE = 96.dp

private val previewItem = SubscriptionDetailUiItem(
    name = "1&1 Telekom",
    logoUrl = "",
    price = "€18.00",
    cycle = BillingCycle.MONTHS,
    frequency = 6,
    nextPayment = "12 Feb 2026",
    startDate = "5 Mar 2024",
    categoryName = "Utilities",
    paymentMethodName = "Direct Debit",
    payerName = "gregorz",
    notes = "Contract runs to the end of 2027.",
    url = "https://www.1und1.de",
    isActive = true
)

@PreviewWallosDarkLight
@Composable
private fun SubscriptionDetailContentPreview() = WallosMobilePreviewTheme {
    SubscriptionDetailContent(uiState = SubscriptionDetailUiState(subscription = previewItem))
}

@PreviewWallosDarkLight
@Composable
private fun SubscriptionDetailContentSparsePreview() = WallosMobilePreviewTheme {
    SubscriptionDetailContent(
        uiState = SubscriptionDetailUiState(
            subscription = previewItem.copy(
                cycle = null,
                startDate = "",
                notes = "",
                url = "",
                isActive = false
            )
        )
    )
}

@PreviewWallosDarkLight
@Composable
private fun SubscriptionDetailContentLoadingPreview() = WallosMobilePreviewTheme {
    SubscriptionDetailContent(uiState = SubscriptionDetailUiState(isLoading = true))
}

@PreviewWallosDarkLight
@Composable
private fun SubscriptionDetailContentErrorPreview() = WallosMobilePreviewTheme {
    SubscriptionDetailContent(
        uiState = SubscriptionDetailUiState(error = NativeText.Simple("The server has no such item."))
    )
}

package com.grappim.wallosmobile.feature.dashboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grappim.wallosmobile.feature.dashboard.ui.widgets.MonthlyBudgetCard
import com.grappim.wallosmobile.feature.dashboard.ui.widgets.PeriodBudgetCard
import com.grappim.wallosmobile.feature.dashboard.ui.widgets.UpcomingPaymentRow
import com.grappim.wallosmobile.feature.dashboard.ui.widgets.YourSavingsCard
import com.grappim.wallosmobile.feature.dashboard.ui.widgets.YourSubscriptionsCard
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.dashboard_overdue_title
import com.grappim.wallosmobile.strings.generated.resources.dashboard_title
import com.grappim.wallosmobile.strings.generated.resources.dashboard_upcoming_empty
import com.grappim.wallosmobile.strings.generated.resources.dashboard_upcoming_title
import com.grappim.wallosmobile.uikit.WallosMobilePreviewTheme
import com.grappim.wallosmobile.uikit.utils.PreviewWallosDarkLight
import com.grappim.wallosmobile.uikit.widgets.topappbar.LocalTopBarConfig
import com.grappim.wallosmobile.uikit.widgets.topappbar.NavigationIconConfig
import com.grappim.wallosmobile.uikit.widgets.topappbar.TopBarConfig
import com.grappim.wallosmobile.utils.ui.NativeText
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DashboardScreen(
    onSubscriptionClick: (id: Int) -> Unit,
    viewModel: DashboardViewModel = koinViewModel<DashboardViewModel>()
) {
    val topBarController = LocalTopBarConfig.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        topBarController.update(
            TopBarConfig(
                title = NativeText.Resource(RString.dashboard_title),
                navigationIcon = NavigationIconConfig.Menu
            )
        )
    }

    DashboardContent(uiState = uiState, onSubscriptionClick = onSubscriptionClick)
}

@Composable
private fun DashboardContent(
    uiState: DashboardUiState,
    onSubscriptionClick: (id: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(SCREEN_PADDING),
            verticalArrangement = Arrangement.spacedBy(ITEM_SPACING)
        ) {
            item { MonthlyBudgetCard(uiState = uiState.monthlyBudget, onRetryClick = uiState.onRetryClick) }

            if (!uiState.periodBudget.isHidden) {
                item { PeriodBudgetCard(uiState = uiState.periodBudget, onRetryClick = uiState.onRetryClick) }
            }

            if (uiState.overdueRenewals.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(RString.dashboard_overdue_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                items(items = uiState.overdueRenewals, key = { it.id }) { item ->
                    UpcomingPaymentRow(item = item, onClick = { onSubscriptionClick(item.id) })
                }
            }

            item {
                Text(
                    text = stringResource(RString.dashboard_upcoming_title),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (uiState.upcomingPayments.isEmpty()) {
                item {
                    Text(
                        text = stringResource(RString.dashboard_upcoming_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(items = uiState.upcomingPayments, key = { it.id }) { item ->
                    UpcomingPaymentRow(item = item, onClick = { onSubscriptionClick(item.id) })
                }
            }

            if (uiState.yourSubscriptions.activeCount > 0) {
                item { YourSubscriptionsCard(uiState = uiState.yourSubscriptions) }
            }

            if (uiState.yourSavings.inactiveCount > 0) {
                item { YourSavingsCard(uiState = uiState.yourSavings) }
            }
        }
    }
}

private val SCREEN_PADDING = 16.dp
private val ITEM_SPACING = 8.dp

private val previewItems = persistentListOf(
    UpcomingPaymentUiItem(id = 1, name = "Disney+", price = "€8.99", nextPayment = "10 Mar 2026"),
    UpcomingPaymentUiItem(id = 2, name = "1&1 Telekom", price = "€18.00", nextPayment = "12 Feb 2026")
)

private val previewOverdueItems = persistentListOf(
    UpcomingPaymentUiItem(id = 3, name = "Netflix", price = "€12.99", nextPayment = "1 Jan 2026")
)

@PreviewWallosDarkLight
@Composable
private fun DashboardContentPreview() = WallosMobilePreviewTheme {
    DashboardContent(
        uiState = DashboardUiState(
            monthlyBudget = MonthlyBudgetCardUiState(
                title = "August 2026",
                costAmount = "€42.00",
                budgetAmount = "€100.00",
                usedPercent = "42.00%",
                remainingAmount = "€58.00"
            ),
            periodBudget = PeriodBudgetCardUiState(
                periodLabel = "Aug 1 - Aug 31",
                budgetAmount = "€100.00",
                remainingAmount = "€58.00"
            ),
            overdueRenewals = previewOverdueItems,
            upcomingPayments = previewItems,
            yourSubscriptions = YourSubscriptionsCardUiState(
                activeCount = 28,
                monthlyCost = "€711.39",
                yearlyCost = "€8,536.68"
            ),
            yourSavings = YourSavingsCardUiState(inactiveCount = 2, savingsPerMonth = "€19.98")
        ),
        onSubscriptionClick = {}
    )
}

@PreviewWallosDarkLight
@Composable
private fun DashboardContentLoadingPreview() = WallosMobilePreviewTheme {
    DashboardContent(uiState = DashboardUiState(isLoading = true), onSubscriptionClick = {})
}

@PreviewWallosDarkLight
@Composable
private fun DashboardContentBudgetHiddenPreview() = WallosMobilePreviewTheme {
    DashboardContent(
        uiState = DashboardUiState(
            monthlyBudget = MonthlyBudgetCardUiState(title = "August 2026", costAmount = "€42.00"),
            periodBudget = PeriodBudgetCardUiState(isHidden = true),
            upcomingPayments = previewItems
        ),
        onSubscriptionClick = {}
    )
}

@PreviewWallosDarkLight
@Composable
private fun DashboardContentEmptyPreview() = WallosMobilePreviewTheme {
    DashboardContent(
        uiState = DashboardUiState(
            monthlyBudget = MonthlyBudgetCardUiState(title = "August 2026", costAmount = "€0.00"),
            periodBudget = PeriodBudgetCardUiState(isHidden = true)
        ),
        onSubscriptionClick = {}
    )
}

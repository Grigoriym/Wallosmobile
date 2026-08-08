package com.grappim.wallosmobile.feature.dashboard.ui.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grappim.wallosmobile.feature.dashboard.ui.MonthlyBudgetCardUiState
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.dashboard_budget_amount
import com.grappim.wallosmobile.strings.generated.resources.dashboard_budget_over
import com.grappim.wallosmobile.strings.generated.resources.dashboard_budget_remaining
import com.grappim.wallosmobile.strings.generated.resources.dashboard_budget_used
import com.grappim.wallosmobile.strings.generated.resources.dashboard_monthly_budget_title
import com.grappim.wallosmobile.strings.generated.resources.dashboard_monthly_cost_title
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_retry
import com.grappim.wallosmobile.uikit.WallosMobilePreviewTheme
import com.grappim.wallosmobile.uikit.utils.PreviewWallosDarkLight
import com.grappim.wallosmobile.utils.ui.NativeText
import com.grappim.wallosmobile.utils.ui.asString
import org.jetbrains.compose.resources.stringResource

/** Cost always shows; the budget/used/remaining/over-budget rows only when [MonthlyBudgetCardUiState.budgetAmount] is set. */
@Composable
fun MonthlyBudgetCard(uiState: MonthlyBudgetCardUiState, onRetryClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CARD_PADDING),
            verticalArrangement = Arrangement.spacedBy(LINE_SPACING)
        ) {
            Text(
                text = stringResource(RString.dashboard_monthly_budget_title),
                style = MaterialTheme.typography.titleMedium
            )

            if (uiState.error.isNotEmpty()) {
                Text(
                    text = uiState.error.asString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                TextButton(onClick = onRetryClick) {
                    Text(stringResource(RString.subscriptions_retry))
                }
            } else {
                if (uiState.title.isNotEmpty()) {
                    Text(
                        text = uiState.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = stringResource(RString.dashboard_monthly_cost_title),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(text = uiState.costAmount, style = MaterialTheme.typography.headlineMedium)

                if (uiState.budgetAmount.isNotEmpty()) {
                    Text(
                        text = stringResource(RString.dashboard_budget_amount, uiState.budgetAmount),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stringResource(RString.dashboard_budget_used, uiState.usedPercent),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stringResource(RString.dashboard_budget_remaining, uiState.remainingAmount),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (uiState.isOverBudget) {
                        Text(
                            text = stringResource(RString.dashboard_budget_over, uiState.overBudgetAmount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

private val CARD_PADDING = 16.dp
private val LINE_SPACING = 4.dp

@PreviewWallosDarkLight
@Composable
private fun MonthlyBudgetCardPreview() = WallosMobilePreviewTheme {
    MonthlyBudgetCard(
        uiState = MonthlyBudgetCardUiState(title = "August 2026", costAmount = "€42.00"),
        onRetryClick = {}
    )
}

@PreviewWallosDarkLight
@Composable
private fun MonthlyBudgetCardWithBudgetPreview() = WallosMobilePreviewTheme {
    MonthlyBudgetCard(
        uiState = MonthlyBudgetCardUiState(
            title = "August 2026",
            costAmount = "€42.00",
            budgetAmount = "€100.00",
            usedPercent = "42.00%",
            remainingAmount = "€58.00"
        ),
        onRetryClick = {}
    )
}

@PreviewWallosDarkLight
@Composable
private fun MonthlyBudgetCardOverBudgetPreview() = WallosMobilePreviewTheme {
    MonthlyBudgetCard(
        uiState = MonthlyBudgetCardUiState(
            title = "August 2026",
            costAmount = "€142.00",
            budgetAmount = "€100.00",
            usedPercent = "100.00%",
            remainingAmount = "€0.00",
            isOverBudget = true,
            overBudgetAmount = "€42.00"
        ),
        onRetryClick = {}
    )
}

@PreviewWallosDarkLight
@Composable
private fun MonthlyBudgetCardErrorPreview() = WallosMobilePreviewTheme {
    MonthlyBudgetCard(
        uiState = MonthlyBudgetCardUiState(
            error = NativeText.Simple("Couldn't reach that server. Check the URL and your connection.")
        ),
        onRetryClick = {}
    )
}

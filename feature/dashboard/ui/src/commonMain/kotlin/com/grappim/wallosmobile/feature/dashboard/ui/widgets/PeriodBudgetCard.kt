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
import com.grappim.wallosmobile.feature.dashboard.ui.PeriodBudgetCardUiState
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.dashboard_budget_over
import com.grappim.wallosmobile.strings.generated.resources.dashboard_budget_remaining
import com.grappim.wallosmobile.strings.generated.resources.dashboard_period_budget_title
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_retry
import com.grappim.wallosmobile.uikit.WallosMobilePreviewTheme
import com.grappim.wallosmobile.uikit.utils.PreviewWallosDarkLight
import com.grappim.wallosmobile.utils.ui.NativeText
import com.grappim.wallosmobile.utils.ui.asString
import org.jetbrains.compose.resources.stringResource

/** Draws nothing when [PeriodBudgetCardUiState.isHidden] — the caller decides whether to compose this at all. */
@Composable
fun PeriodBudgetCard(uiState: PeriodBudgetCardUiState, onRetryClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CARD_PADDING),
            verticalArrangement = Arrangement.spacedBy(LINE_SPACING)
        ) {
            Text(
                text = stringResource(RString.dashboard_period_budget_title),
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
                if (uiState.periodLabel.isNotEmpty()) {
                    Text(
                        text = uiState.periodLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = stringResource(RString.dashboard_budget_remaining, uiState.remainingAmount),
                    style = MaterialTheme.typography.headlineMedium
                )
                if (uiState.isOverBudget) {
                    Text(
                        text = stringResource(RString.dashboard_budget_over, uiState.amountOverBudget),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

private val CARD_PADDING = 16.dp
private val LINE_SPACING = 4.dp

@PreviewWallosDarkLight
@Composable
private fun PeriodBudgetCardPreview() = WallosMobilePreviewTheme {
    PeriodBudgetCard(
        uiState = PeriodBudgetCardUiState(
            periodLabel = "Aug 1 - Aug 31",
            budgetAmount = "€100.00",
            remainingAmount = "€58.00"
        ),
        onRetryClick = {}
    )
}

@PreviewWallosDarkLight
@Composable
private fun PeriodBudgetCardOverBudgetPreview() = WallosMobilePreviewTheme {
    PeriodBudgetCard(
        uiState = PeriodBudgetCardUiState(
            periodLabel = "Aug 1 - Aug 31",
            budgetAmount = "€100.00",
            remainingAmount = "€0.00",
            isOverBudget = true,
            amountOverBudget = "€12.50"
        ),
        onRetryClick = {}
    )
}

@PreviewWallosDarkLight
@Composable
private fun PeriodBudgetCardErrorPreview() = WallosMobilePreviewTheme {
    PeriodBudgetCard(
        uiState = PeriodBudgetCardUiState(
            error = NativeText.Simple("Couldn't reach that server. Check the URL and your connection.")
        ),
        onRetryClick = {}
    )
}

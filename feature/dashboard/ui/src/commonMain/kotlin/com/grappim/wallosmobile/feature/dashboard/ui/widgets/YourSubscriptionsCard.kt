package com.grappim.wallosmobile.feature.dashboard.ui.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grappim.wallosmobile.feature.dashboard.ui.YourSubscriptionsCardUiState
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.dashboard_active_subscriptions
import com.grappim.wallosmobile.strings.generated.resources.dashboard_monthly_cost_amount
import com.grappim.wallosmobile.strings.generated.resources.dashboard_yearly_cost
import com.grappim.wallosmobile.strings.generated.resources.dashboard_your_subscriptions_title
import com.grappim.wallosmobile.uikit.WallosMobilePreviewTheme
import com.grappim.wallosmobile.uikit.utils.PreviewWallosDarkLight
import org.jetbrains.compose.resources.stringResource

/** The caller composes this only when [YourSubscriptionsCardUiState.activeCount] is greater than 0. */
@Composable
fun YourSubscriptionsCard(uiState: YourSubscriptionsCardUiState, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CARD_PADDING),
            verticalArrangement = Arrangement.spacedBy(LINE_SPACING)
        ) {
            Text(
                text = stringResource(RString.dashboard_your_subscriptions_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(RString.dashboard_active_subscriptions, uiState.activeCount),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(RString.dashboard_monthly_cost_amount, uiState.monthlyCost),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(RString.dashboard_yearly_cost, uiState.yearlyCost),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private val CARD_PADDING = 16.dp
private val LINE_SPACING = 4.dp

@PreviewWallosDarkLight
@Composable
private fun YourSubscriptionsCardPreview() = WallosMobilePreviewTheme {
    YourSubscriptionsCard(
        uiState = YourSubscriptionsCardUiState(activeCount = 28, monthlyCost = "€711.39", yearlyCost = "€8,536.68")
    )
}

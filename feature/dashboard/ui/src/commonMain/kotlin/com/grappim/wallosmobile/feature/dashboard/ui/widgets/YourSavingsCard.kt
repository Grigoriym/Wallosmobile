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
import com.grappim.wallosmobile.feature.dashboard.ui.YourSavingsCardUiState
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.dashboard_inactive_subscriptions
import com.grappim.wallosmobile.strings.generated.resources.dashboard_monthly_savings
import com.grappim.wallosmobile.strings.generated.resources.dashboard_your_savings_title
import com.grappim.wallosmobile.uikit.WallosMobilePreviewTheme
import com.grappim.wallosmobile.uikit.utils.PreviewWallosDarkLight
import org.jetbrains.compose.resources.stringResource

/** The caller composes this only when [YourSavingsCardUiState.inactiveCount] is greater than 0. */
@Composable
fun YourSavingsCard(uiState: YourSavingsCardUiState, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CARD_PADDING),
            verticalArrangement = Arrangement.spacedBy(LINE_SPACING)
        ) {
            Text(
                text = stringResource(RString.dashboard_your_savings_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(RString.dashboard_inactive_subscriptions, uiState.inactiveCount),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(RString.dashboard_monthly_savings, uiState.savingsPerMonth),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private val CARD_PADDING = 16.dp
private val LINE_SPACING = 4.dp

@PreviewWallosDarkLight
@Composable
private fun YourSavingsCardPreview() = WallosMobilePreviewTheme {
    YourSavingsCard(uiState = YourSavingsCardUiState(inactiveCount = 2, savingsPerMonth = "€19.98"))
}

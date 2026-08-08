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
import com.grappim.wallosmobile.feature.dashboard.ui.MonthlyCostCardUiState
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.dashboard_monthly_cost_title
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_retry
import com.grappim.wallosmobile.uikit.WallosMobilePreviewTheme
import com.grappim.wallosmobile.uikit.utils.PreviewWallosDarkLight
import com.grappim.wallosmobile.utils.ui.NativeText
import com.grappim.wallosmobile.utils.ui.asString
import org.jetbrains.compose.resources.stringResource

@Composable
fun MonthlyCostCard(uiState: MonthlyCostCardUiState, onRetryClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CARD_PADDING),
            verticalArrangement = Arrangement.spacedBy(LINE_SPACING)
        ) {
            Text(
                text = stringResource(RString.dashboard_monthly_cost_title),
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
                Text(text = uiState.amount, style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}

private val CARD_PADDING = 16.dp
private val LINE_SPACING = 4.dp

@PreviewWallosDarkLight
@Composable
private fun MonthlyCostCardPreview() = WallosMobilePreviewTheme {
    MonthlyCostCard(
        uiState = MonthlyCostCardUiState(title = "August 2026", amount = "€42.00"),
        onRetryClick = {}
    )
}

@PreviewWallosDarkLight
@Composable
private fun MonthlyCostCardErrorPreview() = WallosMobilePreviewTheme {
    MonthlyCostCard(
        uiState = MonthlyCostCardUiState(
            error = NativeText.Simple("Couldn't reach that server. Check the URL and your connection.")
        ),
        onRetryClick = {}
    )
}

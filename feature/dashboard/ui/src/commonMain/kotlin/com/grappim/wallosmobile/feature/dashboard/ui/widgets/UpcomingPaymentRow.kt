package com.grappim.wallosmobile.feature.dashboard.ui.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grappim.wallosmobile.feature.dashboard.ui.UpcomingPaymentUiItem
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_next_payment
import com.grappim.wallosmobile.uikit.WallosMobilePreviewTheme
import com.grappim.wallosmobile.uikit.utils.PreviewWallosDarkLight
import org.jetbrains.compose.resources.stringResource

/** No logo, cycle or inactive badge: 8.2 already filtered to active rows, and the card doesn't need the rest. */
@Composable
fun UpcomingPaymentRow(item: UpcomingPaymentUiItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CARD_PADDING),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CARD_PADDING)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.nextPayment.isNotEmpty()) {
                    Text(
                        text = stringResource(RString.subscriptions_next_payment, item.nextPayment),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(text = item.price, style = MaterialTheme.typography.titleMedium)
        }
    }
}

private val CARD_PADDING = 16.dp

@PreviewWallosDarkLight
@Composable
private fun UpcomingPaymentRowPreview() = WallosMobilePreviewTheme {
    UpcomingPaymentRow(
        item = UpcomingPaymentUiItem(id = 1, name = "Disney+", price = "€8.99", nextPayment = "10 Mar 2026"),
        onClick = {}
    )
}

package com.grappim.wallosmobile.feature.subscriptions.ui.list.widgets

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
import com.grappim.wallosmobile.feature.subscriptions.domain.model.BillingCycle
import com.grappim.wallosmobile.feature.subscriptions.ui.list.SubscriptionUiItem
import com.grappim.wallosmobile.feature.subscriptions.ui.widgets.InactiveBadge
import com.grappim.wallosmobile.feature.subscriptions.ui.widgets.SubscriptionLogo
import com.grappim.wallosmobile.feature.subscriptions.ui.widgets.cycleText
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_next_payment
import com.grappim.wallosmobile.uikit.WallosMobilePreviewTheme
import com.grappim.wallosmobile.uikit.utils.PreviewWallosDarkLight
import org.jetbrains.compose.resources.stringResource

@Composable
fun SubscriptionCard(item: SubscriptionUiItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CARD_PADDING),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CARD_PADDING)
        ) {
            SubscriptionLogo(
                logoUrl = item.logoUrl,
                name = item.name,
                size = LOGO_SIZE,
                logoRefreshToken = item.logoRefreshToken
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val cycleText = cycleText(cycle = item.cycle, frequency = item.frequency)
                if (cycleText != null) {
                    Text(
                        text = cycleText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (item.nextPayment.isNotEmpty()) {
                    Text(
                        text = stringResource(RString.subscriptions_next_payment, item.nextPayment),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(BADGE_SPACING)
            ) {
                Text(text = item.price, style = MaterialTheme.typography.titleMedium)

                if (!item.isActive) {
                    InactiveBadge()
                }
            }
        }
    }
}

private val CARD_PADDING = 16.dp
private val LOGO_SIZE = 48.dp
private val BADGE_SPACING = 4.dp

@PreviewWallosDarkLight
@Composable
private fun SubscriptionCardPreview() = WallosMobilePreviewTheme {
    SubscriptionCard(
        item = SubscriptionUiItem(
            id = 1,
            name = "Disney+",
            logoUrl = "",
            price = "€8.99",
            nextPayment = "10 Mar 2026",
            cycle = BillingCycle.MONTHS,
            frequency = 1,
            isActive = true
        ),
        onClick = {}
    )
}

@PreviewWallosDarkLight
@Composable
private fun SubscriptionCardInactivePreview() = WallosMobilePreviewTheme {
    SubscriptionCard(
        item = SubscriptionUiItem(
            id = 2,
            name = "A subscription with a very long name indeed",
            logoUrl = "",
            price = "€1,234.56",
            nextPayment = "31 Dec 2026",
            cycle = BillingCycle.MONTHS,
            frequency = 6,
            isActive = false
        ),
        onClick = {}
    )
}

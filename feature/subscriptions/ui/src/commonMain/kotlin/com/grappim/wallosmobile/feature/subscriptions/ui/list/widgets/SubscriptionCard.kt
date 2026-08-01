package com.grappim.wallosmobile.feature.subscriptions.ui.list.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.grappim.wallosmobile.feature.subscriptions.domain.model.BillingCycle
import com.grappim.wallosmobile.feature.subscriptions.ui.list.SubscriptionUiItem
import com.grappim.wallosmobile.strings.RPlurals
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.subscription_cycle_days
import com.grappim.wallosmobile.strings.generated.resources.subscription_cycle_months
import com.grappim.wallosmobile.strings.generated.resources.subscription_cycle_one_time
import com.grappim.wallosmobile.strings.generated.resources.subscription_cycle_weeks
import com.grappim.wallosmobile.strings.generated.resources.subscription_cycle_years
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_inactive
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_next_payment
import com.grappim.wallosmobile.uikit.WallosMobilePreviewTheme
import com.grappim.wallosmobile.uikit.utils.PreviewWallosDarkLight
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun SubscriptionCard(item: SubscriptionUiItem, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CARD_PADDING),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CARD_PADDING)
        ) {
            SubscriptionLogo(logoUrl = item.logoUrl, name = item.name)

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

/**
 * The bare filename is empty for a subscription that never got a logo, and Wallos serves the
 * uploads directory without authentication, so a plain URL load is all this needs.
 */
@Composable
private fun SubscriptionLogo(logoUrl: String, name: String, modifier: Modifier = Modifier) {
    val shape = MaterialTheme.shapes.small

    if (logoUrl.isEmpty()) {
        Surface(
            modifier = modifier.size(LOGO_SIZE),
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        AsyncImage(
            model = logoUrl,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier
                .size(LOGO_SIZE)
                .clip(shape)
        )
    }
}

@Composable
private fun InactiveBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            modifier = Modifier.padding(horizontal = BADGE_H_PADDING, vertical = BADGE_V_PADDING),
            text = stringResource(RString.subscriptions_inactive),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

/**
 * "every 6 months" is a plural, so it can only be built where `pluralStringResource` can be
 * called — which is why 2.2 could not put it in `utils:formatter:datetime` and the state carries
 * the [BillingCycle] rather than a string. `null` for an unknown cycle: the card drops the line
 * instead of guessing a unit.
 */
@Composable
private fun cycleText(cycle: BillingCycle?, frequency: Int): String? = when (cycle) {
    null -> null
    BillingCycle.ONE_TIME -> stringResource(RString.subscription_cycle_one_time)
    BillingCycle.DAYS -> pluralStringResource(RPlurals.subscription_cycle_days, frequency, frequency)
    BillingCycle.WEEKS -> pluralStringResource(RPlurals.subscription_cycle_weeks, frequency, frequency)
    BillingCycle.MONTHS -> pluralStringResource(RPlurals.subscription_cycle_months, frequency, frequency)
    BillingCycle.YEARS -> pluralStringResource(RPlurals.subscription_cycle_years, frequency, frequency)
}

private val CARD_PADDING = 16.dp
private val LOGO_SIZE = 48.dp
private val BADGE_SPACING = 4.dp
private val BADGE_H_PADDING = 8.dp
private val BADGE_V_PADDING = 2.dp

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
        )
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
        )
    )
}

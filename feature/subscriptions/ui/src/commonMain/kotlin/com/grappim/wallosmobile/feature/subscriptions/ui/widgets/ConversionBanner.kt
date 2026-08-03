package com.grappim.wallosmobile.feature.subscriptions.ui.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_conversion_body
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_conversion_title
import com.grappim.wallosmobile.uikit.WallosMobilePreviewTheme
import com.grappim.wallosmobile.uikit.utils.PreviewWallosDarkLight
import org.jetbrains.compose.resources.stringResource

/**
 * The visible half of 3.11: the instance was asked to show one currency, cannot, and said nothing
 * about it — `get_subscriptions.php` returns unconverted prices with an empty `notes` array (API
 * doc §5.5). The prices below are right; what is wrong is that they don't compare, which matters
 * most to 3.6's sort by price.
 *
 * No retry, unlike [StaleBanner]: nothing the app can send fixes this. The fix is a Fixer API key
 * on the server, so the banner names it and stops there.
 *
 * `surfaceVariant` for the same reason the stale banner uses it — the data on screen is real, and
 * an `errorContainer` would say otherwise.
 */
@Composable
internal fun ConversionBanner(modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(
            modifier = Modifier.padding(BANNER_PADDING),
            verticalArrangement = Arrangement.spacedBy(LINE_SPACING)
        ) {
            Text(
                text = stringResource(RString.subscriptions_conversion_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(RString.subscriptions_conversion_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private val BANNER_PADDING = 16.dp
private val LINE_SPACING = 2.dp

@PreviewWallosDarkLight
@Composable
private fun ConversionBannerPreview() = WallosMobilePreviewTheme {
    ConversionBanner()
}

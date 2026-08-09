package com.grappim.wallosmobile.feature.paymentmethods.ui.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import com.grappim.wallosmobile.uikit.WallosMobilePreviewTheme
import com.grappim.wallosmobile.uikit.utils.PreviewWallosDarkLight

/**
 * Mirrors `feature:subscriptions:ui`'s `SubscriptionLogo` — a failed load falls back to the same
 * initial placeholder a missing icon draws (3.12's own reasoning) — without that widget's
 * `logoRefreshToken`: this list reloads its whole state on every open (no cache, this milestone's
 * preamble), so there is no already-`Error` request Coil needs coaxing to retry.
 */
@Composable
internal fun PaymentMethodIcon(iconUrl: String, name: String, size: Dp, modifier: Modifier = Modifier) {
    val shape = MaterialTheme.shapes.small

    if (iconUrl.isEmpty()) {
        IconPlaceholder(name = name, modifier = modifier.size(size))
    } else {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalPlatformContext.current).data(iconUrl).build(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            error = { IconPlaceholder(name = name, modifier = Modifier.fillMaxSize()) },
            modifier = modifier.size(size).clip(shape)
        )
    }
}

@Composable
private fun IconPlaceholder(name: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceVariant) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = name.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@PreviewWallosDarkLight
@Composable
private fun PaymentMethodIconPreview() = WallosMobilePreviewTheme {
    PaymentMethodIcon(iconUrl = "", name = "PayPal", size = 40.dp)
}

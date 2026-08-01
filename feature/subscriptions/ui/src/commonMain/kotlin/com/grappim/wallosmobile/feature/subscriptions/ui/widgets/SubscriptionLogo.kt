package com.grappim.wallosmobile.feature.subscriptions.ui.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.grappim.wallosmobile.uikit.WallosMobilePreviewTheme
import com.grappim.wallosmobile.uikit.utils.PreviewWallosDarkLight

/**
 * The bare filename is empty for a subscription that never got a logo, and Wallos serves the
 * uploads directory without authentication, so a plain URL load is all this needs.
 *
 * Shared by the card and the detail header, which differ only in [size] and in the size of the
 * initial the placeholder draws.
 */
@Composable
internal fun SubscriptionLogo(
    logoUrl: String,
    name: String,
    size: Dp,
    modifier: Modifier = Modifier,
    placeholderTextStyle: TextStyle = MaterialTheme.typography.titleMedium
) {
    val shape = MaterialTheme.shapes.small

    if (logoUrl.isEmpty()) {
        Surface(
            modifier = modifier.size(size),
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = name.take(1).uppercase(),
                    style = placeholderTextStyle,
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
                .size(size)
                .clip(shape)
        )
    }
}

@PreviewWallosDarkLight
@Composable
private fun SubscriptionLogoPreview() = WallosMobilePreviewTheme {
    SubscriptionLogo(logoUrl = "", name = "Disney+", size = 48.dp)
}

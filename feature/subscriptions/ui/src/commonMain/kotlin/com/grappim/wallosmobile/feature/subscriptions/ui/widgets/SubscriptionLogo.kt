package com.grappim.wallosmobile.feature.subscriptions.ui.widgets

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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.grappim.wallosmobile.uikit.WallosMobilePreviewTheme
import com.grappim.wallosmobile.uikit.utils.PreviewWallosDarkLight

/**
 * The bare filename is empty for a subscription that never got a logo, and Wallos serves the
 * uploads directory without authentication, so a URL is all the model needs to be.
 *
 * Shared by the card and the detail header, which differ only in [size] and in the size of the
 * initial the placeholder draws.
 *
 * **A failed load draws the same placeholder as a missing one** (3.12): branching on the empty
 * filename alone left every row of a server the loader couldn't reach as a blank gap, with a
 * perfectly good fallback sitting unused. `SubcomposeAsyncImage` rather than `AsyncImage` because
 * the fallback is an initial to lay out, and `AsyncImage`'s `error` slot takes a `Painter`.
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
        LogoPlaceholder(
            name = name,
            shape = shape,
            textStyle = placeholderTextStyle,
            modifier = modifier.size(size)
        )
    } else {
        SubcomposeAsyncImage(
            model = logoUrl,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            error = {
                LogoPlaceholder(
                    name = name,
                    shape = shape,
                    textStyle = placeholderTextStyle,
                    modifier = Modifier.fillMaxSize()
                )
            },
            modifier = modifier
                .size(size)
                .clip(shape)
        )
    }
}

@Composable
private fun LogoPlaceholder(name: String, shape: Shape, textStyle: TextStyle, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = name.take(1).uppercase(),
                style = textStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@PreviewWallosDarkLight
@Composable
private fun SubscriptionLogoPreview() = WallosMobilePreviewTheme {
    SubscriptionLogo(logoUrl = "", name = "Disney+", size = 48.dp)
}

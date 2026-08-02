package com.grappim.wallosmobile.feature.subscriptions.ui.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_retry
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_stale_offline
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_stale_title
import com.grappim.wallosmobile.uikit.WallosMobilePreviewTheme
import com.grappim.wallosmobile.uikit.utils.PreviewWallosDarkLight
import com.grappim.wallosmobile.uikit.widgets.network.LocalIsOffline
import com.grappim.wallosmobile.utils.ui.NativeText
import com.grappim.wallosmobile.utils.ui.asString
import org.jetbrains.compose.resources.stringResource

/**
 * The visible half of the offline-first repository (3.5): rows the last refresh couldn't confirm
 * stay on screen with this above them, instead of being replaced by a full-screen error.
 *
 * The reason line comes from [LocalIsOffline] rather than from [message] when the device has no
 * network — a refresh that never left the phone fails as "check the URL and your connection",
 * which sends the user to look at a server that is not the problem.
 *
 * Not `errorContainer`: what is on screen is real data, and the failure is that it might be old.
 */
@Composable
internal fun StaleBanner(message: NativeText, onRetryClick: () -> Unit, modifier: Modifier = Modifier) {
    val isOffline = LocalIsOffline.current

    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(
            modifier = Modifier.padding(start = BANNER_PADDING, top = BANNER_PADDING, bottom = BANNER_PADDING),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BANNER_SPACING)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(LINE_SPACING)
            ) {
                Text(
                    text = stringResource(RString.subscriptions_stale_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (isOffline) {
                        stringResource(RString.subscriptions_stale_offline)
                    } else {
                        message.asString()
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextButton(onClick = onRetryClick) {
                Text(stringResource(RString.subscriptions_retry))
            }
        }
    }
}

private val BANNER_PADDING = 16.dp
private val BANNER_SPACING = 8.dp
private val LINE_SPACING = 2.dp

@PreviewWallosDarkLight
@Composable
private fun StaleBannerPreview() = WallosMobilePreviewTheme {
    StaleBanner(
        message = NativeText.Simple("Couldn't reach that server. Check the URL and your connection."),
        onRetryClick = {}
    )
}

@PreviewWallosDarkLight
@Composable
private fun StaleBannerOfflinePreview() = WallosMobilePreviewTheme {
    CompositionLocalProvider(LocalIsOffline provides true) {
        StaleBanner(message = NativeText.Simple("ignored while offline"), onRetryClick = {})
    }
}

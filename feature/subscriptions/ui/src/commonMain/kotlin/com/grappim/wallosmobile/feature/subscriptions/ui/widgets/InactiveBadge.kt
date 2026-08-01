package com.grappim.wallosmobile.feature.subscriptions.ui.widgets

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_inactive
import com.grappim.wallosmobile.uikit.WallosMobilePreviewTheme
import com.grappim.wallosmobile.uikit.utils.PreviewWallosDarkLight
import org.jetbrains.compose.resources.stringResource

/** Shown on the card and on the detail header — inactive is the only state Wallos flags. */
@Composable
internal fun InactiveBadge(modifier: Modifier = Modifier) {
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

private val BADGE_H_PADDING = 8.dp
private val BADGE_V_PADDING = 2.dp

@PreviewWallosDarkLight
@Composable
private fun InactiveBadgePreview() = WallosMobilePreviewTheme {
    InactiveBadge()
}

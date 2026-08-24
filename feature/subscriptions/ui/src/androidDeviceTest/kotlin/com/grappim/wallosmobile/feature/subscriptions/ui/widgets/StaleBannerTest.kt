package com.grappim.wallosmobile.feature.subscriptions.ui.widgets

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_retry
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_stale_offline
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_stale_title
import com.grappim.wallosmobile.uikit.WallosMobilePreviewTheme
import com.grappim.wallosmobile.uikit.widgets.network.LocalIsOffline
import com.grappim.wallosmobile.utils.ui.NativeText
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertTrue

/** 19.2's banner half — `WallosMobilePreviewTheme` supplies [LocalIsOffline], which has no default. */
class StaleBannerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun onlineShowsMessageAndRetries() {
        var retried = false
        val message = NativeText.Simple("Couldn't reach that server. Check the URL and your connection.")
        composeTestRule.setContent {
            WallosMobilePreviewTheme {
                StaleBanner(message = message, onRetryClick = { retried = true })
            }
        }

        val titleText = runBlocking { getString(RString.subscriptions_stale_title) }
        composeTestRule.onNodeWithText(titleText).assertExists()
        composeTestRule.onNodeWithText(message.text).assertExists()

        val retryText = runBlocking { getString(RString.subscriptions_retry) }
        composeTestRule.onNodeWithText(retryText).performClick()
        assertTrue(retried)
    }

    @Test
    fun offlineShowsOfflineReasonInstead() {
        composeTestRule.setContent {
            WallosMobilePreviewTheme {
                CompositionLocalProvider(LocalIsOffline provides true) {
                    StaleBanner(message = NativeText.Simple("ignored while offline"), onRetryClick = {})
                }
            }
        }

        val offlineText = runBlocking { getString(RString.subscriptions_stale_offline) }
        composeTestRule.onNodeWithText(offlineText).assertExists()
    }
}

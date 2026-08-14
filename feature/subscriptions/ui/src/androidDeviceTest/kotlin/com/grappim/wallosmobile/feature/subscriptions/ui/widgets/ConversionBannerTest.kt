package com.grappim.wallosmobile.feature.subscriptions.ui.widgets

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_conversion_body
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_conversion_title
import com.grappim.wallosmobile.uikit.WallosMobilePreviewTheme
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.junit.Rule
import kotlin.test.Test

/** 19.2's banner half — no actions on this one (3.11's own `Note:`: nothing the app can send fixes it). */
class ConversionBannerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun showsTitleAndBody() {
        composeTestRule.setContent {
            WallosMobilePreviewTheme {
                ConversionBanner()
            }
        }

        val titleText = runBlocking { getString(RString.subscriptions_conversion_title) }
        val bodyText = runBlocking { getString(RString.subscriptions_conversion_body) }
        composeTestRule.onNodeWithText(titleText).assertExists()
        composeTestRule.onNodeWithText(bodyText).assertExists()
    }
}

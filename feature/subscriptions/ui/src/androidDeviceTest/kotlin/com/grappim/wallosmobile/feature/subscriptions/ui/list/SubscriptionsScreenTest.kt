package com.grappim.wallosmobile.feature.subscriptions.ui.list

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_empty
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.junit.Rule
import kotlin.test.Test

/**
 * Instrumented, not a host test — no `jvm()` target here for TaigaMobileNova's own
 * `runComposeUiTest`/`jvmTest` technique to attach to. See 19.1's own preamble in
 * docs/CHECKLIST.md. This spikes the wiring alone: one render, one visible node.
 */
class SubscriptionsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emptyStateShowsItsText() {
        composeTestRule.setContent {
            SubscriptionsContent(uiState = SubscriptionsUiState(), onSubscriptionClick = {})
        }

        val expectedText = runBlocking { getString(RString.subscriptions_empty) }
        composeTestRule.onNodeWithText(expectedText).assertExists()
    }
}

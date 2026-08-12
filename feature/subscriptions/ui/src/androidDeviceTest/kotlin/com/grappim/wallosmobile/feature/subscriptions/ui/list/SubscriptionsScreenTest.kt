package com.grappim.wallosmobile.feature.subscriptions.ui.list

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.grappim.wallosmobile.feature.subscriptions.domain.model.BillingCycle
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_empty
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_filter_clear
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_filter_no_match
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_retry
import com.grappim.wallosmobile.utils.ui.NativeText
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Instrumented, not a host test — no `jvm()` target here for TaigaMobileNova's own
 * `runComposeUiTest`/`jvmTest` technique to attach to. See 19.1's own preamble in
 * docs/CHECKLIST.md.
 *
 * The matrix 19.2 dates: one render per `when`-block branch (`isLoading`, `isFailed`, `isEmpty`,
 * `isNoMatch`) plus the ordinary loaded case. `isStale` has no branch of its own here — rows and
 * the banner draw together — so it is covered by `StaleBannerTest` instead, alongside
 * `ConversionBannerTest`.
 */
class SubscriptionsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loadingStateShowsProgressIndicator() {
        composeTestRule.setContent {
            SubscriptionsContent(uiState = SubscriptionsUiState(isLoading = true), onSubscriptionClick = {})
        }

        composeTestRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertExists()
    }

    @Test
    fun failedStateShowsErrorAndRetry() {
        var retried = false
        val errorMessage = NativeText.Simple("Couldn't reach that server. Check the URL and your connection.")
        composeTestRule.setContent {
            SubscriptionsContent(
                uiState = SubscriptionsUiState(error = errorMessage, onRetryClick = { retried = true }),
                onSubscriptionClick = {}
            )
        }

        composeTestRule.onNodeWithText(errorMessage.text).assertExists()

        val retryText = runBlocking { getString(RString.subscriptions_retry) }
        composeTestRule.onNodeWithText(retryText).performClick()
        assertTrue(retried)
    }

    @Test
    fun emptyStateShowsItsText() {
        composeTestRule.setContent {
            SubscriptionsContent(uiState = SubscriptionsUiState(), onSubscriptionClick = {})
        }

        val expectedText = runBlocking { getString(RString.subscriptions_empty) }
        composeTestRule.onNodeWithText(expectedText).assertExists()
    }

    @Test
    fun noMatchStateShowsClearButton() {
        var cleared = false
        composeTestRule.setContent {
            SubscriptionsContent(
                uiState = SubscriptionsUiState(
                    hasCachedRows = true,
                    filters = SubscriptionsFilterUiState(onClear = { cleared = true })
                ),
                onSubscriptionClick = {}
            )
        }

        val noMatchText = runBlocking { getString(RString.subscriptions_filter_no_match) }
        composeTestRule.onNodeWithText(noMatchText).assertExists()

        val clearText = runBlocking { getString(RString.subscriptions_filter_clear) }
        composeTestRule.onNodeWithText(clearText).performClick()
        assertTrue(cleared)
    }

    @Test
    fun loadedStateShowsRealRows() {
        val items = persistentListOf(
            SubscriptionUiItem(
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
        composeTestRule.setContent {
            SubscriptionsContent(
                uiState = SubscriptionsUiState(items = items, hasCachedRows = true),
                onSubscriptionClick = {}
            )
        }

        composeTestRule.onNodeWithText("Disney+").assertExists()
    }
}

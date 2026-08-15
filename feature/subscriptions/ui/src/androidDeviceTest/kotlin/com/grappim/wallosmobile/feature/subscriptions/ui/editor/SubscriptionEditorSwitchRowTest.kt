package com.grappim.wallosmobile.feature.subscriptions.ui.editor

import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.subscription_editor_notify
import com.grappim.wallosmobile.uikit.WallosMobilePreviewTheme
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression check for 27.2's `toggleable` change on `SwitchRow`: the label and the switch must
 * merge into one semantics node exposing `Role.Switch`'s on/off state, not two separate nodes (an
 * unfocusable label plus a small switch). Finding the row by its label text and asserting the
 * toggle state on that same node proves the merge; a click landing on the label text reaching
 * `onNotifyChange` proves the whole row is the hit target, not just the `Switch` itself.
 */
class SubscriptionEditorSwitchRowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun notifyToggleRowIsSingleMergedSemanticsNode() {
        var changedTo: Boolean? = null
        composeTestRule.setContent {
            WallosMobilePreviewTheme {
                SubscriptionEditorContent(
                    uiState = SubscriptionEditorUiState(notify = true, onNotifyChange = { changedTo = it })
                )
            }
        }

        val notifyLabel = runBlocking { getString(RString.subscription_editor_notify) }
        val notifyRow = composeTestRule.onNodeWithText(notifyLabel)
        notifyRow.assertIsOn()

        // The row sits well below the fold in this scrollable form — `performClick` dispatches a
        // real touch at the node's on-screen position, so it needs to be scrolled into view first
        // (unlike `assertIsOn` above, which reads semantics regardless of what's on screen).
        notifyRow.performScrollTo().performClick()
        assertEquals(false, changedTo)
    }
}

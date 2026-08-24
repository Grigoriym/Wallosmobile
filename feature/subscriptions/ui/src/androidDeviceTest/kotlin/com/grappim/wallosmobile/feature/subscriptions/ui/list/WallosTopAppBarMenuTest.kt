package com.grappim.wallosmobile.feature.subscriptions.ui.list

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDrawerState
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.uikit_menu_content_description
import com.grappim.wallosmobile.uikit.WallosMobilePreviewTheme
import com.grappim.wallosmobile.uikit.widgets.topappbar.NavigationIconConfig
import com.grappim.wallosmobile.uikit.widgets.topappbar.TopBarConfig
import com.grappim.wallosmobile.uikit.widgets.topappbar.WallosTopAppBar
import com.grappim.wallosmobile.utils.ui.NativeText
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.junit.Rule
import kotlin.test.Test

/**
 * Regression check for 27.1's localization of `WallosTopAppBar`'s Menu content description, on
 * the `Menu` config `SubscriptionsScreen`'s shell uses (`SubscriptionsScreen.kt`'s
 * `NavigationIconConfig.Menu`). `androidDeviceTest` is only wired for `feature:subscriptions:ui`
 * and `core:storage` (27.4) — `uikit` itself has no device-test source set — so this renders the
 * shared component directly from here rather than through the full drawer shell, which lives in
 * `composeApp` and can't be reached from this module.
 */
@OptIn(ExperimentalMaterial3Api::class)
class WallosTopAppBarMenuTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun menuIconAnnouncesLocalizedContentDescription() {
        composeTestRule.setContent {
            WallosMobilePreviewTheme {
                WallosTopAppBar(
                    isVisible = true,
                    drawerState = rememberDrawerState(DrawerValue.Closed),
                    topBarConfig = TopBarConfig(
                        title = NativeText.Simple("Subscriptions"),
                        navigationIcon = NavigationIconConfig.Menu
                    ),
                    defaultGoBack = {}
                )
            }
        }

        val menuContentDescription = runBlocking { getString(RString.uikit_menu_content_description) }
        composeTestRule.onNodeWithContentDescription(menuContentDescription).assertExists()
    }
}

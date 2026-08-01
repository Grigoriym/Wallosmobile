package com.grappim.wallosmobile.uikit.widgets.topappbar

import com.grappim.wallosmobile.utils.ui.NativeText
import kotlin.test.Test
import kotlin.test.assertEquals

class TopBarControllerTest {

    @Test
    fun `starts with an empty config`() {
        assertEquals(TopBarConfig(), TopBarController().config)
    }

    @Test
    fun `update replaces the config`() {
        val controller = TopBarController()
        val config = TopBarConfig(
            title = NativeText.Simple("Subscriptions"),
            navigationIcon = NavigationIconConfig.Menu
        )

        controller.update(config)

        assertEquals(config, controller.config)
    }

    @Test
    fun `reset restores the empty config`() {
        val controller = TopBarController()
        controller.update(TopBarConfig(title = NativeText.Simple("Subscriptions")))

        controller.reset()

        assertEquals(TopBarConfig(), controller.config)
    }
}

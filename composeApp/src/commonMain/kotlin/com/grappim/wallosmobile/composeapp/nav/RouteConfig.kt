package com.grappim.wallosmobile.composeapp.nav

import androidx.navigation3.runtime.NavKey
import com.grappim.wallosmobile.feature.settings.ui.SettingsRoute
import com.grappim.wallosmobile.feature.subscriptions.ui.detail.SubscriptionDetailRoute
import com.grappim.wallosmobile.feature.subscriptions.ui.list.SubscriptionsRoute

/**
 * How the drawer behaves on a given route.
 */
sealed interface DrawerConfig {
    /** Drawer reachable by swipe as well as by the menu button — top-level screens. */
    data object Enabled : DrawerConfig

    /** Menu button only, so a horizontal gesture belongs to the screen — detail screens. */
    data object GesturesDisabled : DrawerConfig
}

data class RouteConfig(val drawerConfig: DrawerConfig = DrawerConfig.Enabled)

/**
 * The shell's per-route settings. A route with nothing special to say is absent from here and
 * takes the defaults.
 */
object RouteConfigProvider {

    fun getConfig(route: NavKey): RouteConfig = when (route) {
        is SubscriptionsRoute -> RouteConfig(drawerConfig = DrawerConfig.Enabled)
        is SubscriptionDetailRoute -> RouteConfig(drawerConfig = DrawerConfig.GesturesDisabled)
        is SettingsRoute -> RouteConfig(drawerConfig = DrawerConfig.Enabled)
        else -> RouteConfig()
    }
}

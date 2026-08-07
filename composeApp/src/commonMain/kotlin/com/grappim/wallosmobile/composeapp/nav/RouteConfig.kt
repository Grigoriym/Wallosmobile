package com.grappim.wallosmobile.composeapp.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import com.grappim.wallosmobile.feature.settings.ui.SettingsRoute
import com.grappim.wallosmobile.feature.subscriptions.ui.detail.SubscriptionDetailRoute
import com.grappim.wallosmobile.feature.subscriptions.ui.editor.SubscriptionEditorRoute
import com.grappim.wallosmobile.feature.subscriptions.ui.list.SubscriptionsRoute
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.subscription_editor_fab_content_description
import com.grappim.wallosmobile.utils.ui.NativeText

/**
 * How the drawer behaves on a given route.
 */
sealed interface DrawerConfig {
    /** Drawer reachable by swipe as well as by the menu button — top-level screens. */
    data object Enabled : DrawerConfig

    /** Menu button only, so a horizontal gesture belongs to the screen — detail screens. */
    data object GesturesDisabled : DrawerConfig
}

/**
 * Whether a route gets a FAB (7.6) — parked as a bare `drawerConfig` field until now, since
 * nothing before Phase 3 wrote anything (1.8's note).
 */
sealed interface FabConfig {
    data object None : FabConfig

    data class Standard(val icon: ImageVector, val contentDescription: NativeText, val navigateTo: NavKey) : FabConfig
}

data class RouteConfig(val drawerConfig: DrawerConfig = DrawerConfig.Enabled, val fabConfig: FabConfig = FabConfig.None)

/**
 * The shell's per-route settings. A route with nothing special to say is absent from here and
 * takes the defaults.
 */
object RouteConfigProvider {

    fun getConfig(route: NavKey): RouteConfig = when (route) {
        is SubscriptionsRoute -> RouteConfig(
            drawerConfig = DrawerConfig.Enabled,
            fabConfig = FabConfig.Standard(
                icon = Icons.Filled.Add,
                contentDescription = NativeText.Resource(RString.subscription_editor_fab_content_description),
                navigateTo = SubscriptionEditorRoute()
            )
        )

        is SubscriptionDetailRoute -> RouteConfig(drawerConfig = DrawerConfig.GesturesDisabled)

        is SubscriptionEditorRoute -> RouteConfig(drawerConfig = DrawerConfig.GesturesDisabled)

        is SettingsRoute -> RouteConfig(drawerConfig = DrawerConfig.Enabled)

        else -> RouteConfig()
    }
}

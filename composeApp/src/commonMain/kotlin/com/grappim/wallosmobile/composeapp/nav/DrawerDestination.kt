package com.grappim.wallosmobile.composeapp.nav

import androidx.navigation3.runtime.NavKey
import kotlinx.collections.immutable.persistentSetOf

/**
 * The top-level sections, one per drawer entry. Each owns an independent sub-stack, so every
 * destination here must also appear in [DRAWER_NAV_ITEMS] — `NavigationState.currentSubStack`
 * `error()`s on a section it has no stack for.
 */
enum class DrawerDestination(val route: NavKey) {
    Subscriptions(SubscriptionsRoute),
    Settings(SettingsRoute)
}

/** The set `rememberNavigationState` builds a sub-stack for. */
val DRAWER_NAV_ITEMS = persistentSetOf<NavKey>(
    SubscriptionsRoute,
    SettingsRoute
)

/** Where the drawer shell opens. Navigating here from another section clears the section stack. */
val START_DESTINATION: NavKey = SubscriptionsRoute

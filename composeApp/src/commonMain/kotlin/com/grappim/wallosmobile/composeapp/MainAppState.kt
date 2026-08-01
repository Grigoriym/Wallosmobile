package com.grappim.wallosmobile.composeapp

import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.grappim.wallosmobile.composeapp.nav.DRAWER_NAV_ITEMS
import com.grappim.wallosmobile.composeapp.nav.DrawerConfig
import com.grappim.wallosmobile.composeapp.nav.DrawerDestination
import com.grappim.wallosmobile.composeapp.nav.RouteConfig
import com.grappim.wallosmobile.composeapp.nav.RouteConfigProvider
import com.grappim.wallosmobile.composeapp.nav.START_DESTINATION
import com.grappim.wallosmobile.composeapp.nav.navSavedStateConfiguration
import com.grappim.wallosmobile.core.navigation.NavigationState
import com.grappim.wallosmobile.core.navigation.rememberNavigationState
import kotlinx.coroutines.CoroutineScope

@Composable
fun rememberMainAppState(
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    drawerState: DrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
): MainAppState {
    val navigationState = rememberNavigationState(
        startKey = START_DESTINATION,
        topLevelKeys = DRAWER_NAV_ITEMS,
        configuration = navSavedStateConfiguration
    )

    return remember(navigationState, coroutineScope, drawerState) {
        MainAppState(
            navigationState = navigationState,
            coroutineScope = coroutineScope,
            drawerState = drawerState
        )
    }
}

/**
 * What the shell needs to know about where it is: the back stacks, the drawer, and the current
 * route's configuration.
 */
@Stable
class MainAppState(
    val navigationState: NavigationState,
    val coroutineScope: CoroutineScope,
    val drawerState: DrawerState
) {

    val currentRouteConfig: RouteConfig by derivedStateOf {
        RouteConfigProvider.getConfig(navigationState.currentKey)
    }

    val drawerGesturesEnabled: Boolean by derivedStateOf {
        currentRouteConfig.drawerConfig == DrawerConfig.Enabled
    }

    /** Which drawer item is highlighted — the active *section*, not the visible screen. */
    val currentDrawerDestination: DrawerDestination? by derivedStateOf {
        DrawerDestination.entries.find { it.route == navigationState.currentTopLevelKey }
    }
}

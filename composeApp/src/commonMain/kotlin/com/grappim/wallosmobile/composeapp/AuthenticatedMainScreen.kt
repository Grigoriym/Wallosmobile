package com.grappim.wallosmobile.composeapp

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.grappim.wallosmobile.composeapp.nav.DrawerDestination
import com.grappim.wallosmobile.composeapp.nav.DrawerItemsBuilder
import com.grappim.wallosmobile.composeapp.nav.MainNavHost
import com.grappim.wallosmobile.composeapp.widget.WallosDrawerWidget
import com.grappim.wallosmobile.core.navigation.Navigator
import com.grappim.wallosmobile.uikit.widgets.topappbar.LocalTopBarConfig
import com.grappim.wallosmobile.uikit.widgets.topappbar.NavigationIconConfig
import com.grappim.wallosmobile.uikit.widgets.topappbar.TopBarController
import com.grappim.wallosmobile.uikit.widgets.topappbar.WallosTopAppBar
import kotlinx.coroutines.launch

/**
 * The shell every screen renders inside: drawer, top app bar, and the nav display.
 *
 * [drawerItemsBuilder] is constructed rather than injected — the Koin graph is only started in
 * 1.11, and nothing here needs it before then.
 */
@Composable
fun AuthenticatedMainScreen(
    appState: MainAppState,
    modifier: Modifier = Modifier,
    drawerItemsBuilder: DrawerItemsBuilder = remember { DrawerItemsBuilder() }
) {
    val navigator = remember(appState) { Navigator(appState.navigationState) }
    val topBarController = remember { TopBarController() }
    val drawerItems = remember(drawerItemsBuilder) { drawerItemsBuilder.build() }

    CompositionLocalProvider(LocalTopBarConfig provides topBarController) {
        WallosDrawerWidget(
            modifier = modifier,
            drawerItems = drawerItems,
            currentTopLevelDestination = appState.currentDrawerDestination,
            drawerState = appState.drawerState,
            onDrawerItemClick = { destination: DrawerDestination ->
                appState.coroutineScope.launch {
                    appState.drawerState.close()
                }
                navigator.navigate(destination.route)
            },
            gesturesEnabled = appState.drawerGesturesEnabled
        ) {
            MainScaffold(
                appState = appState,
                navigator = navigator,
                topBarController = topBarController
            )
        }
    }
}

@Composable
private fun MainScaffold(
    appState: MainAppState,
    navigator: Navigator,
    topBarController: TopBarController,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.imePadding(),
        topBar = {
            WallosTopAppBar(
                isVisible = topBarController.config.navigationIcon !is NavigationIconConfig.None,
                drawerState = appState.drawerState,
                topBarConfig = topBarController.config,
                defaultGoBack = { navigator.goBack() }
            )
        }
    ) { innerPadding ->
        MainNavHost(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            navigationState = appState.navigationState,
            navigator = navigator
        )

        /**
         * Composed *after* [MainNavHost] deliberately: of several enabled back handlers the one
         * composed last wins, and composing this first leaves back navigating the stack with the
         * drawer still open. `isAnimationRunning` covers the drawer that looks open but isn't yet.
         */
        NavigationBackHandler(
            state = rememberNavigationEventState(NavigationEventInfo.None),
            isBackEnabled = appState.drawerState.isOpen || appState.drawerState.isAnimationRunning,
            onBackCompleted = {
                appState.coroutineScope.launch {
                    appState.drawerState.close()
                }
            }
        )
    }
}

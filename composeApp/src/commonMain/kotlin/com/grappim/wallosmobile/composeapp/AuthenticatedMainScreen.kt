package com.grappim.wallosmobile.composeapp

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.grappim.wallosmobile.composeapp.nav.DrawerDestination
import com.grappim.wallosmobile.composeapp.nav.DrawerItemsBuilder
import com.grappim.wallosmobile.composeapp.nav.FabConfig
import com.grappim.wallosmobile.composeapp.nav.MainNavHost
import com.grappim.wallosmobile.composeapp.widget.WallosDrawerWidget
import com.grappim.wallosmobile.core.navigation.Navigator
import com.grappim.wallosmobile.core.storage.NetworkMonitor
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.app_update_downloaded
import com.grappim.wallosmobile.strings.generated.resources.app_update_restart
import com.grappim.wallosmobile.uikit.widgets.network.LocalIsOffline
import com.grappim.wallosmobile.uikit.widgets.snackbar.LocalSnackbarHostController
import com.grappim.wallosmobile.uikit.widgets.snackbar.SnackbarHostController
import com.grappim.wallosmobile.uikit.widgets.topappbar.LocalTopBarConfig
import com.grappim.wallosmobile.uikit.widgets.topappbar.NavigationIconConfig
import com.grappim.wallosmobile.uikit.widgets.topappbar.TopBarController
import com.grappim.wallosmobile.uikit.widgets.topappbar.WallosTopAppBar
import com.grappim.wallosmobile.utils.ui.asString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * The shell every screen renders inside: drawer, top app bar, and the nav display.
 */
@Composable
fun AuthenticatedMainScreen(
    appState: MainAppState,
    modifier: Modifier = Modifier,
    drawerItemsBuilder: DrawerItemsBuilder = koinInject(),
    networkMonitor: NetworkMonitor = koinInject(),
    updateDownloaded: Flow<Unit> = emptyFlow(),
    onRestartUpdate: () -> Unit = {}
) {
    val navigator = remember(appState) { Navigator(appState.navigationState) }
    val topBarController = remember { TopBarController() }
    val snackbarHostController = remember { SnackbarHostController() }
    val drawerItems = remember(drawerItemsBuilder) { drawerItemsBuilder.build() }

    /*
     * A `StateFlow`, so this reads its current value in the *first* composition. Anything that
     * arrives a pass later would push `MainNavHost` down with it, and `rememberNavBackStack`
     * consumes its restored state only in the first pass (§5.5) — the same trap `WallosAppContent`
     * seeds `lastKnownConnected` to avoid.
     */
    val isOnline by networkMonitor.isOnline.collectAsState()

    /*
     * `AppUpdateChecker` (androidApp) can't be referenced from here (composeApp cannot depend
     * upward on androidApp), so `MainActivity` narrows it down to this one signal before it ever
     * crosses the module boundary — the same shape `WallosAppContent.onDarkThemeChange` already
     * uses for its own androidApp-bound callback.
     */
    val updateDownloadedMessage = stringResource(RString.app_update_downloaded)
    val restartLabel = stringResource(RString.app_update_restart)
    LaunchedEffect(updateDownloaded) {
        updateDownloaded.collectLatest {
            val result = snackbarHostController.show(
                message = updateDownloadedMessage,
                actionLabel = restartLabel,
                duration = SnackbarDuration.Indefinite
            )
            if (result == SnackbarResult.ActionPerformed) {
                onRestartUpdate()
            }
        }
    }

    CompositionLocalProvider(
        LocalTopBarConfig provides topBarController,
        LocalIsOffline provides !isOnline,
        LocalSnackbarHostController provides snackbarHostController
    ) {
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
                topBarController = topBarController,
                snackbarHostController = snackbarHostController
            )
        }
    }
}

@Composable
private fun MainScaffold(
    appState: MainAppState,
    navigator: Navigator,
    topBarController: TopBarController,
    snackbarHostController: SnackbarHostController,
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
        },
        snackbarHost = { SnackbarHost(snackbarHostController.hostState) },
        floatingActionButton = {
            // Navigating to the editor is not itself a write — the offline gate belongs on that
            // screen's Save button, not here, the same reasoning `SubscriptionsScreen`'s Filter
            // action never needed one.
            when (val fabConfig = appState.currentRouteConfig.fabConfig) {
                is FabConfig.Standard -> FloatingActionButton(onClick = { navigator.navigate(fabConfig.navigateTo) }) {
                    Icon(imageVector = fabConfig.icon, contentDescription = fabConfig.contentDescription.asString())
                }

                FabConfig.None -> Unit
            }
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

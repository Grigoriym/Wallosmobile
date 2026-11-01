package com.grappim.wallosmobile.composeapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.grappim.kit.storage.NetworkMonitor
import com.grappim.kit.uikit.widgets.topbar.LocalTopBarConfig
import com.grappim.kit.uikit.widgets.topbar.TopBar
import com.grappim.kit.uikit.widgets.topbar.TopBarController
import com.grappim.wallosmobile.feature.settings.ui.customheaders.CustomHeadersScreen
import com.grappim.wallosmobile.feature.setup.ui.LoginScreen
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.uikit_back_content_description
import com.grappim.wallosmobile.strings.generated.resources.uikit_menu_content_description
import com.grappim.wallosmobile.uikit.widgets.network.LocalIsOffline
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * The disconnected side of the startup branch. The custom-headers screen opened from login is the
 * same one Settings reaches (plan §4.5b), but there is no shell or back stack here — login is not
 * a route — so which of the two is showing is state, and this provides the two composition locals
 * that screen reads from the shell.
 */
@Composable
internal fun LoginBranch() {
    var isCustomHeadersOpen by rememberSaveable { mutableStateOf(false) }

    if (isCustomHeadersOpen) {
        LoginCustomHeaders(onBackClick = { isCustomHeadersOpen = false })
    } else {
        LoginScreen(onCustomHeadersClick = { isCustomHeadersOpen = true })
    }
}

@Composable
private fun LoginCustomHeaders(onBackClick: () -> Unit, networkMonitor: NetworkMonitor = koinInject()) {
    val topBarController = remember { TopBarController() }
    val isOnline by networkMonitor.isOnline.collectAsState()

    CompositionLocalProvider(
        LocalTopBarConfig provides topBarController,
        LocalIsOffline provides !isOnline
    ) {
        Scaffold(
            modifier = Modifier.imePadding(),
            topBar = {
                TopBar(
                    isVisible = true,
                    drawerState = rememberDrawerState(DrawerValue.Closed),
                    topBarConfig = topBarController.config,
                    defaultGoBack = onBackClick,
                    backContentDescription = stringResource(RString.uikit_back_content_description),
                    menuContentDescription = stringResource(RString.uikit_menu_content_description)
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                CustomHeadersScreen(onBackClick = onBackClick)
            }
        }

        NavigationBackHandler(
            state = rememberNavigationEventState(NavigationEventInfo.None),
            onBackCompleted = onBackClick
        )
    }
}

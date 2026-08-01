package com.grappim.wallosmobile.composeapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.grappim.wallosmobile.core.storage.ApiKeyStorage
import com.grappim.wallosmobile.feature.setup.ui.LoginScreen
import com.grappim.wallosmobile.uikit.WallosMobileTheme
import org.koin.compose.koinInject

/**
 * The app's single composable entry point, so `MainActivity` knows about no other module.
 *
 * The startup branch (plan §7.1) is *state*, not navigation: [ApiKeyStorage.isConnected] is the
 * one source of truth, so logging in and disconnecting both swap the whole tree without either
 * side telling the other. That is why login is not a `NavKey` — it never enters a back stack, and
 * `NavKeySerializers` has nothing to register for it.
 */
@Composable
fun WallosAppContent(apiKeyStorage: ApiKeyStorage = koinInject()) {
    /*
     * Seeding the branch from saved instance state is load-bearing, not an optimisation.
     * `isConnected` is a DataStore flow with no value for the first frame, so waiting for it
     * pushes [AuthenticatedMainScreen] into a *later* composition pass — and `rememberNavBackStack`
     * only consumes its restored state in the first one. Without this the app survives process
     * death but comes back to the start destination with the back stack silently gone.
     */
    var lastKnownConnected by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val isConnected by apiKeyStorage.isConnected.collectAsState(initial = lastKnownConnected)

    LaunchedEffect(isConnected) {
        lastKnownConnected = isConnected
    }

    WallosMobileTheme {
        when (isConnected) {
            // Only on a genuinely first launch: nothing saved, and DataStore has not answered.
            // Blank rather than a spinner — it lasts a frame, and a flash of progress reads as
            // a stutter.
            null -> Box(modifier = Modifier.fillMaxSize())

            true -> AuthenticatedMainScreen(appState = rememberMainAppState())

            false -> LoginScreen()
        }
    }
}

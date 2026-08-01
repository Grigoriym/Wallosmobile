package com.grappim.wallosmobile.composeapp

import androidx.compose.runtime.Composable
import com.grappim.wallosmobile.uikit.WallosMobileTheme

/**
 * The app's single composable entry point, so `MainActivity` knows about no other module.
 * 1.11 puts the startup branch — stored key → shell, else login — inside the theme here.
 */
@Composable
fun WallosAppContent() {
    WallosMobileTheme {
        AuthenticatedMainScreen(appState = rememberMainAppState())
    }
}

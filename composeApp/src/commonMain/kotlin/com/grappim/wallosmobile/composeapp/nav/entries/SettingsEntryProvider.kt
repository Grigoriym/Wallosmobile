package com.grappim.wallosmobile.composeapp.nav.entries

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.grappim.wallosmobile.core.navigation.Navigator
import com.grappim.wallosmobile.feature.profile.ui.ProfileRoute
import com.grappim.wallosmobile.feature.profile.ui.ProfileScreen
import com.grappim.wallosmobile.feature.settings.ui.SettingsRoute
import com.grappim.wallosmobile.feature.settings.ui.SettingsScreen
import com.grappim.wallosmobile.feature.settings.ui.about.AboutRoute
import com.grappim.wallosmobile.feature.settings.ui.about.AboutScreen
import com.grappim.wallosmobile.feature.settings.ui.appearance.InterfaceRoute
import com.grappim.wallosmobile.feature.settings.ui.appearance.InterfaceScreen
import com.grappim.wallosmobile.feature.settings.ui.startdestination.StartDestinationRoute
import com.grappim.wallosmobile.feature.settings.ui.startdestination.StartDestinationScreen
import com.grappim.wallosmobile.feature.settings.ui.trustedcerts.TrustedCertsRoute
import com.grappim.wallosmobile.feature.settings.ui.trustedcerts.TrustedCertsScreen

/**
 * Disconnect still navigates nowhere — clearing the key flips `ApiKeyStorage.isConnected` and the
 * startup branch swaps the shell for login (plan §7.1). The [Navigator] is here for the sub-screens
 * that hang off the settings root: Interface (4.3), About (4.4), Profile (9.9), the start
 * destination picker (12.2) and the trusted-certificates list (18.2).
 */
fun EntryProviderScope<NavKey>.settingsEntry(navigator: Navigator) {
    entry<SettingsRoute> {
        SettingsScreen(
            onInterfaceClick = { navigator.navigate(InterfaceRoute) },
            onStartDestinationClick = { navigator.navigate(StartDestinationRoute) },
            onAboutClick = { navigator.navigate(AboutRoute) },
            onProfileClick = { navigator.navigate(ProfileRoute) },
            onTrustedCertsClick = { navigator.navigate(TrustedCertsRoute) }
        )
    }
    entry<InterfaceRoute> {
        InterfaceScreen(onBackClick = { navigator.goBack() })
    }
    entry<StartDestinationRoute> {
        StartDestinationScreen(onBackClick = { navigator.goBack() })
    }
    entry<AboutRoute> {
        AboutScreen(onBackClick = { navigator.goBack() })
    }
    entry<ProfileRoute> {
        ProfileScreen(onBackClick = { navigator.goBack() })
    }
    entry<TrustedCertsRoute> {
        TrustedCertsScreen(onBackClick = { navigator.goBack() })
    }
}

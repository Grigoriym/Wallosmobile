package com.grappim.wallosmobile.composeapp.nav.entries

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.grappim.wallosmobile.core.navigation.Navigator
import com.grappim.wallosmobile.feature.settings.ui.SettingsRoute
import com.grappim.wallosmobile.feature.settings.ui.SettingsScreen
import com.grappim.wallosmobile.feature.settings.ui.about.AboutRoute
import com.grappim.wallosmobile.feature.settings.ui.about.AboutScreen
import com.grappim.wallosmobile.feature.settings.ui.appearance.InterfaceRoute
import com.grappim.wallosmobile.feature.settings.ui.appearance.InterfaceScreen

/**
 * Disconnect still navigates nowhere — clearing the key flips `ApiKeyStorage.isConnected` and the
 * startup branch swaps the shell for login (plan §7.1). The [Navigator] is here for the sub-screens
 * that hang off the settings root: Interface (4.3) and About (4.4).
 */
fun EntryProviderScope<NavKey>.settingsEntry(navigator: Navigator) {
    entry<SettingsRoute> {
        SettingsScreen(
            onInterfaceClick = { navigator.navigate(InterfaceRoute) },
            onAboutClick = { navigator.navigate(AboutRoute) }
        )
    }
    entry<InterfaceRoute> {
        InterfaceScreen(onBackClick = { navigator.goBack() })
    }
    entry<AboutRoute> {
        AboutScreen(onBackClick = { navigator.goBack() })
    }
}

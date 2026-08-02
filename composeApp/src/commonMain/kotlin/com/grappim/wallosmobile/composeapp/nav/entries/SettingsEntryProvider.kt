package com.grappim.wallosmobile.composeapp.nav.entries

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.grappim.wallosmobile.feature.settings.ui.SettingsRoute
import com.grappim.wallosmobile.feature.settings.ui.SettingsScreen

/**
 * Settings takes no [com.grappim.wallosmobile.core.navigation.Navigator]: Disconnect navigates
 * nowhere. Clearing the key flips `ApiKeyStorage.isConnected` and the startup branch swaps the
 * shell for login (plan §7.1), so there is no back stack move to make.
 */
fun EntryProviderScope<NavKey>.settingsEntry() {
    entry<SettingsRoute> { SettingsScreen() }
}

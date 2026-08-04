package com.grappim.wallosmobile.core.storage.theme

import kotlinx.coroutines.flow.Flow

/**
 * The user's palette choice. Device state, not account state: unlike the cached subscriptions it
 * belongs to the install rather than to the stored key, so `ApiKeyStorage.clear()` leaves it alone
 * and disconnecting doesn't hand the next login a light-mode app.
 */
interface ThemeStorage {

    /** Emits [ThemeMode.default] until a stored value arrives, and again on every change. */
    val themeMode: Flow<ThemeMode>

    suspend fun setThemeMode(mode: ThemeMode)
}

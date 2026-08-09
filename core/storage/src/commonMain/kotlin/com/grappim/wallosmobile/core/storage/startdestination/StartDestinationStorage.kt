package com.grappim.wallosmobile.core.storage.startdestination

import kotlinx.coroutines.flow.Flow

/**
 * The drawer section the app opens on. Device state, not account state: like `ThemeStorage` it
 * belongs to the install rather than to the stored key, so `ApiKeyStorage.clear()` leaves it alone.
 */
interface StartDestinationStorage {

    /** Emits [StartDestination.default] until a stored value arrives, and again on every change. */
    val startDestination: Flow<StartDestination>

    suspend fun setStartDestination(value: StartDestination)
}

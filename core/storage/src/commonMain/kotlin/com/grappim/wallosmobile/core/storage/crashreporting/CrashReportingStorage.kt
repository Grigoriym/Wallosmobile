package com.grappim.wallosmobile.core.storage.crashreporting

import kotlinx.coroutines.flow.Flow

/**
 * The user's crash-reporting consent. Device state, not account state, same reasoning as
 * `ThemeStorage`: it belongs to the install, so `ApiKeyStorage.clear()` leaves it alone.
 */
interface CrashReportingStorage {

    /** Defaults to `false` (opt-in) until a stored value arrives, and again on every change. */
    val crashReportingEnabled: Flow<Boolean>

    suspend fun setCrashReportingEnabled(enabled: Boolean)
}

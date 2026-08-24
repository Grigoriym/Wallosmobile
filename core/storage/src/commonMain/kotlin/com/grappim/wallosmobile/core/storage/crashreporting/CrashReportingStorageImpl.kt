package com.grappim.wallosmobile.core.storage.crashreporting

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

/**
 * Shares the module's one DataStore file with the key, the URL, the theme mode and the pinned
 * certificates — every write to any of them re-emits the whole `Preferences`, hence the
 * `distinctUntilChanged`.
 */
@Single(binds = [CrashReportingStorage::class])
internal class CrashReportingStorageImpl(private val dataStore: DataStore<Preferences>) : CrashReportingStorage {

    override val crashReportingEnabled: Flow<Boolean> = dataStore.data
        .map { prefs -> prefs[KEY_CRASH_REPORTING_ENABLED] ?: false }
        .distinctUntilChanged()

    override suspend fun setCrashReportingEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_CRASH_REPORTING_ENABLED] = enabled
        }
    }

    private companion object {
        private val KEY_CRASH_REPORTING_ENABLED = booleanPreferencesKey("crash_reporting_enabled")
    }
}

package com.grappim.wallosmobile.core.storage.theme

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

/**
 * Shares the module's one DataStore file with the key, the URL and the pinned certificates —
 * every write to any of them re-emits the whole `Preferences`, hence the `distinctUntilChanged`.
 *
 * A value this doesn't recognise (a hand-edited file, or a mode a future version dropped) reads as
 * the default rather than throwing: a preference is not worth a crash.
 */
@Single(binds = [ThemeStorage::class])
internal class ThemeStorageImpl(private val dataStore: DataStore<Preferences>) : ThemeStorage {

    override val themeMode: Flow<ThemeMode> = dataStore.data
        .map { prefs -> ThemeMode.fromValue(prefs[KEY_THEME_MODE]) ?: ThemeMode.default() }
        .distinctUntilChanged()

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.value
        }
    }

    private companion object {
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    }
}

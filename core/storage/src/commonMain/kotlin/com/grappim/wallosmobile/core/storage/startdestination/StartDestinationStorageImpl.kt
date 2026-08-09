package com.grappim.wallosmobile.core.storage.startdestination

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
 * A value this doesn't recognise (a hand-edited file, or a section a future version dropped) reads
 * as the default rather than throwing: a preference is not worth a crash.
 */
@Single(binds = [StartDestinationStorage::class])
internal class StartDestinationStorageImpl(private val dataStore: DataStore<Preferences>) : StartDestinationStorage {

    override val startDestination: Flow<StartDestination> = dataStore.data
        .map { prefs -> StartDestination.fromValue(prefs[KEY_START_DESTINATION]) ?: StartDestination.default() }
        .distinctUntilChanged()

    override suspend fun setStartDestination(value: StartDestination) {
        dataStore.edit { prefs ->
            prefs[KEY_START_DESTINATION] = value.value
        }
    }

    private companion object {
        private val KEY_START_DESTINATION = stringPreferencesKey("start_destination")
    }
}

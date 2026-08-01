package com.grappim.wallosmobile.core.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.core.annotation.Single
import kotlin.concurrent.Volatile

@Single(binds = [ServerUrlStorage::class])
internal class ServerUrlStorageImpl(private val dataStore: DataStore<Preferences>) : ServerUrlStorage {

    @Volatile
    private var cached: String? = null

    /**
     * Blocks on the *first* read only, then serves the cache: the caller is Ktor's
     * `defaultRequest` block, which cannot suspend. Two threads racing here both read the same
     * value, so the write is idempotent.
     */
    override val serverUrl: String
        get() = cached ?: runBlocking { read() }.also { cached = it }

    override suspend fun saveServerUrl(url: String) {
        dataStore.edit { prefs ->
            prefs[KEY_SERVER_URL] = url
        }
        cached = url
    }

    private suspend fun read(): String = dataStore.data.first()[KEY_SERVER_URL].orEmpty()

    private companion object {
        private val KEY_SERVER_URL = stringPreferencesKey("server_url")
    }
}

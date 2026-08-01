package com.grappim.wallosmobile.core.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single(binds = [ApiKeyStorage::class])
internal class ApiKeyStorageImpl(
    private val dataStore: DataStore<Preferences>,
    private val secretCipher: SecretCipher
) : ApiKeyStorage {

    private val storedKey: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_API_KEY]?.let(secretCipher::decrypt)
    }

    override val isConnected: Flow<Boolean> = storedKey
        .map { !it.isNullOrBlank() }
        .distinctUntilChanged()

    override suspend fun getKey(): String? = storedKey.first()

    override suspend fun setKey(key: String) {
        val encrypted = secretCipher.encrypt(key)
        dataStore.edit { prefs ->
            prefs[KEY_API_KEY] = encrypted
        }
    }

    override suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_API_KEY)
        }
    }

    private companion object {
        private val KEY_API_KEY = stringPreferencesKey("api_key")
    }
}

package com.grappim.wallosmobile.core.storage.cert

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Single

/**
 * The certificates the user has accepted, pinned to the host they were accepted *for*.
 *
 * The pin is `(host, fingerprint)` rather than the fingerprint alone: a certificate trusted for
 * one server must not silently authenticate a different one that presents the same bytes.
 */
interface TrustedCertStorage {
    suspend fun isTrusted(host: String, sha256Fingerprint: String): Boolean
    suspend fun trust(host: String, sha256Fingerprint: String)
}

/**
 * Shares the module's one DataStore file, like every other storage class here. That also decides
 * what disconnect does to a pin: `ApiKeyStorage.clear()` removes its own key, so trust survives
 * it — the pin is a statement about a *server's* certificate, not about the account whose
 * credential was just thrown away.
 */
@Single(binds = [TrustedCertStorage::class])
internal class TrustedCertStorageImpl(private val dataStore: DataStore<Preferences>) : TrustedCertStorage {

    override suspend fun isTrusted(host: String, sha256Fingerprint: String): Boolean =
        entry(host, sha256Fingerprint) in stored()

    override suspend fun trust(host: String, sha256Fingerprint: String) {
        dataStore.edit { prefs ->
            prefs[KEY_TRUSTED_CERTS] = prefs[KEY_TRUSTED_CERTS].orEmpty() + entry(host, sha256Fingerprint)
        }
    }

    private suspend fun stored(): Set<String> = dataStore.data.first()[KEY_TRUSTED_CERTS].orEmpty()

    private fun entry(host: String, sha256Fingerprint: String) = "$host$SEPARATOR$sha256Fingerprint"

    private companion object {
        private val KEY_TRUSTED_CERTS = stringSetPreferencesKey("trusted_certs")

        // Neither a host nor a hex fingerprint can contain it, so the two can't be confused.
        private const val SEPARATOR = "|"
    }
}

package com.grappim.wallosmobile.core.storage.cert

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.grappim.wallosmobile.core.domain.PendingCertTrust
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

/**
 * The certificates the user has accepted, pinned to the host they were accepted *for*.
 *
 * The pin is `(host, fingerprint)` rather than the fingerprint alone: a certificate trusted for
 * one server must not silently authenticate a different one that presents the same bytes.
 */
interface TrustedCertStorage {
    suspend fun isTrusted(host: String, sha256Fingerprint: String): Boolean
    suspend fun trust(pendingCertTrust: PendingCertTrust)
    fun getAllFlow(): Flow<List<PendingCertTrust>>
    suspend fun untrust(host: String, sha256Fingerprint: String)
}

/**
 * Shares the module's one DataStore file, like every other storage class here. That also decides
 * what disconnect does to a pin: `ApiKeyStorage.clear()` removes its own key, so trust survives
 * it — the pin is a statement about a *server's* certificate, not about the account whose
 * credential was just thrown away.
 *
 * Stores the full `PendingCertTrust` JSON-encoded, not just `host|fingerprint`: a Settings screen
 * (18.2) needs the subject/issuer/validity fields to show what it's revoking, not only the pin
 * key. Same shape as `TaigaMobileNova`'s own `TrustedCertStorageImpl`.
 */
@Single(binds = [TrustedCertStorage::class])
internal class TrustedCertStorageImpl(private val dataStore: DataStore<Preferences>) : TrustedCertStorage {

    private val trustedEntriesFlow: Flow<List<PendingCertTrust>> =
        dataStore.data.map { prefs -> decodeEntries(prefs[KEY_TRUSTED_CERTS]) }

    override suspend fun isTrusted(host: String, sha256Fingerprint: String): Boolean =
        trustedEntriesFlow.first().any { it.matches(host, sha256Fingerprint) }

    override suspend fun trust(pendingCertTrust: PendingCertTrust) {
        dataStore.edit { prefs ->
            val entries = decodeEntries(prefs[KEY_TRUSTED_CERTS])
                .filterNot { it.matches(pendingCertTrust.host, pendingCertTrust.sha256Fingerprint) }
            prefs[KEY_TRUSTED_CERTS] = json.encodeToString(entries + pendingCertTrust)
        }
    }

    override fun getAllFlow(): Flow<List<PendingCertTrust>> = trustedEntriesFlow

    override suspend fun untrust(host: String, sha256Fingerprint: String) {
        dataStore.edit { prefs ->
            val entries = decodeEntries(prefs[KEY_TRUSTED_CERTS]).filterNot { it.matches(host, sha256Fingerprint) }
            prefs[KEY_TRUSTED_CERTS] = json.encodeToString(entries)
        }
    }

    private fun decodeEntries(value: String?): List<PendingCertTrust> =
        value?.takeIf { it.isNotBlank() }?.let { json.decodeFromString(it) } ?: emptyList()

    private fun PendingCertTrust.matches(host: String, sha256Fingerprint: String) =
        this.host == host && this.sha256Fingerprint == sha256Fingerprint

    private companion object {
        private val KEY_TRUSTED_CERTS = stringPreferencesKey("trusted_certs")
        private val json = Json { ignoreUnknownKeys = true }
    }
}

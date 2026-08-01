package com.grappim.wallosmobile.core.storage

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ApiKeyStorageImplTest {

    private val dataStore = FakePreferencesDataStore()
    private val cipher = FakeSecretCipher()
    private val storage = ApiKeyStorageImpl(dataStore, cipher)

    @Test
    fun `setKey then getKey returns the key`() = runTest {
        storage.setKey("abc123")

        assertEquals("abc123", storage.getKey())
    }

    @Test
    fun `getKey is null when nothing was ever stored`() = runTest {
        assertNull(storage.getKey())
    }

    @Test
    fun `the key is never written in plaintext`() = runTest {
        storage.setKey("abc123")

        val stored = dataStore.current[stringPreferencesKey("api_key")]
        assertEquals(FakeSecretCipher.PREFIX + "abc123", stored)
    }

    @Test
    fun `an undecryptable key reads as absent`() = runTest {
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey("api_key")] = "not-produced-by-this-cipher"
        }

        assertNull(storage.getKey())
        storage.isConnected.test {
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `isConnected follows the stored key`() = runTest {
        storage.isConnected.test {
            assertFalse(awaitItem())

            storage.setKey("abc123")
            assertTrue(awaitItem())

            storage.clear()
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `isConnected is false for a blank key`() = runTest {
        storage.setKey("")

        storage.isConnected.test {
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `clear removes the key but keeps the server url`() = runTest {
        val serverUrlStorage = ServerUrlStorageImpl(dataStore)
        serverUrlStorage.saveServerUrl("https://wallos.example.com/")
        storage.setKey("abc123")

        storage.clear()

        assertNull(storage.getKey())
        assertEquals("https://wallos.example.com/", serverUrlStorage.serverUrl)
    }
}

/**
 * Reversible stand-in for the Keystore: `encrypt` is observable in the stored value, and anything
 * it did not produce fails to decrypt — the restored-backup case.
 */
private class FakeSecretCipher : SecretCipher {

    override fun encrypt(value: String): String = PREFIX + value

    override fun decrypt(value: String): String? = value.removePrefix(PREFIX).takeIf { value.startsWith(PREFIX) }

    companion object {
        const val PREFIX = "enc:"
    }
}

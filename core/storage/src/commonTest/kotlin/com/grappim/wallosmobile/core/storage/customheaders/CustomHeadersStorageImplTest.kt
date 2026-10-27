package com.grappim.wallosmobile.core.storage.customheaders

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import app.cash.turbine.test
import com.grappim.wallosmobile.core.storage.FakePreferencesDataStore
import com.grappim.wallosmobile.core.storage.FakeSecretCipher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CustomHeadersStorageImplTest {

    private val dataStore = FakePreferencesDataStore()
    private val storage = CustomHeadersStorageImpl(dataStore, FakeSecretCipher())

    private val headers = listOf(
        CustomHeader("Authorization", "Basic cGFuZ29saW46c2VjcmV0"),
        CustomHeader("CF-Access-Client-Id", "abc.access")
    )

    @Test
    fun `nothing saved reads as an empty list`() = runTest {
        assertTrue(storage.currentHeaders.isEmpty())
        storage.headers.test {
            assertEquals(emptyList(), awaitItem())
        }
    }

    @Test
    fun `setHeaders round-trips through both reads`() = runTest {
        storage.setHeaders(headers)

        assertEquals(headers, storage.currentHeaders)
        storage.headers.test {
            assertEquals(headers, awaitItem())
        }
    }

    @Test
    fun `the list is only ever written through the cipher`() = runTest {
        storage.setHeaders(headers)

        val stored = dataStore.current[KEY].orEmpty()
        assertTrue(stored.startsWith(FakeSecretCipher.PREFIX))
        assertEquals(headers, CustomHeadersStorageImpl(dataStore, FakeSecretCipher()).currentHeaders)
    }

    @Test
    fun `a list persisted before construction is read without suspending`() = runTest {
        storage.setHeaders(headers)
        val persisted = mutablePreferencesOf(KEY to dataStore.current[KEY].orEmpty())
        val reopened = CustomHeadersStorageImpl(FakePreferencesDataStore(persisted), FakeSecretCipher())

        assertEquals(headers, reopened.currentHeaders)
    }

    /** The cache is what the per-request read serves; a store change behind it isn't re-read. */
    @Test
    fun `currentHeaders serves the cache after the first read`() = runTest {
        storage.setHeaders(headers)
        assertEquals(headers, storage.currentHeaders)

        dataStore.edit { prefs -> prefs.remove(KEY) }

        assertEquals(headers, storage.currentHeaders)
    }

    @Test
    fun `saving an empty list removes the entry`() = runTest {
        storage.setHeaders(headers)

        storage.setHeaders(emptyList())

        assertNull(dataStore.current[KEY])
        assertTrue(storage.currentHeaders.isEmpty())
    }

    @Test
    fun `an undecryptable blob reads as no headers`() = runTest {
        dataStore.edit { prefs -> prefs[KEY] = "not-produced-by-this-cipher" }

        assertTrue(storage.currentHeaders.isEmpty())
    }

    @Test
    fun `a blob that decrypts but does not decode reads as no headers`() = runTest {
        dataStore.edit { prefs -> prefs[KEY] = FakeSecretCipher.PREFIX + "{not json" }

        assertTrue(storage.currentHeaders.isEmpty())
    }

    @Test
    fun `headers re-emits on change`() = runTest {
        storage.headers.test {
            assertEquals(emptyList(), awaitItem())

            storage.setHeaders(headers)
            assertEquals(headers, awaitItem())

            storage.setHeaders(emptyList())
            assertEquals(emptyList(), awaitItem())
        }
    }

    private companion object {
        val KEY = stringPreferencesKey("custom_headers")
    }
}

package com.grappim.wallosmobile.core.storage

import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ServerUrlStorageImplTest {

    @Test
    fun `serverUrl is empty when nothing was saved`() {
        val storage = ServerUrlStorageImpl(FakePreferencesDataStore())

        assertEquals("", storage.serverUrl)
    }

    @Test
    fun `saveServerUrl is visible to the non-suspending read`() = runTest {
        val storage = ServerUrlStorageImpl(FakePreferencesDataStore())

        storage.saveServerUrl("https://wallos.example.com/")

        assertEquals("https://wallos.example.com/", storage.serverUrl)
    }

    @Test
    fun `a url persisted before construction is read without suspending`() {
        val stored = mutablePreferencesOf(
            stringPreferencesKey("server_url") to "https://host/wallos/"
        )
        val storage = ServerUrlStorageImpl(FakePreferencesDataStore(stored))

        // No coroutine in sight: this is exactly how Ktor's `defaultRequest` block calls it.
        assertEquals("https://host/wallos/", storage.serverUrl)
    }

    @Test
    fun `saving twice serves the newest value`() = runTest {
        val storage = ServerUrlStorageImpl(FakePreferencesDataStore())

        storage.saveServerUrl("https://first.example.com/")
        storage.saveServerUrl("https://second.example.com/")

        assertEquals("https://second.example.com/", storage.serverUrl)
    }
}

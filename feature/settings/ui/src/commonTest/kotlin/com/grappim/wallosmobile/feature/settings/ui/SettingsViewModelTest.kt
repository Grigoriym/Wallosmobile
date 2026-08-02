package com.grappim.wallosmobile.feature.settings.ui

import com.grappim.wallosmobile.core.storage.ApiKeyStorage
import com.grappim.wallosmobile.testing.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettingsViewModelTest {

    private val apiKeyStorage = FakeApiKeyStorage()
    private val mainDispatcherRule = MainDispatcherRule()

    @BeforeTest
    fun setup() {
        mainDispatcherRule.setup()
    }

    @AfterTest
    fun tearDown() {
        mainDispatcherRule.tearDown()
    }

    private fun viewModel() = SettingsViewModel(apiKeyStorage = apiKeyStorage)

    @Test
    fun `disconnect clears the stored key`() = runTest {
        viewModel().uiState.value.onDisconnectClick()

        assertEquals(1, apiKeyStorage.clearCount)
    }

    @Test
    fun `disconnect touches nothing but the key`() = runTest {
        viewModel().uiState.value.onDisconnectClick()

        // The server URL survives a disconnect (plan §4.7), so re-connecting is one field. Nothing
        // else on this seam may be called — `setKey` in particular would store a blank key, which
        // reads as connected and fails every call.
        assertTrue(apiKeyStorage.storedKeys.isEmpty())
    }

    @Test
    fun `a failed write is logged, not thrown`() = runTest {
        apiKeyStorage.failOnClear = true

        viewModel().uiState.value.onDisconnectClick()

        assertEquals(1, apiKeyStorage.clearCount)
    }

    // Private to this file, as in 1.10 and 2.3: `:testing` is on every module's test classpath,
    // and this fake has exactly one consumer.
    private class FakeApiKeyStorage : ApiKeyStorage {

        var clearCount = 0
        var failOnClear = false
        val storedKeys = mutableListOf<String>()

        override val isConnected: Flow<Boolean> = flowOf(true)

        override suspend fun getKey(): String? = "key"

        override suspend fun setKey(key: String) {
            storedKeys += key
        }

        override suspend fun clear() {
            clearCount++
            if (failOnClear) error("the store is unwritable")
        }
    }
}

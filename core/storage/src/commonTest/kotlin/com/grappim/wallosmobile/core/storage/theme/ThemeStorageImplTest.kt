package com.grappim.wallosmobile.core.storage.theme

import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import app.cash.turbine.test
import com.grappim.wallosmobile.core.storage.FakePreferencesDataStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ThemeStorageImplTest {

    @Test
    fun `the mode is System when nothing was ever stored`() = runTest {
        val storage = ThemeStorageImpl(FakePreferencesDataStore())

        storage.themeMode.test {
            assertEquals(ThemeMode.System, awaitItem())
        }
    }

    @Test
    fun `setThemeMode is observed by the flow`() = runTest {
        val storage = ThemeStorageImpl(FakePreferencesDataStore())

        storage.themeMode.test {
            assertEquals(ThemeMode.System, awaitItem())

            storage.setThemeMode(ThemeMode.Dark)
            assertEquals(ThemeMode.Dark, awaitItem())

            storage.setThemeMode(ThemeMode.Light)
            assertEquals(ThemeMode.Light, awaitItem())
        }
    }

    @Test
    fun `a mode stored before construction is the first value emitted`() = runTest {
        val stored = mutablePreferencesOf(stringPreferencesKey("theme_mode") to "dark")

        ThemeStorageImpl(FakePreferencesDataStore(stored)).themeMode.test {
            assertEquals(ThemeMode.Dark, awaitItem())
        }
    }

    /** A hand-edited file, or a mode a future version stopped writing. */
    @Test
    fun `an unrecognised stored value reads as the default`() = runTest {
        val stored = mutablePreferencesOf(stringPreferencesKey("theme_mode") to "sepia")

        ThemeStorageImpl(FakePreferencesDataStore(stored)).themeMode.test {
            assertEquals(ThemeMode.default(), awaitItem())
        }
    }

    /** Every write to the shared file re-emits the whole `Preferences` (the key, the URL, a pin). */
    @Test
    fun `writing the same mode twice emits once`() = runTest {
        val dataStore = FakePreferencesDataStore()
        val storage = ThemeStorageImpl(dataStore)

        storage.themeMode.test {
            assertEquals(ThemeMode.System, awaitItem())

            storage.setThemeMode(ThemeMode.Dark)
            assertEquals(ThemeMode.Dark, awaitItem())

            storage.setThemeMode(ThemeMode.Dark)
            expectNoEvents()
        }
    }
}

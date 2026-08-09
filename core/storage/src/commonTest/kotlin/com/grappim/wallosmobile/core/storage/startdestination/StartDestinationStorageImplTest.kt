package com.grappim.wallosmobile.core.storage.startdestination

import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import app.cash.turbine.test
import com.grappim.wallosmobile.core.storage.FakePreferencesDataStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class StartDestinationStorageImplTest {

    @Test
    fun `the destination is Dashboard when nothing was ever stored`() = runTest {
        val storage = StartDestinationStorageImpl(FakePreferencesDataStore())

        storage.startDestination.test {
            assertEquals(StartDestination.Dashboard, awaitItem())
        }
    }

    @Test
    fun `setStartDestination is observed by the flow`() = runTest {
        val storage = StartDestinationStorageImpl(FakePreferencesDataStore())

        storage.startDestination.test {
            assertEquals(StartDestination.Dashboard, awaitItem())

            storage.setStartDestination(StartDestination.Subscriptions)
            assertEquals(StartDestination.Subscriptions, awaitItem())

            storage.setStartDestination(StartDestination.Currencies)
            assertEquals(StartDestination.Currencies, awaitItem())
        }
    }

    @Test
    fun `a destination stored before construction is the first value emitted`() = runTest {
        val stored = mutablePreferencesOf(stringPreferencesKey("start_destination") to "household")

        StartDestinationStorageImpl(FakePreferencesDataStore(stored)).startDestination.test {
            assertEquals(StartDestination.Household, awaitItem())
        }
    }

    /** A hand-edited file, or a section a future version stopped writing. */
    @Test
    fun `an unrecognised stored value reads as the default`() = runTest {
        val stored = mutablePreferencesOf(stringPreferencesKey("start_destination") to "profile")

        StartDestinationStorageImpl(FakePreferencesDataStore(stored)).startDestination.test {
            assertEquals(StartDestination.default(), awaitItem())
        }
    }

    /** Every write to the shared file re-emits the whole `Preferences` (the key, the URL, a pin). */
    @Test
    fun `writing the same destination twice emits once`() = runTest {
        val dataStore = FakePreferencesDataStore()
        val storage = StartDestinationStorageImpl(dataStore)

        storage.startDestination.test {
            assertEquals(StartDestination.Dashboard, awaitItem())

            storage.setStartDestination(StartDestination.Settings)
            assertEquals(StartDestination.Settings, awaitItem())

            storage.setStartDestination(StartDestination.Settings)
            expectNoEvents()
        }
    }
}

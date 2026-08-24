package com.grappim.wallosmobile.core.storage.crashreporting

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import app.cash.turbine.test
import com.grappim.wallosmobile.core.storage.FakePreferencesDataStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class CrashReportingStorageImplTest {

    @Test
    fun `crash reporting is disabled when nothing was ever stored`() = runTest {
        val storage = CrashReportingStorageImpl(FakePreferencesDataStore())

        storage.crashReportingEnabled.test {
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `setCrashReportingEnabled is observed by the flow`() = runTest {
        val storage = CrashReportingStorageImpl(FakePreferencesDataStore())

        storage.crashReportingEnabled.test {
            assertFalse(awaitItem())

            storage.setCrashReportingEnabled(true)
            assertEquals(true, awaitItem())

            storage.setCrashReportingEnabled(false)
            assertEquals(false, awaitItem())
        }
    }

    @Test
    fun `a value stored before construction is the first value emitted`() = runTest {
        val stored = mutablePreferencesOf(booleanPreferencesKey("crash_reporting_enabled") to true)

        CrashReportingStorageImpl(FakePreferencesDataStore(stored)).crashReportingEnabled.test {
            assertEquals(true, awaitItem())
        }
    }

    /** Every write to the shared file re-emits the whole `Preferences` (the key, the URL, a pin). */
    @Test
    fun `writing the same value twice emits once`() = runTest {
        val dataStore = FakePreferencesDataStore()
        val storage = CrashReportingStorageImpl(dataStore)

        storage.crashReportingEnabled.test {
            assertFalse(awaitItem())

            storage.setCrashReportingEnabled(true)
            assertEquals(true, awaitItem())

            storage.setCrashReportingEnabled(true)
            expectNoEvents()
        }
    }
}

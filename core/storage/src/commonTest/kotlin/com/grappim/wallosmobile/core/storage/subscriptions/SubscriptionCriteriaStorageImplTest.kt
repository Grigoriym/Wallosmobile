package com.grappim.wallosmobile.core.storage.subscriptions

import app.cash.turbine.test
import com.grappim.wallosmobile.core.storage.FakePreferencesDataStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SubscriptionCriteriaStorageImplTest {

    @Test
    fun `nothing stored reads as null`() = runTest {
        SubscriptionCriteriaStorageImpl(FakePreferencesDataStore()).criteria.test {
            assertNull(awaitItem())
        }
    }

    @Test
    fun `setCriteria is observed by the flow and by a later instance`() = runTest {
        val dataStore = FakePreferencesDataStore()
        val storage = SubscriptionCriteriaStorageImpl(dataStore)

        storage.criteria.test {
            assertNull(awaitItem())
            storage.setCriteria("""{"sort":"NAME"}""")
            assertEquals("""{"sort":"NAME"}""", awaitItem())
        }

        SubscriptionCriteriaStorageImpl(dataStore).criteria.test {
            assertEquals("""{"sort":"NAME"}""", awaitItem())
        }
    }

    /** Every write to the shared file re-emits the whole `Preferences`. */
    @Test
    fun `writing the same criteria twice emits once`() = runTest {
        val storage = SubscriptionCriteriaStorageImpl(FakePreferencesDataStore())

        storage.criteria.test {
            assertNull(awaitItem())
            storage.setCriteria("a")
            assertEquals("a", awaitItem())
            storage.setCriteria("a")
            expectNoEvents()
        }
    }
}

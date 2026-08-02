package com.grappim.wallosmobile.core.storage

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.cash.turbine.test
import com.grappim.wallosmobile.core.storage.db.CurrencyDao
import com.grappim.wallosmobile.core.storage.db.CurrencyEntity
import com.grappim.wallosmobile.core.storage.db.SubscriptionDao
import com.grappim.wallosmobile.core.storage.db.SubscriptionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ApiKeyStorageImplTest {

    private val dataStore = FakePreferencesDataStore()
    private val cipher = FakeSecretCipher()
    private val subscriptionDao = FakeSubscriptionDao()
    private val currencyDao = FakeCurrencyDao()
    private val storage = ApiKeyStorageImpl(dataStore, cipher, subscriptionDao, currencyDao)

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

    /** The cached rows belong to the account whose key is being dropped (3.4). */
    @Test
    fun `clear empties the cache along with the key`() = runTest {
        subscriptionDao.rows = listOf(subscriptionEntity())
        currencyDao.rows = listOf(CurrencyEntity(id = 1, name = "Euro", symbol = "€", code = "EUR"))

        storage.clear()

        assertTrue(subscriptionDao.rows.isEmpty())
        assertTrue(currencyDao.rows.isEmpty())
    }
}

private fun subscriptionEntity() = SubscriptionEntity(
    id = 1,
    name = "Fiton",
    logo = "",
    price = 9.99,
    currencyId = 1,
    currencySymbol = "€",
    cycleCode = 3,
    frequency = 1,
    nextPayment = "2026-09-01",
    startDate = null,
    isActive = true,
    notes = "",
    url = "",
    categoryName = "Health & Fitness",
    paymentMethodName = "PayPal",
    payerName = "gregorz"
)

/** Only what [ApiKeyStorageImpl] touches: the eviction. Reads are the DAO tests' business (3.3). */
private class FakeSubscriptionDao : SubscriptionDao {

    var rows: List<SubscriptionEntity> = emptyList()

    override fun observeAll(): Flow<List<SubscriptionEntity>> = flowOf(rows)

    override fun observeById(id: Int): Flow<SubscriptionEntity?> = flowOf(rows.firstOrNull { it.id == id })

    override suspend fun deleteAll() {
        rows = emptyList()
    }

    override suspend fun insertAll(subscriptions: List<SubscriptionEntity>) {
        rows = rows.filterNot { row -> subscriptions.any { it.id == row.id } } + subscriptions
    }
}

private class FakeCurrencyDao : CurrencyDao {

    var rows: List<CurrencyEntity> = emptyList()

    override suspend fun getAll(): List<CurrencyEntity> = rows

    override suspend fun deleteAll() {
        rows = emptyList()
    }

    override suspend fun insertAll(currencies: List<CurrencyEntity>) {
        rows = rows.filterNot { row -> currencies.any { it.id == row.id } } + currencies
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

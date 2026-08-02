package com.grappim.wallosmobile.core.storage.db

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Instrumented, not a host test, and it has to be: on the Android target the only `Room` builders
 * take a `Context`, and `BundledSQLiteDriver`'s native library ships in the aar's `jni/`. This
 * opens a real database — the driver, the generated `_Impl` and the schema are all under test,
 * which is the whole point of testing a DAO at all.
 */
class WallosDBTest {

    private lateinit var db: WallosDB
    private lateinit var subscriptionDao: SubscriptionDao
    private lateinit var currencyDao: CurrencyDao

    @BeforeTest
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder<WallosDB>(context)
            .setDriver(BundledSQLiteDriver())
            .build()
        subscriptionDao = db.subscriptionDao()
        currencyDao = db.currencyDao()
    }

    @AfterTest
    fun tearDown() {
        db.close()
    }

    @Test
    fun subscriptionsSurviveARoundTripInIdOrder() = runBlocking {
        subscriptionDao.replaceAll(listOf(subscription(id = 7), subscription(id = 2)))

        assertEquals(listOf(2, 7), subscriptionDao.observeAll().first().map { it.id })
    }

    @Test
    fun everyColumnComesBackUnchanged() = runBlocking {
        val row = subscription(id = 4)

        subscriptionDao.replaceAll(listOf(row))

        assertEquals(row, subscriptionDao.getById(4))
    }

    @Test
    fun nullableColumnsComeBackNull() = runBlocking {
        subscriptionDao.replaceAll(
            listOf(subscription(id = 1).copy(cycleCode = null, nextPayment = null, startDate = null))
        )

        val stored = requireNotNull(subscriptionDao.getById(1))
        assertNull(stored.cycleCode)
        assertNull(stored.nextPayment)
        assertNull(stored.startDate)
    }

    /** A row the server no longer sends must not survive the refresh that dropped it. */
    @Test
    fun replaceAllDropsRowsMissingFromTheNewList() = runBlocking {
        subscriptionDao.replaceAll(listOf(subscription(id = 1), subscription(id = 2)))

        subscriptionDao.replaceAll(listOf(subscription(id = 2, name = "renamed")))

        val stored = subscriptionDao.observeAll().first()
        assertEquals(listOf(2), stored.map { it.id })
        assertEquals("renamed", stored.single().name)
    }

    @Test
    fun getByIdIsNullForAnUnknownId() = runBlocking {
        subscriptionDao.replaceAll(listOf(subscription(id = 1)))

        assertNull(subscriptionDao.getById(99))
    }

    @Test
    fun deletingAllSubscriptionsEmptiesTheTable() = runBlocking {
        subscriptionDao.replaceAll(listOf(subscription(id = 1)))

        subscriptionDao.deleteAll()

        assertTrue(subscriptionDao.observeAll().first().isEmpty())
    }

    @Test
    fun observeAllEmitsAgainWhenTheCacheIsReplaced() = runBlocking {
        subscriptionDao.replaceAll(listOf(subscription(id = 1)))
        val before = subscriptionDao.observeAll().first()

        subscriptionDao.replaceAll(listOf(subscription(id = 1), subscription(id = 2)))
        val after = subscriptionDao.observeAll().first()

        assertEquals(1, before.size)
        assertEquals(2, after.size)
    }

    @Test
    fun currenciesSurviveARoundTrip() = runBlocking {
        val euro = CurrencyEntity(id = 1, name = "Euro", symbol = "€", code = "EUR")
        val dollar = CurrencyEntity(id = 2, name = "US Dollar", symbol = "$", code = "USD")

        currencyDao.replaceAll(listOf(dollar, euro))

        assertEquals(listOf(euro, dollar), currencyDao.getAll())
    }

    @Test
    fun deletingAllCurrenciesEmptiesTheTable() = runBlocking {
        currencyDao.replaceAll(listOf(CurrencyEntity(id = 1, name = "Euro", symbol = "€", code = "EUR")))

        currencyDao.deleteAll()

        assertTrue(currencyDao.getAll().isEmpty())
    }

    private fun subscription(id: Int, name: String = "Fiton") = SubscriptionEntity(
        id = id,
        name = name,
        logo = "fiton.png",
        price = 9.99,
        currencyId = 1,
        currencySymbol = "€",
        cycleCode = 3,
        frequency = 1,
        nextPayment = "2026-09-01",
        startDate = "2024-02-11",
        isActive = true,
        notes = "",
        url = "",
        categoryName = "Health & Fitness",
        paymentMethodName = "PayPal",
        payerName = "gregorz"
    )
}

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
    private lateinit var priceConversionDao: PriceConversionDao

    @BeforeTest
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder<WallosDB>(context)
            .setDriver(BundledSQLiteDriver())
            .build()
        subscriptionDao = db.subscriptionDao()
        currencyDao = db.currencyDao()
        priceConversionDao = db.priceConversionDao()
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

        assertEquals(row, subscriptionDao.observeById(4).first())
    }

    @Test
    fun nullableColumnsComeBackNull() = runBlocking {
        subscriptionDao.replaceAll(
            listOf(subscription(id = 1).copy(cycleCode = null, nextPayment = null, startDate = null))
        )

        val stored = requireNotNull(subscriptionDao.observeById(1).first())
        assertNull(stored.cycleCode)
        assertNull(stored.nextPayment)
        assertNull(stored.startDate)
    }

    /** The detail screen's row is rewritten by the list refresh, so its query has to re-emit. */
    @Test
    fun observeByIdEmitsAgainWhenTheRowIsRewritten() = runBlocking {
        subscriptionDao.replaceAll(listOf(subscription(id = 1)))
        val before = subscriptionDao.observeById(1).first()

        subscriptionDao.insertAll(listOf(subscription(id = 1, name = "renamed")))
        val after = subscriptionDao.observeById(1).first()

        assertEquals("Fiton", before?.name)
        assertEquals("renamed", after?.name)
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
    fun observeByIdIsNullForAnUnknownId() = runBlocking {
        subscriptionDao.replaceAll(listOf(subscription(id = 1)))

        assertNull(subscriptionDao.observeById(99).first())
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

    /** 5.4 reads this table as a flow, so it has to re-emit when a refresh replaces it. */
    @Test
    fun observeAllEmitsAgainWhenTheCurrencyTableIsReplaced() = runBlocking {
        currencyDao.replaceAll(listOf(CurrencyEntity(id = 1, name = "Euro", symbol = "€", code = "EUR")))
        val before = currencyDao.observeAll().first()

        currencyDao.replaceAll(listOf(CurrencyEntity(id = 2, name = "US Dollar", symbol = "$", code = "USD")))
        val after = currencyDao.observeAll().first()

        assertEquals(listOf("EUR"), before.map { it.code })
        assertEquals(listOf("USD"), after.map { it.code })
    }

    @Test
    fun deletingAllCurrenciesEmptiesTheTable() = runBlocking {
        currencyDao.replaceAll(listOf(CurrencyEntity(id = 1, name = "Euro", symbol = "€", code = "EUR")))

        currencyDao.deleteAll()

        assertTrue(currencyDao.getAll().isEmpty())
    }

    /** The one-row table (3.11): `REPLACE` on a fixed primary key has to overwrite, not accumulate. */
    @Test
    fun theConversionRowIsReplacedRatherThanAddedTo() = runBlocking {
        priceConversionDao.put(PriceConversionEntity(isEnabled = false, mainCurrencyId = null, hasRates = false))

        val second = PriceConversionEntity(isEnabled = true, mainCurrencyId = 2, hasRates = true)
        priceConversionDao.put(second)

        assertEquals(second, priceConversionDao.get())
    }

    @Test
    fun anAbsentConversionRowReadsAsNull() = runBlocking {
        assertNull(priceConversionDao.get())
        assertNull(priceConversionDao.observe().first())
    }

    /** A nullable `main_currency` has to survive SQLite, since an older instance sends none. */
    @Test
    fun aNullMainCurrencyComesBackNull() = runBlocking {
        priceConversionDao.put(PriceConversionEntity(isEnabled = true, mainCurrencyId = null, hasRates = true))

        assertNull(requireNotNull(priceConversionDao.get()).mainCurrencyId)
    }

    /** The list screen reads this as a flow, so a refresh writing it has to reach the screen. */
    @Test
    fun observeEmitsAgainWhenTheConversionRowIsRewritten() = runBlocking {
        priceConversionDao.put(PriceConversionEntity(isEnabled = false, mainCurrencyId = 1, hasRates = false))
        val before = priceConversionDao.observe().first()

        priceConversionDao.put(PriceConversionEntity(isEnabled = true, mainCurrencyId = 1, hasRates = true))
        val after = priceConversionDao.observe().first()

        assertEquals(false, before?.isEnabled)
        assertEquals(true, after?.isEnabled)
    }

    @Test
    fun deletingTheConversionRowEmptiesTheTable() = runBlocking {
        priceConversionDao.put(PriceConversionEntity(isEnabled = true, mainCurrencyId = 1, hasRates = true))

        priceConversionDao.deleteAll()

        assertNull(priceConversionDao.get())
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

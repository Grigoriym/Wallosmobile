package com.grappim.wallosmobile.feature.subscriptions.data

import app.cash.turbine.test
import com.grappim.wallosmobile.core.domain.WallosError
import com.grappim.wallosmobile.core.storage.db.CurrencyDao
import com.grappim.wallosmobile.core.storage.db.CurrencyEntity
import com.grappim.wallosmobile.core.storage.db.SubscriptionDao
import com.grappim.wallosmobile.core.storage.db.SubscriptionEntity
import com.grappim.wallosmobile.feature.subscriptions.dto.CurrencyDTO
import com.grappim.wallosmobile.feature.subscriptions.dto.SubscriptionDTO
import com.grappim.wallosmobile.feature.subscriptions.mapper.CurrencyEntityMapper
import com.grappim.wallosmobile.feature.subscriptions.mapper.CurrencyMapper
import com.grappim.wallosmobile.feature.subscriptions.mapper.HtmlUnescaper
import com.grappim.wallosmobile.feature.subscriptions.mapper.SubscriptionEntityMapper
import com.grappim.wallosmobile.feature.subscriptions.mapper.SubscriptionMapper
import com.grappim.wallosmobile.utils.formatter.datetime.DateFormatter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SubscriptionsRepositoryImplTest {

    private val subscriptionDao = FakeSubscriptionDao()
    private val currencyDao = FakeCurrencyDao()

    @Test
    fun `observe emits what the cache holds, mapped back to the model`() = runTest {
        subscriptionDao.rows = listOf(entity(id = 1, name = "Fiton"), entity(id = 2, name = "Netflix"))

        repository().observeSubscriptions().test {
            assertEquals(listOf("Fiton", "Netflix"), awaitItem().map { it.name })
        }
    }

    @Test
    fun `observe emits an empty list before anything has been cached`() = runTest {
        repository().observeSubscriptions().test {
            assertTrue(awaitItem().isEmpty())
        }
    }

    @Test
    fun `a refresh reaches the screen through the cache, not around it`() = runTest {
        val api = FakeSubscriptionsApi(
            subscriptions = listOf(subscriptionDTO(id = 1)),
            currencies = listOf(CurrencyDTO(id = 1, symbol = "€"))
        )
        val repository = repository(api)

        repository.observeSubscriptions().test {
            assertTrue(awaitItem().isEmpty())

            repository.refreshSubscriptions().getOrThrow()

            assertEquals(listOf(1), awaitItem().map { it.id })
        }
    }

    @Test
    fun `refresh joins every subscription to the symbol its currency_id points at`() = runTest {
        val api = FakeSubscriptionsApi(
            subscriptions = listOf(subscriptionDTO(id = 1, currencyId = 1), subscriptionDTO(id = 2, currencyId = 2)),
            currencies = listOf(
                CurrencyDTO(id = 1, name = "Euro", symbol = "€", code = "EUR"),
                CurrencyDTO(id = 2, name = "US Dollar", symbol = "$", code = "USD")
            )
        )

        repository(api).refreshSubscriptions().getOrThrow()

        assertEquals(listOf("€", "$"), subscriptionDao.rows.map { it.currencySymbol })
    }

    @Test
    fun `refresh leaves the symbol blank for a currency the instance no longer lists`() = runTest {
        val api = FakeSubscriptionsApi(
            subscriptions = listOf(subscriptionDTO(currencyId = 404)),
            currencies = listOf(CurrencyDTO(id = 1, symbol = "€"))
        )

        repository(api).refreshSubscriptions().getOrThrow()

        assertEquals("", subscriptionDao.rows.single().currencySymbol)
    }

    @Test
    fun `refresh maps the rows on the way through`() = runTest {
        val api = FakeSubscriptionsApi(
            subscriptions = listOf(subscriptionDTO(name = "1&amp;1 Telekom")),
            currencies = listOf(CurrencyDTO(id = 1, symbol = "€"))
        )

        repository(api).refreshSubscriptions().getOrThrow()

        assertEquals("1&1 Telekom", subscriptionDao.rows.single().name)
    }

    /** The currency table is cached for the *next* refresh's resolution (plan §4.7). */
    @Test
    fun `refresh caches the currency list too`() = runTest {
        val api = FakeSubscriptionsApi(
            subscriptions = emptyList(),
            currencies = listOf(CurrencyDTO(id = 2, name = "US Dollar", symbol = "$", code = "USD"))
        )

        repository(api).refreshSubscriptions().getOrThrow()

        assertEquals(
            listOf(CurrencyEntity(id = 2, name = "US Dollar", symbol = "$", code = "USD")),
            currencyDao.rows
        )
    }

    /** A whole-list fetch is a snapshot: a row it doesn't carry has been deleted server-side. */
    @Test
    fun `refresh drops a cached row the server no longer sends`() = runTest {
        subscriptionDao.rows = listOf(entity(id = 1), entity(id = 2))
        val api = FakeSubscriptionsApi(
            subscriptions = listOf(subscriptionDTO(id = 2)),
            currencies = listOf(CurrencyDTO(id = 1, symbol = "€"))
        )

        repository(api).refreshSubscriptions().getOrThrow()

        assertEquals(listOf(2), subscriptionDao.rows.map { it.id })
    }

    /**
     * The whole point of the cache: 2.4 cleared the list on a failure because there was nothing
     * behind the error. There is now, and a failed refresh must not touch it.
     */
    @Test
    fun `a failed refresh leaves the cached rows standing`() = runTest {
        subscriptionDao.rows = listOf(entity(id = 1, name = "Fiton"))
        val api = FakeSubscriptionsApi(subscriptionsFailure = WallosError.Server("502 Bad Gateway"))

        val result = repository(api).refreshSubscriptions()

        assertFailsWith<WallosError.Server> { result.getOrThrow() }
        assertEquals(listOf("Fiton"), subscriptionDao.rows.map { it.name })
    }

    /**
     * The subscriptions come first so a failure on the resource the user asked for short-circuits
     * before the second round trip.
     */
    @Test
    fun `does not fetch currencies when the subscriptions call fails`() = runTest {
        val api = FakeSubscriptionsApi(subscriptionsFailure = WallosError.Unauthenticated("Invalid API key"))

        assertTrue(repository(api).refreshSubscriptions().isFailure)
        assertEquals(0, api.currenciesCalls)
    }

    /** Half a refresh is worse than none: neither table is written unless both calls answered. */
    @Test
    fun `writes nothing when the currencies call fails`() = runTest {
        subscriptionDao.rows = listOf(entity(id = 1, name = "Fiton"))
        val api = FakeSubscriptionsApi(
            subscriptions = listOf(subscriptionDTO(id = 9, name = "Netflix")),
            currenciesFailure = WallosError.Malformed("<html>")
        )

        assertFailsWith<WallosError.Malformed> { repository(api).refreshSubscriptions().getOrThrow() }
        assertEquals(listOf("Fiton"), subscriptionDao.rows.map { it.name })
        assertTrue(currencyDao.rows.isEmpty())
    }

    @Test
    fun `observing one row reads it out of the cache`() = runTest {
        subscriptionDao.rows = listOf(entity(id = 1), entity(id = 4, name = "Fiton"))

        repository().observeSubscription(id = 4).test {
            assertEquals("Fiton", awaitItem()?.name)
        }
    }

    @Test
    fun `observing an uncached row emits null`() = runTest {
        repository().observeSubscription(id = 99).test {
            assertNull(awaitItem())
        }
    }

    /** 2.5's second round trip, paid off: the symbol comes from the cached currency table. */
    @Test
    fun `refreshing one row upserts it using the cached currencies`() = runTest {
        subscriptionDao.rows = listOf(entity(id = 4, name = "stale"), entity(id = 7))
        currencyDao.rows = listOf(CurrencyEntity(id = 2, name = "US Dollar", symbol = "$", code = "USD"))
        val api = FakeSubscriptionsApi(subscription = subscriptionDTO(id = 4, name = "Fiton", currencyId = 2))

        repository(api).refreshSubscription(id = 4).getOrThrow()

        val stored = subscriptionDao.rows.single { it.id == 4 }
        assertEquals("Fiton", stored.name)
        assertEquals("$", stored.currencySymbol)
        assertEquals(listOf(4, 7), subscriptionDao.rows.map { it.id }.sorted())
        assertEquals(0, api.currenciesCalls)
    }

    /** The one case the cached table can't answer: nothing has filled it yet. */
    @Test
    fun `refreshing one row fetches the currencies when the cache has none`() = runTest {
        val api = FakeSubscriptionsApi(
            subscription = subscriptionDTO(id = 4, currencyId = 1),
            currencies = listOf(CurrencyDTO(id = 1, symbol = "€"))
        )

        repository(api).refreshSubscription(id = 4).getOrThrow()

        assertEquals("€", subscriptionDao.rows.single().currencySymbol)
        assertEquals(1, api.currenciesCalls)
        assertEquals(listOf(1), currencyDao.rows.map { it.id })
    }

    /**
     * `Unauthorized or Not Found` is a per-row ownership answer as much as a deletion (API doc
     * §3.3), so it must not be read as "drop the row".
     */
    @Test
    fun `a failed single refresh leaves the cached row alone`() = runTest {
        subscriptionDao.rows = listOf(entity(id = 4, name = "Fiton"))
        val api = FakeSubscriptionsApi(singleFailure = WallosError.NotFound("Unauthorized or Not Found"))

        assertTrue(repository(api).refreshSubscription(id = 4).isFailure)
        assertEquals(listOf("Fiton"), subscriptionDao.rows.map { it.name })
    }

    private fun repository(api: SubscriptionsApi = FakeSubscriptionsApi()): SubscriptionsRepositoryImpl {
        val unescaper = HtmlUnescaper()
        val dateFormatter = DateFormatter()
        return SubscriptionsRepositoryImpl(
            api = api,
            cache = SubscriptionsCache(
                subscriptionDao = subscriptionDao,
                currencyDao = currencyDao,
                subscriptionEntityMapper = SubscriptionEntityMapper(dateFormatter),
                currencyEntityMapper = CurrencyEntityMapper()
            ),
            subscriptionMapper = SubscriptionMapper(dateFormatter, unescaper),
            currencyMapper = CurrencyMapper(unescaper),
            dispatcher = UnconfinedTestDispatcher()
        )
    }

    private fun subscriptionDTO(id: Int = 1, name: String = "Fiton", currencyId: Int = 1) = SubscriptionDTO(
        id = id,
        name = name,
        price = 31.99,
        currencyId = currencyId,
        nextPayment = "2026-01-31",
        cycle = 3,
        frequency = 1
    )

    private fun entity(id: Int, name: String = "Fiton") = SubscriptionEntity(
        id = id,
        name = name,
        logo = "",
        price = 31.99,
        currencyId = 1,
        currencySymbol = "€",
        cycleCode = 3,
        frequency = 1,
        nextPayment = "2026-01-31",
        startDate = null,
        isActive = true,
        notes = "",
        url = "",
        categoryName = "No category",
        paymentMethodName = "PayPal",
        payerName = "gregorz"
    )

    /**
     * `error("… not set")` rather than a default: a test that forgot to arrange its data should
     * fail with a message naming the missing field, not silently assert on an empty list
     * (plan §6.1).
     */
    private class FakeSubscriptionsApi(
        private val subscriptions: List<SubscriptionDTO>? = null,
        private val subscription: SubscriptionDTO? = null,
        private val currencies: List<CurrencyDTO>? = null,
        private val subscriptionsFailure: Throwable? = null,
        private val singleFailure: Throwable? = null,
        private val currenciesFailure: Throwable? = null
    ) : SubscriptionsApi {

        val singleCalls = mutableListOf<Int>()
        var currenciesCalls = 0
            private set

        override suspend fun getSubscriptions(): List<SubscriptionDTO> {
            subscriptionsFailure?.let { throw it }
            return subscriptions ?: error("subscriptions not set")
        }

        override suspend fun getSubscription(id: Int): SubscriptionDTO {
            singleCalls += id
            singleFailure?.let { throw it }
            return subscription ?: error("subscription not set")
        }

        override suspend fun getCurrencies(): List<CurrencyDTO> {
            currenciesCalls++
            currenciesFailure?.let { throw it }
            return currencies ?: error("currencies not set")
        }
    }
}

/**
 * The DAO contract the repository leans on, in memory: `insertAll` upserts by id (the real one is
 * `OnConflictStrategy.REPLACE`) and the observers re-emit on every write, which is what the real
 * Room query does and what the cache-first ViewModels depend on. The queries themselves are the
 * instrumented DAO suite's business (3.3).
 */
private class FakeSubscriptionDao : SubscriptionDao {

    private val state = MutableStateFlow<List<SubscriptionEntity>>(emptyList())

    var rows: List<SubscriptionEntity>
        get() = state.value
        set(value) {
            state.value = value.sortedBy { it.id }
        }

    override fun observeAll(): Flow<List<SubscriptionEntity>> = state

    override fun observeById(id: Int): Flow<SubscriptionEntity?> = state.map { rows ->
        rows.firstOrNull { it.id == id }
    }

    override suspend fun deleteAll() {
        rows = emptyList()
    }

    override suspend fun insertAll(subscriptions: List<SubscriptionEntity>) {
        rows = rows.filterNot { row -> subscriptions.any { it.id == row.id } } + subscriptions
    }
}

private class FakeCurrencyDao : CurrencyDao {

    var rows: List<CurrencyEntity> = emptyList()

    override suspend fun getAll(): List<CurrencyEntity> = rows.sortedBy { it.id }

    override suspend fun deleteAll() {
        rows = emptyList()
    }

    override suspend fun insertAll(currencies: List<CurrencyEntity>) {
        rows = rows.filterNot { row -> currencies.any { it.id == row.id } } + currencies
    }
}

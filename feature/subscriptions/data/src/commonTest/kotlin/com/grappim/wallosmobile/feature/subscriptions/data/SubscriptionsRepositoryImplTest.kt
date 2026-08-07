package com.grappim.wallosmobile.feature.subscriptions.data

import app.cash.turbine.test
import com.grappim.wallosmobile.core.api.FormParams
import com.grappim.wallosmobile.core.domain.WallosError
import com.grappim.wallosmobile.core.storage.db.CurrencyDao
import com.grappim.wallosmobile.core.storage.db.CurrencyEntity
import com.grappim.wallosmobile.core.storage.db.PriceConversionDao
import com.grappim.wallosmobile.core.storage.db.PriceConversionEntity
import com.grappim.wallosmobile.core.storage.db.SubscriptionDao
import com.grappim.wallosmobile.core.storage.db.SubscriptionEntity
import com.grappim.wallosmobile.feature.subscriptions.domain.model.AddSubscriptionParams
import com.grappim.wallosmobile.feature.subscriptions.domain.model.EditSubscriptionParams
import com.grappim.wallosmobile.feature.subscriptions.domain.model.PriceConversion
import com.grappim.wallosmobile.feature.subscriptions.domain.model.WritableBillingCycle
import com.grappim.wallosmobile.feature.subscriptions.dto.CurrencyDTO
import com.grappim.wallosmobile.feature.subscriptions.dto.SubscriptionDTO
import com.grappim.wallosmobile.feature.subscriptions.mapper.CurrencyEntityMapper
import com.grappim.wallosmobile.feature.subscriptions.mapper.CurrencyMapper
import com.grappim.wallosmobile.feature.subscriptions.mapper.HtmlUnescaper
import com.grappim.wallosmobile.feature.subscriptions.mapper.PriceConversionEntityMapper
import com.grappim.wallosmobile.feature.subscriptions.mapper.SubscriptionEntityMapper
import com.grappim.wallosmobile.feature.subscriptions.mapper.SubscriptionMapper
import com.grappim.wallosmobile.utils.formatter.datetime.DateFormatter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SubscriptionsRepositoryImplTest {

    private val subscriptionDao = FakeSubscriptionDao()
    private val currencyDao = FakeCurrencyDao()
    private val priceConversionDao = FakePriceConversionDao()

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

    // --- 3.11: currency conversion ------------------------------------------------------------

    @Test
    fun `refresh asks for conversion only when the instance's own setting says so`() = runTest {
        val on = FakeSubscriptionsApi(
            subscriptions = emptyList(),
            currencies = listOf(euro()),
            isConversionEnabled = true
        )
        val off = FakeSubscriptionsApi(subscriptions = emptyList(), currencies = listOf(euro()))

        repository(on).refreshSubscriptions().getOrThrow()
        repository(off).refreshSubscriptions().getOrThrow()

        assertEquals(listOf(true), on.conversionAsked)
        assertEquals(listOf(false), off.conversionAsked)
    }

    /**
     * The finding the whole step turns on: `get_subscriptions.php` overwrites `price` and leaves
     * `currency_id` naming the currency it converted **from**, so taking the row's own symbol puts
     * a dollar sign in front of an amount in euros.
     */
    @Test
    fun `a converted row is stored with the main currency's symbol, not its own`() = runTest {
        val api = FakeSubscriptionsApi(
            subscriptions = listOf(subscriptionDTO(id = 1, currencyId = 2)),
            currencies = listOf(euro(), dollar(rate = "1.09")),
            mainCurrencyId = 1,
            isConversionEnabled = true
        )

        repository(api).refreshSubscriptions().getOrThrow()

        assertEquals("€", subscriptionDao.rows.single().currencySymbol)
    }

    /** The server skips a row already in the main currency, so its own symbol is still the truth. */
    @Test
    fun `a row already in the main currency keeps its own symbol`() = runTest {
        val api = FakeSubscriptionsApi(
            subscriptions = listOf(subscriptionDTO(id = 1, currencyId = 1)),
            currencies = listOf(euro(), dollar(rate = "1.09")),
            mainCurrencyId = 1,
            isConversionEnabled = true
        )

        repository(api).refreshSubscriptions().getOrThrow()

        assertEquals("€", subscriptionDao.rows.single().currencySymbol)
    }

    /**
     * The silent failure itself (API doc §5.5): conversion was asked for, the instance has never
     * fetched rates, and the response says nothing — same `success: true`, same empty `notes`. Every
     * rate still at exactly `1` is the only observable trace, and the price must keep its own sign.
     */
    @Test
    fun `conversion without exchange rates leaves every price in its own currency`() = runTest {
        val api = FakeSubscriptionsApi(
            subscriptions = listOf(subscriptionDTO(id = 1, currencyId = 2)),
            currencies = listOf(euro(), dollar()),
            mainCurrencyId = 1,
            isConversionEnabled = true
        )

        repository(api).refreshSubscriptions().getOrThrow()

        assertEquals("$", subscriptionDao.rows.single().currencySymbol)
    }

    /** No target to convert into, so nothing was converted whatever the setting says. */
    @Test
    fun `an instance that sends no main_currency converts nothing`() = runTest {
        val api = FakeSubscriptionsApi(
            subscriptions = listOf(subscriptionDTO(id = 1, currencyId = 2)),
            currencies = listOf(euro(), dollar(rate = "1.09")),
            mainCurrencyId = null,
            isConversionEnabled = true
        )

        repository(api).refreshSubscriptions().getOrThrow()

        assertEquals("$", subscriptionDao.rows.single().currencySymbol)
    }

    @Test
    fun `refresh caches the conversion state beside the prices it explains`() = runTest {
        val api = FakeSubscriptionsApi(
            subscriptions = emptyList(),
            currencies = listOf(euro(), dollar(rate = "1.09")),
            mainCurrencyId = 1,
            isConversionEnabled = true
        )
        val repository = repository(api)

        repository.refreshSubscriptions().getOrThrow()

        repository.observePriceConversion().test {
            assertEquals(PriceConversion(isEnabled = true, mainCurrencyId = 1, hasRates = true), awaitItem())
        }
    }

    /**
     * The table behind 5.4's label: a converted row keeps the `currency_id` it came from, and only
     * this list turns that id into something a screen can name.
     */
    @Test
    fun `a refresh leaves the currency list where a screen can read it`() = runTest {
        val api = FakeSubscriptionsApi(
            subscriptions = emptyList(),
            currencies = listOf(euro(), dollar()),
            mainCurrencyId = 1
        )
        val repository = repository(api)

        repository.refreshSubscriptions().getOrThrow()

        repository.observeCurrencies().test {
            assertEquals(listOf("EUR", "USD"), awaitItem().map { it.code })
        }
    }

    @Test
    fun `observing currencies before any refresh emits an empty list`() = runTest {
        repository().observeCurrencies().test {
            assertTrue(awaitItem().isEmpty())
        }
    }

    /** Nothing refreshed yet is indistinguishable from nothing converted, and reads as such. */
    @Test
    fun `observing the conversion state before any refresh converts nothing`() = runTest {
        repository().observePriceConversion().test {
            val conversion = awaitItem()
            assertFalse(conversion.isActive)
            assertFalse(conversion.isEnabledWithoutRates)
        }
    }

    /**
     * A preference is not data: `api/settings/get_settings.php` is missing on an old enough
     * instance, and a 404 there must cost the conversion rather than the list.
     */
    @Test
    fun `a settings read that fails leaves the refresh working, unconverted`() = runTest {
        val api = FakeSubscriptionsApi(
            subscriptions = listOf(subscriptionDTO(id = 1, currencyId = 2)),
            currencies = listOf(euro(), dollar(rate = "1.09")),
            settingsFailure = WallosError.NotFound("404")
        )

        repository(api).refreshSubscriptions().getOrThrow()

        assertEquals(listOf(false), api.conversionAsked)
        assertEquals("$", subscriptionDao.rows.single().currencySymbol)
    }

    /** One row cannot disagree with the list it sits in, so it is fetched the same way. */
    @Test
    fun `refreshing one row asks for the conversion the cached list was fetched with`() = runTest {
        priceConversionDao.row = PriceConversionEntity(isEnabled = true, mainCurrencyId = 1, hasRates = true)
        currencyDao.rows = listOf(
            CurrencyEntity(id = 1, name = "Euro", symbol = "€", code = "EUR"),
            CurrencyEntity(id = 2, name = "US Dollar", symbol = "$", code = "USD")
        )
        val api = FakeSubscriptionsApi(subscription = subscriptionDTO(id = 4, currencyId = 2))

        repository(api).refreshSubscription(id = 4).getOrThrow()

        assertEquals(listOf(true), api.conversionAsked)
        assertEquals("€", subscriptionDao.rows.single().currencySymbol)
    }

    /** A detail opened before any list refresh: convert nothing rather than guess (3.11). */
    @Test
    fun `refreshing one row into an empty cache converts nothing`() = runTest {
        val api = FakeSubscriptionsApi(
            subscription = subscriptionDTO(id = 4, currencyId = 2),
            currencies = listOf(euro(), dollar(rate = "1.09"))
        )

        repository(api).refreshSubscription(id = 4).getOrThrow()

        assertEquals(listOf(false), api.conversionAsked)
        assertEquals("$", subscriptionDao.rows.single().currencySymbol)
    }

    // --- 7.5: add / edit / delete --------------------------------------------------------------

    @Test
    fun `add reaches the wire, then re-syncs the cache with the new row`() = runTest {
        val api = FakeSubscriptionsApi(
            subscriptions = listOf(subscriptionDTO(id = 55, name = "Netflix")),
            currencies = listOf(CurrencyDTO(id = 1, symbol = "€")),
            addResult = 55
        )

        val id = repository(api).addSubscription(addParams()).getOrThrow()

        assertEquals(55, id)
        assertEquals(listOf(55), subscriptionDao.rows.map { it.id })
    }

    @Test
    fun `add sends the required fields, the cycle's wire code among them`() = runTest {
        val api = FakeSubscriptionsApi(
            subscriptions = emptyList(),
            currencies = listOf(CurrencyDTO(id = 1, symbol = "€")),
            addResult = 1
        )

        repository(api).addSubscription(addParams(cycle = WritableBillingCycle.YEARS)).getOrThrow()

        val sent = api.addFields.single().asMap()
        assertEquals("Netflix", sent["name"])
        assertEquals("4", sent["cycle"])
        assertEquals("2026-01-31", sent["next_payment"])
        assertEquals("1", sent["auto_renew"])
        assertEquals("0", sent["inactive"])
    }

    /** The server-side `cycle`/`Missing parameters` guard, surfacing here rather than in the api test alone. */
    @Test
    fun `a server-rejected add leaves the cache untouched`() = runTest {
        subscriptionDao.rows = listOf(entity(id = 1, name = "Fiton"))
        val api = FakeSubscriptionsApi(addFailure = WallosError.Validation("Invalid parameter", "bad cycle"))

        val result = repository(api).addSubscription(addParams())

        assertFailsWith<WallosError.Validation> { result.getOrThrow() }
        assertEquals(listOf("Fiton"), subscriptionDao.rows.map { it.name })
    }

    @Test
    fun `edit reaches the wire, then re-syncs the cache`() = runTest {
        val api = FakeSubscriptionsApi(
            subscriptions = listOf(subscriptionDTO(id = 4, name = "Renamed")),
            currencies = listOf(CurrencyDTO(id = 1, symbol = "€"))
        )

        repository(api).editSubscription(4, EditSubscriptionParams(name = "Renamed")).getOrThrow()

        assertEquals(listOf("Renamed"), subscriptionDao.rows.map { it.name })
        assertEquals(listOf(4 to "Renamed"), api.editCalls.map { (id, fields) -> id to fields.asMap()["name"] })
    }

    /** Only the fields actually set reach the wire — the server keeps its own value for the rest. */
    @Test
    fun `edit omits every field the caller didn't set`() = runTest {
        val api =
            FakeSubscriptionsApi(subscriptions = emptyList(), currencies = listOf(CurrencyDTO(id = 1, symbol = "€")))

        repository(api).editSubscription(4, EditSubscriptionParams(name = "Renamed")).getOrThrow()

        assertEquals(setOf("name"), api.editCalls.single().second.asMap().keys)
    }

    @Test
    fun `a server-rejected edit leaves the cache untouched`() = runTest {
        subscriptionDao.rows = listOf(entity(id = 4, name = "Fiton"))
        val api = FakeSubscriptionsApi(editFailure = WallosError.Validation("Invalid cycle", "bad cycle"))

        val result = repository(api).editSubscription(4, EditSubscriptionParams(name = "Fitness"))

        assertFailsWith<WallosError.Validation> { result.getOrThrow() }
        assertEquals(listOf("Fiton"), subscriptionDao.rows.map { it.name })
    }

    /** Delete removes the row from the cache directly — there is nothing left server-side to refetch. */
    @Test
    fun `delete removes the row from the cache without a refresh round trip`() = runTest {
        subscriptionDao.rows = listOf(entity(id = 1, name = "Fiton"), entity(id = 2, name = "Netflix"))
        val api = FakeSubscriptionsApi()

        repository(api).deleteSubscription(1).getOrThrow()

        assertEquals(listOf(2), subscriptionDao.rows.map { it.id })
        assertEquals(0, api.currenciesCalls)
    }

    @Test
    fun `a server-rejected delete leaves the cached row standing`() = runTest {
        subscriptionDao.rows = listOf(entity(id = 1, name = "Fiton"))
        val api = FakeSubscriptionsApi(deleteFailure = WallosError.NotFound("Unauthorized or Not Found"))

        val result = repository(api).deleteSubscription(1)

        assertFailsWith<WallosError.NotFound> { result.getOrThrow() }
        assertEquals(listOf("Fiton"), subscriptionDao.rows.map { it.name })
    }

    private fun addParams(cycle: WritableBillingCycle = WritableBillingCycle.YEARS) = AddSubscriptionParams(
        name = "Netflix",
        price = 31.99,
        currencyId = 1,
        cycle = cycle,
        frequency = 1,
        nextPayment = LocalDate(2026, 1, 31)
    )

    private fun euro(rate: String = "1") = CurrencyDTO(id = 1, name = "Euro", symbol = "€", code = "EUR", rate = rate)

    private fun dollar(rate: String = "1") =
        CurrencyDTO(id = 2, name = "US Dollar", symbol = "$", code = "USD", rate = rate)

    private fun repository(api: SubscriptionsApi = FakeSubscriptionsApi()): SubscriptionsRepositoryImpl {
        val unescaper = HtmlUnescaper()
        val dateFormatter = DateFormatter()
        return SubscriptionsRepositoryImpl(
            api = api,
            cache = SubscriptionsCache(
                subscriptionDao = subscriptionDao,
                currencyDao = currencyDao,
                priceConversionDao = priceConversionDao,
                subscriptionEntityMapper = SubscriptionEntityMapper(dateFormatter),
                currencyEntityMapper = CurrencyEntityMapper(),
                priceConversionEntityMapper = PriceConversionEntityMapper()
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
        payerName = "gregorz",
        categoryId = null,
        paymentMethodId = null,
        payerUserId = null,
        autoRenew = true,
        notify = false,
        notifyDaysBefore = null
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
        private val mainCurrencyId: Int? = 1,
        private val isConversionEnabled: Boolean = false,
        private val subscriptionsFailure: Throwable? = null,
        private val singleFailure: Throwable? = null,
        private val currenciesFailure: Throwable? = null,
        private val settingsFailure: Throwable? = null,
        private val addResult: Int? = null,
        private val addFailure: Throwable? = null,
        private val editFailure: Throwable? = null,
        private val deleteFailure: Throwable? = null
    ) : SubscriptionsApi {

        val singleCalls = mutableListOf<Int>()
        var currenciesCalls = 0
            private set

        val addFields = mutableListOf<FormParams>()
        val editCalls = mutableListOf<Pair<Int, FormParams>>()
        val deleteCalls = mutableListOf<Int>()

        /** What the two reads were actually asked for, which is the whole of 3.11's request half. */
        val conversionAsked = mutableListOf<Boolean>()

        override suspend fun getSubscriptions(convertCurrency: Boolean): List<SubscriptionDTO> {
            conversionAsked += convertCurrency
            subscriptionsFailure?.let { throw it }
            return subscriptions ?: error("subscriptions not set")
        }

        override suspend fun getSubscription(id: Int, convertCurrency: Boolean): SubscriptionDTO {
            singleCalls += id
            conversionAsked += convertCurrency
            singleFailure?.let { throw it }
            return subscription ?: error("subscription not set")
        }

        override suspend fun getCurrencies(): CurrenciesPayload {
            currenciesCalls++
            currenciesFailure?.let { throw it }
            return CurrenciesPayload(
                currencies = currencies ?: error("currencies not set"),
                mainCurrencyId = mainCurrencyId
            )
        }

        override suspend fun isCurrencyConversionEnabled(): Boolean {
            settingsFailure?.let { throw it }
            return isConversionEnabled
        }

        override suspend fun addSubscription(fields: FormParams): Int {
            addFields += fields
            addFailure?.let { throw it }
            return addResult ?: error("addResult not set")
        }

        override suspend fun editSubscription(id: Int, fields: FormParams) {
            editCalls += id to fields
            editFailure?.let { throw it }
        }

        override suspend fun deleteSubscription(id: Int) {
            deleteCalls += id
            deleteFailure?.let { throw it }
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

    override suspend fun deleteById(id: Int) {
        rows = rows.filterNot { it.id == id }
    }

    override suspend fun insertAll(subscriptions: List<SubscriptionEntity>) {
        rows = rows.filterNot { row -> subscriptions.any { it.id == row.id } } + subscriptions
    }
}

private class FakeCurrencyDao : CurrencyDao {

    private val state = MutableStateFlow<List<CurrencyEntity>>(emptyList())

    var rows: List<CurrencyEntity>
        get() = state.value
        set(value) {
            state.value = value.sortedBy { it.id }
        }

    override suspend fun getAll(): List<CurrencyEntity> = rows

    override fun observeAll(): Flow<List<CurrencyEntity>> = state

    override suspend fun deleteAll() {
        rows = emptyList()
    }

    override suspend fun insertAll(currencies: List<CurrencyEntity>) {
        rows = rows.filterNot { row -> currencies.any { it.id == row.id } } + currencies
    }
}

private class FakePriceConversionDao : PriceConversionDao {

    private val state = MutableStateFlow<PriceConversionEntity?>(null)

    var row: PriceConversionEntity?
        get() = state.value
        set(value) {
            state.value = value
        }

    override fun observe(): Flow<PriceConversionEntity?> = state

    override suspend fun get(): PriceConversionEntity? = state.value

    override suspend fun put(conversion: PriceConversionEntity) {
        state.value = conversion
    }

    override suspend fun deleteAll() {
        state.value = null
    }
}

package com.grappim.wallosmobile.feature.subscriptions.data

import com.grappim.wallosmobile.core.domain.WallosError
import com.grappim.wallosmobile.feature.subscriptions.dto.CurrencyDTO
import com.grappim.wallosmobile.feature.subscriptions.dto.SubscriptionDTO
import com.grappim.wallosmobile.feature.subscriptions.mapper.CurrencyMapper
import com.grappim.wallosmobile.feature.subscriptions.mapper.HtmlUnescaper
import com.grappim.wallosmobile.feature.subscriptions.mapper.SubscriptionMapper
import com.grappim.wallosmobile.utils.formatter.datetime.DateFormatter
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SubscriptionsRepositoryImplTest {

    @Test
    fun `joins every subscription to the symbol its currency_id points at`() = runTest {
        val api = FakeSubscriptionsApi(
            subscriptions = listOf(subscriptionDTO(id = 1, currencyId = 1), subscriptionDTO(id = 2, currencyId = 2)),
            currencies = listOf(
                CurrencyDTO(id = 1, name = "Euro", symbol = "€", code = "EUR"),
                CurrencyDTO(id = 2, name = "US Dollar", symbol = "$", code = "USD")
            )
        )

        val result = repository(api).getSubscriptions().getOrThrow()

        assertEquals(listOf("€", "$"), result.map { it.currencySymbol })
    }

    @Test
    fun `leaves the symbol blank for a currency the instance no longer lists`() = runTest {
        val api = FakeSubscriptionsApi(
            subscriptions = listOf(subscriptionDTO(currencyId = 404)),
            currencies = listOf(CurrencyDTO(id = 1, symbol = "€"))
        )

        assertEquals("", repository(api).getSubscriptions().getOrThrow().single().currencySymbol)
    }

    @Test
    fun `maps the rows on the way through`() = runTest {
        val api = FakeSubscriptionsApi(
            subscriptions = listOf(subscriptionDTO(name = "1&amp;1 Telekom")),
            currencies = listOf(CurrencyDTO(id = 1, symbol = "€"))
        )

        assertEquals("1&1 Telekom", repository(api).getSubscriptions().getOrThrow().single().name)
    }

    @Test
    fun `joins the single subscription too`() = runTest {
        val api = FakeSubscriptionsApi(
            subscription = subscriptionDTO(id = 4, currencyId = 2),
            currencies = listOf(CurrencyDTO(id = 1, symbol = "€"), CurrencyDTO(id = 2, symbol = "$"))
        )

        val result = repository(api).getSubscription(id = 4).getOrThrow()

        assertEquals(4, result.id)
        assertEquals("$", result.currencySymbol)
        assertEquals(listOf(4), api.singleCalls)
    }

    @Test
    fun `returns an empty list without inventing anything`() = runTest {
        val api = FakeSubscriptionsApi(subscriptions = emptyList(), currencies = emptyList())

        assertTrue(repository(api).getSubscriptions().getOrThrow().isEmpty())
    }

    /**
     * The subscriptions come first so a failure on the resource the user asked for short-circuits
     * before the second round trip.
     */
    @Test
    fun `does not fetch currencies when the subscriptions call fails`() = runTest {
        val api = FakeSubscriptionsApi(subscriptionsFailure = WallosError.Unauthenticated("Invalid API key"))

        val result = repository(api).getSubscriptions()

        assertFailsWith<WallosError.Unauthenticated> { result.getOrThrow() }
        assertEquals(0, api.currenciesCalls)
    }

    @Test
    fun `reports a failed currencies call as a failure, not a list with no symbols`() = runTest {
        val api = FakeSubscriptionsApi(
            subscriptions = listOf(subscriptionDTO()),
            currenciesFailure = WallosError.Malformed("<html>")
        )

        assertFailsWith<WallosError.Malformed> { repository(api).getSubscriptions().getOrThrow() }
    }

    /** `resultOf` catches, so a thrown `WallosError` never escapes as an exception. */
    @Test
    fun `wraps a thrown error in the Result rather than propagating it`() = runTest {
        val api = FakeSubscriptionsApi(singleFailure = WallosError.NotFound("Subscription not found"))

        assertTrue(repository(api).getSubscription(id = 1).isFailure)
    }

    private fun repository(api: SubscriptionsApi): SubscriptionsRepositoryImpl {
        val unescaper = HtmlUnescaper()
        return SubscriptionsRepositoryImpl(
            api = api,
            subscriptionMapper = SubscriptionMapper(DateFormatter(), unescaper),
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

package com.grappim.wallosmobile.feature.subscriptions.data

import com.grappim.wallosmobile.core.api.WallosApiClient
import com.grappim.wallosmobile.core.api.WallosEnvelopeParser
import com.grappim.wallosmobile.core.domain.WallosError
import com.grappim.wallosmobile.core.storage.ApiKeyStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.forms.FormDataContent
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SubscriptionsApiImplTest {

    @Test
    fun `unwraps the subscriptions envelope`() = runTest {
        val api = api(SubscriptionsJsonFixtures.SUBSCRIPTIONS)

        val result = api.getSubscriptions()

        assertEquals(2, result.size)
        assertEquals("Fiton", result.first().name)
        // Straight off the wire — unescaping is the mapper's job, not the api's.
        assertEquals("1&amp;1 Telekom", result[1].name)
        assertEquals("", result[1].startDate)
    }

    @Test
    fun `reads an empty list as an empty list`() = runTest {
        assertTrue(api(SubscriptionsJsonFixtures.NO_SUBSCRIPTIONS).getSubscriptions().isEmpty())
    }

    @Test
    fun `unwraps the singular envelope, which nests the row one level deeper`() = runTest {
        val result = api(SubscriptionsJsonFixtures.SUBSCRIPTION).getSubscription(id = 4)

        assertEquals(4, result.id)
        assertEquals("Fiton", result.name)
    }

    @Test
    fun `unwraps the currencies envelope`() = runTest {
        val result = api(SubscriptionsJsonFixtures.CURRENCIES).getCurrencies()

        assertEquals(2, result.size)
        assertEquals("€", result.first().symbol)
        assertEquals("USD", result[1].code)
    }

    @Test
    fun `hits the documented paths`() = runTest {
        val paths = mutableListOf<String>()
        val record: (HttpRequestData) -> Unit = { paths += it.url.encodedPath }

        api(SubscriptionsJsonFixtures.SUBSCRIPTIONS, record).getSubscriptions()
        api(SubscriptionsJsonFixtures.SUBSCRIPTION, record).getSubscription(id = 4)
        api(SubscriptionsJsonFixtures.CURRENCIES, record).getCurrencies()

        assertEquals(
            listOf(
                "/api/subscriptions/get_subscriptions.php",
                "/api/subscriptions/get_subscription.php",
                "/api/currencies/get_currencies.php"
            ),
            paths
        )
    }

    @Test
    fun `sends the id, and no filters at all on the list`() = runTest {
        var listBody: FormDataContent? = null
        var singleBody: FormDataContent? = null

        api(SubscriptionsJsonFixtures.SUBSCRIPTIONS) { listBody = it.body as FormDataContent }
            .getSubscriptions()
        api(SubscriptionsJsonFixtures.SUBSCRIPTION) { singleBody = it.body as FormDataContent }
            .getSubscription(id = 4)

        assertEquals("4", singleBody?.formData?.get("id"))
        // Nothing beyond the key: filtering and sorting are client-side, which is also what keeps
        // the §3.2 `all-user-subscription` + filter SQL bug unreachable.
        assertEquals(listOf("api_key"), listBody?.formData?.names()?.toList())
    }

    /** The 200-with-`success:false` case, on the one read endpoint that can produce it. */
    @Test
    fun `surfaces a missing subscription as a WallosError, not an empty result`() = runTest {
        val api = api(SubscriptionsJsonFixtures.SUBSCRIPTION_NOT_FOUND)

        assertFailsWith<WallosError> { api.getSubscription(id = 99999) }
    }

    @Test
    fun `survives the PHP warning display_errors prepends to a valid body`() = runTest {
        val api =
            api(
                "<b>Warning</b>: fopen failed in /var/www/html/api.php on line 3\n" +
                    SubscriptionsJsonFixtures.CURRENCIES
            )

        assertEquals(2, api.getCurrencies().size)
    }

    private fun api(responseBody: String, onRequest: (HttpRequestData) -> Unit = {}): SubscriptionsApi {
        val engine = MockEngine { request ->
            onRequest(request)
            respond(content = responseBody, status = HttpStatusCode.OK)
        }
        return SubscriptionsApiImpl(
            WallosApiClient(
                httpClient = HttpClient(engine),
                apiKeyStorage = FakeApiKeyStorage,
                envelopeParser = WallosEnvelopeParser()
            )
        )
    }

    private object FakeApiKeyStorage : ApiKeyStorage {
        override val isConnected: Flow<Boolean> = flowOf(true)

        override suspend fun getKey(): String = "s3cr3tk3y"

        override suspend fun setKey(key: String) = error("not used by this test")

        override suspend fun clear() = error("not used by this test")
    }
}

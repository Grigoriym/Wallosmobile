package com.grappim.wallosmobile.feature.subscriptions.data

import com.grappim.wallosmobile.core.api.FormParams
import com.grappim.wallosmobile.core.api.MultipartFile
import com.grappim.wallosmobile.core.api.WallosApiClient
import com.grappim.wallosmobile.core.api.WallosEnvelopeParser
import com.grappim.wallosmobile.core.domain.WallosError
import com.grappim.wallosmobile.core.storage.ApiKeyStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SubscriptionsApiImplTest {

    @Test
    fun `unwraps the subscriptions envelope`() = runTest {
        val api = api(SubscriptionsJsonFixtures.SUBSCRIPTIONS)

        val result = api.getSubscriptions(convertCurrency = false)

        assertEquals(2, result.size)
        assertEquals("Fiton", result.first().name)
        // Straight off the wire — unescaping is the mapper's job, not the api's.
        assertEquals("1&amp;1 Telekom", result[1].name)
        assertEquals("", result[1].startDate)
    }

    @Test
    fun `reads an empty list as an empty list`() = runTest {
        assertTrue(api(SubscriptionsJsonFixtures.NO_SUBSCRIPTIONS).getSubscriptions(convertCurrency = false).isEmpty())
    }

    @Test
    fun `unwraps the singular envelope, which nests the row one level deeper`() = runTest {
        val result = api(SubscriptionsJsonFixtures.SUBSCRIPTION).getSubscription(id = 4, convertCurrency = false)

        assertEquals(4, result.id)
        assertEquals("Fiton", result.name)
    }

    @Test
    fun `unwraps the currencies envelope`() = runTest {
        val result = api(SubscriptionsJsonFixtures.CURRENCIES).getCurrencies()

        assertEquals(2, result.currencies.size)
        assertEquals("€", result.currencies.first().symbol)
        assertEquals("USD", result.currencies[1].code)
        // The envelope's own field, and the only thing that says what a converted price is in.
        assertEquals(1, result.mainCurrencyId)
    }

    @Test
    fun `hits the documented paths`() = runTest {
        val paths = mutableListOf<String>()
        val record: (HttpRequestData) -> Unit = { paths += it.url.encodedPath }

        api(SubscriptionsJsonFixtures.SUBSCRIPTIONS, record).getSubscriptions(convertCurrency = false)
        api(SubscriptionsJsonFixtures.SUBSCRIPTION, record).getSubscription(id = 4, convertCurrency = false)
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
            .getSubscriptions(convertCurrency = false)
        api(SubscriptionsJsonFixtures.SUBSCRIPTION) { singleBody = it.body as FormDataContent }
            .getSubscription(id = 4, convertCurrency = false)

        assertEquals("4", singleBody?.formData?.get("id"))
        // Nothing beyond the key: filtering and sorting are client-side, which is also what keeps
        // the §3.2 `all-user-subscription` + filter SQL bug unreachable.
        assertEquals(listOf("api_key"), listBody?.formData?.names()?.toList())
    }

    /**
     * The literal string `"true"` — the server compares against it and reads anything else, `"1"`
     * included, as false (API doc §3.2).
     */
    @Test
    fun `asks for conversion with the literal string the server compares against`() = runTest {
        var listBody: FormDataContent? = null
        var singleBody: FormDataContent? = null

        api(SubscriptionsJsonFixtures.SUBSCRIPTIONS) { listBody = it.body as FormDataContent }
            .getSubscriptions(convertCurrency = true)
        api(SubscriptionsJsonFixtures.SUBSCRIPTION) { singleBody = it.body as FormDataContent }
            .getSubscription(id = 4, convertCurrency = true)

        assertEquals("true", listBody?.formData?.get("convert_currency"))
        assertEquals("true", singleBody?.formData?.get("convert_currency"))
    }

    /** Omitted rather than `"false"`: absence says the same thing in a shorter body (1.3). */
    @Test
    fun `leaves convert_currency out of the body entirely when it is off`() = runTest {
        var singleBody: FormDataContent? = null

        api(SubscriptionsJsonFixtures.SUBSCRIPTION) { singleBody = it.body as FormDataContent }
            .getSubscription(id = 4, convertCurrency = false)

        assertEquals(setOf("api_key", "id"), singleBody?.formData?.names())
    }

    @Test
    fun `reads convert_currency out of the settings envelope`() = runTest {
        var path: String? = null
        val api = api(SubscriptionsJsonFixtures.SETTINGS_CONVERT_ON) { path = it.url.encodedPath }

        assertTrue(api.isCurrencyConversionEnabled())
        assertEquals("/api/settings/get_settings.php", path)
    }

    @Test
    fun `reads the setting as off when the instance says zero`() = runTest {
        assertFalse(api(SubscriptionsJsonFixtures.SETTINGS_CONVERT_OFF).isCurrencyConversionEnabled())
    }

    /**
     * An instance old enough to have no `convert_currency` column at all. Off is the answer that
     * cannot mislabel a price, so the absent key must not read as on.
     */
    @Test
    fun `reads the setting as off when the instance has no such setting`() = runTest {
        assertFalse(api(SubscriptionsJsonFixtures.SETTINGS_WITHOUT_CONVERT).isCurrencyConversionEnabled())
    }

    /** The 200-with-`success:false` case, on the one read endpoint that can produce it. */
    @Test
    fun `surfaces a missing subscription as a WallosError, not an empty result`() = runTest {
        val api = api(SubscriptionsJsonFixtures.SUBSCRIPTION_NOT_FOUND)

        assertFailsWith<WallosError> { api.getSubscription(id = 99999, convertCurrency = false) }
    }

    @Test
    fun `survives the PHP warning display_errors prepends to a valid body`() = runTest {
        val api =
            api(
                "<b>Warning</b>: fopen failed in /var/www/html/api.php on line 3\n" +
                    SubscriptionsJsonFixtures.CURRENCIES
            )

        assertEquals(2, api.getCurrencies().currencies.size)
    }

    // --- 7.5: set_subscriptions.php ------------------------------------------------------------

    @Test
    fun `add sends action=add alongside the caller's own fields`() = runTest {
        var body: FormDataContent? = null

        api(SubscriptionsJsonFixtures.SUBSCRIPTION_ADDED) { body = it.body as FormDataContent }
            .addSubscription(FormParams().put("name", "Netflix"))

        assertEquals("add", body?.formData?.get("action"))
        assertEquals("Netflix", body?.formData?.get("name"))
    }

    @Test
    fun `add returns the id from under subscriptionId`() = runTest {
        val id = api(SubscriptionsJsonFixtures.SUBSCRIPTION_ADDED).addSubscription(FormParams())

        assertEquals(55, id)
    }

    @Test
    fun `edit sends action=edit and the id under the primary alias`() = runTest {
        var body: FormDataContent? = null

        api(SubscriptionsJsonFixtures.SUBSCRIPTION_UPDATED) { body = it.body as FormDataContent }
            .editSubscription(4, FormParams().put("name", "Renamed"))

        assertEquals("edit", body?.formData?.get("action"))
        assertEquals("4", body?.formData?.get("id"))
        assertEquals("Renamed", body?.formData?.get("name"))
    }

    @Test
    fun `delete sends action=delete and the id`() = runTest {
        var body: FormDataContent? = null

        api(SubscriptionsJsonFixtures.SUBSCRIPTION_DELETED) { body = it.body as FormDataContent }
            .deleteSubscription(4)

        assertEquals("delete", body?.formData?.get("action"))
        assertEquals("4", body?.formData?.get("id"))
    }

    /** The server-side `cycle` guard (API doc §3.4), on the one write endpoint that can trip it here. */
    @Test
    fun `a server-rejected write surfaces as a typed WallosError`() = runTest {
        assertFailsWith<WallosError.Validation> {
            api(SubscriptionsJsonFixtures.INVALID_CYCLE).addSubscription(FormParams())
        }
    }

    // --- 7.9: multipart logo upload ---------------------------------------------------------

    @OptIn(InternalAPI::class)
    @Test
    fun `add with a logo file switches to a multipart body carrying both`() = runTest {
        var body: MultiPartFormDataContent? = null

        api(SubscriptionsJsonFixtures.SUBSCRIPTION_ADDED) { body = it.body as MultiPartFormDataContent }
            .addSubscription(FormParams().put("name", "Netflix"), logoFile())

        val parts = body?.parts.orEmpty()
        assertEquals("add", parts.formValue("action"))
        assertEquals("Netflix", parts.formValue("name"))
        val filePart = parts.filterIsInstance<PartData.BinaryItem>().single()
        assertEquals("logo", filePart.name)
    }

    @OptIn(InternalAPI::class)
    @Test
    fun `edit with a logo file also switches to multipart, id included`() = runTest {
        var body: MultiPartFormDataContent? = null

        api(SubscriptionsJsonFixtures.SUBSCRIPTION_UPDATED) { body = it.body as MultiPartFormDataContent }
            .editSubscription(4, FormParams(), logoFile())

        val parts = body?.parts.orEmpty()
        assertEquals("edit", parts.formValue("action"))
        assertEquals("4", parts.formValue("id"))
    }

    /** No logo, no reason to switch: the urlencoded body stays the default (7.5's own shape). */
    @Test
    fun `add without a logo file keeps the plain urlencoded body`() = runTest {
        var body: FormDataContent? = null

        api(SubscriptionsJsonFixtures.SUBSCRIPTION_ADDED) { body = it.body as FormDataContent }
            .addSubscription(FormParams().put("name", "Netflix"))

        assertEquals("Netflix", body?.formData?.get("name"))
    }

    private fun logoFile() =
        MultipartFile(fieldName = "logo", fileName = "logo.png", mimeType = "image/png", bytes = byteArrayOf(1, 2, 3))

    private fun List<PartData>.formValue(name: String): String? =
        filterIsInstance<PartData.FormItem>().firstOrNull { it.name == name }?.value

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

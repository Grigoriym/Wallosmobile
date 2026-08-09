package com.grappim.wallosmobile.feature.currencies.data

import com.grappim.wallosmobile.core.api.FormParams
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CurrenciesApiImplTest {

    @Test
    fun `unwraps the currencies list and the main currency id`() = runTest {
        val result = api(GET_ALL_BODY).getAll()

        assertEquals(2, result.currencies.size)
        assertEquals("Euro", result.currencies.first().name)
        assertEquals("1.0000", result.currencies.first().rate)
        assertEquals(false, result.currencies.first().inUse)
        assertEquals(true, result.currencies[1].inUse)
        assertEquals(1, result.mainCurrencyId)
    }

    @Test
    fun `reads an empty list and a missing main currency`() = runTest {
        val result = api(EMPTY_BODY).getAll()

        assertTrue(result.currencies.isEmpty())
        assertNull(result.mainCurrencyId)
    }

    @Test
    fun `hits the documented paths`() = runTest {
        var getPath: String? = null
        var setPath: String? = null

        api(GET_ALL_BODY) { getPath = it.url.encodedPath }.getAll()
        api(ADD_BODY) { setPath = it.url.encodedPath }.add(FormParams().put("name", "New"))

        assertEquals("/api/currencies/get_currencies.php", getPath)
        assertEquals("/api/currencies/set_currencies.php", setPath)
    }

    @Test
    fun `add returns the id under the currencyId alias`() = runTest {
        val id = api(ADD_BODY).add(FormParams().put("name", "New"))

        assertEquals(5, id)
    }

    @Test
    fun `edit sends the id under the currencyId alias`() = runTest {
        var body: FormDataContent? = null

        api(OK_BODY) { body = it.body as FormDataContent }.edit(7, FormParams().put("rate", "1.5"))

        assertEquals("edit", body?.formData?.get("action"))
        assertEquals("7", body?.formData?.get("currencyId"))
        assertEquals("1.5", body?.formData?.get("rate"))
    }

    @Test
    fun `delete sends the id under the currencyId alias`() = runTest {
        var body: FormDataContent? = null

        api(OK_BODY) { body = it.body as FormDataContent }.delete(7)

        assertEquals("delete", body?.formData?.get("action"))
        assertEquals("7", body?.formData?.get("currencyId"))
    }

    @Test
    fun `surfaces the in-use delete failure as a typed WallosError`() = runTest {
        assertFailsWith<WallosError.InUse> { api(IN_USE_BODY).delete(7) }
    }

    private fun api(responseBody: String, onRequest: (HttpRequestData) -> Unit = {}): CurrenciesApi {
        val engine = MockEngine { request ->
            onRequest(request)
            respond(content = responseBody, status = HttpStatusCode.OK)
        }
        return CurrenciesApiImpl(
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

    private companion object {
        const val GET_ALL_BODY = """
            {"success":true,"title":"currencies","main_currency":1,"currencies":[
                {"id":1,"name":"Euro","symbol":"€","code":"EUR","rate":"1.0000","in_use":false},
                {"id":2,"name":"US Dollar","symbol":"$","code":"USD","rate":"1.1000","in_use":true}
            ]}
        """
        const val EMPTY_BODY = """{"success":true,"title":"currencies","currencies":[]}"""
        const val ADD_BODY = """{"success":true,"title":"currencies","currencyId":5}"""
        const val OK_BODY = """{"success":true,"title":"currencies"}"""
        const val IN_USE_BODY = """{"success":false,"title":"Currency in use","message":"Currency in use"}"""
    }
}

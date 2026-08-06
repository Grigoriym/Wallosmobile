package com.grappim.wallosmobile.feature.categories.data

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
import kotlin.test.assertTrue

class CategoriesApiImplTest {

    @Test
    fun `unwraps the categories list`() = runTest {
        val result = api(GET_ALL_BODY).getAll()

        assertEquals(2, result.size)
        assertEquals("Streaming", result.first().name)
        assertEquals(false, result.first().inUse)
        assertEquals(true, result[1].inUse)
    }

    @Test
    fun `reads an empty list as an empty list`() = runTest {
        assertTrue(api(EMPTY_BODY).getAll().isEmpty())
    }

    @Test
    fun `hits the documented paths`() = runTest {
        var getPath: String? = null
        var setPath: String? = null

        api(GET_ALL_BODY) { getPath = it.url.encodedPath }.getAll()
        api(ADD_BODY) { setPath = it.url.encodedPath }.add(FormParams().put("name", "New"))

        assertEquals("/api/categories/get_categories.php", getPath)
        assertEquals("/api/categories/set_categories.php", setPath)
    }

    @Test
    fun `add returns the id under the categoryId alias`() = runTest {
        val id = api(ADD_BODY).add(FormParams().put("name", "New"))

        assertEquals(5, id)
    }

    @Test
    fun `edit sends the id under the categoryId alias`() = runTest {
        var body: FormDataContent? = null

        api(OK_BODY) { body = it.body as FormDataContent }.edit(7, FormParams().put("name", "Renamed"))

        assertEquals("edit", body?.formData?.get("action"))
        assertEquals("7", body?.formData?.get("categoryId"))
        assertEquals("Renamed", body?.formData?.get("name"))
    }

    @Test
    fun `delete sends the id under the categoryId alias`() = runTest {
        var body: FormDataContent? = null

        api(OK_BODY) { body = it.body as FormDataContent }.delete(7)

        assertEquals("delete", body?.formData?.get("action"))
        assertEquals("7", body?.formData?.get("categoryId"))
    }

    @Test
    fun `surfaces the in-use delete failure as a typed WallosError`() = runTest {
        assertFailsWith<WallosError.InUse> { api(IN_USE_BODY).delete(7) }
    }

    private fun api(responseBody: String, onRequest: (HttpRequestData) -> Unit = {}): CategoriesApi {
        val engine = MockEngine { request ->
            onRequest(request)
            respond(content = responseBody, status = HttpStatusCode.OK)
        }
        return CategoriesApiImpl(
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
            {"success":true,"title":"categories","categories":[
                {"id":1,"name":"Streaming","order":1,"in_use":false},
                {"id":2,"name":"Utilities","order":2,"in_use":true}
            ]}
        """
        const val EMPTY_BODY = """{"success":true,"title":"categories","categories":[]}"""
        const val ADD_BODY = """{"success":true,"title":"categories","categoryId":5}"""
        const val OK_BODY = """{"success":true,"title":"categories"}"""
        const val IN_USE_BODY = """{"success":false,"title":"Category in use","message":"Category in use"}"""
    }
}

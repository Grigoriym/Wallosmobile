package com.grappim.wallosmobile.feature.household.data

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

class HouseholdApiImplTest {

    @Test
    fun `unwraps the household list`() = runTest {
        val result = api(GET_ALL_BODY).getAll()

        assertEquals(2, result.size)
        assertEquals("John Doe", result.first().name)
        assertEquals("john@example.com", result.first().email)
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

        assertEquals("/api/household/get_household.php", getPath)
        assertEquals("/api/household/set_household.php", setPath)
    }

    @Test
    fun `add returns the id under the memberId alias`() = runTest {
        val id = api(ADD_BODY).add(FormParams().put("name", "New"))

        assertEquals(5, id)
    }

    @Test
    fun `edit sends the id under the memberId alias`() = runTest {
        var body: FormDataContent? = null

        api(OK_BODY) { body = it.body as FormDataContent }
            .edit(7, FormParams().put("name", "Renamed").put("email", "renamed@example.com"))

        assertEquals("edit", body?.formData?.get("action"))
        assertEquals("7", body?.formData?.get("memberId"))
        assertEquals("Renamed", body?.formData?.get("name"))
        assertEquals("renamed@example.com", body?.formData?.get("email"))
    }

    @Test
    fun `delete sends the id under the memberId alias`() = runTest {
        var body: FormDataContent? = null

        api(OK_BODY) { body = it.body as FormDataContent }.delete(7)

        assertEquals("delete", body?.formData?.get("action"))
        assertEquals("7", body?.formData?.get("memberId"))
    }

    @Test
    fun `surfaces the in-use delete failure as a typed WallosError`() = runTest {
        assertFailsWith<WallosError.InUse> { api(IN_USE_BODY).delete(7) }
    }

    private fun api(responseBody: String, onRequest: (HttpRequestData) -> Unit = {}): HouseholdApi {
        val engine = MockEngine { request ->
            onRequest(request)
            respond(content = responseBody, status = HttpStatusCode.OK)
        }
        return HouseholdApiImpl(
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
            {"success":true,"title":"household","household":[
                {"id":1,"name":"John Doe","email":"john@example.com","in_use":false},
                {"id":2,"name":"Jane Doe","email":"jane@example.com","in_use":true}
            ]}
        """
        const val EMPTY_BODY = """{"success":true,"title":"household","household":[]}"""
        const val ADD_BODY = """{"success":true,"title":"Member added","memberId":5}"""
        const val OK_BODY = """{"success":true,"title":"Member updated"}"""
        const val IN_USE_BODY = """{"success":false,"title":"Household member in use","message":"in use"}"""
    }
}

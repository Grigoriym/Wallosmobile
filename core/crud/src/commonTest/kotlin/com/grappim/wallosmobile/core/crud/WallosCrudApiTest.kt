package com.grappim.wallosmobile.core.crud

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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Exercises [WallosCrudApi] with a resource type declared only here — nothing about categories,
 * household or payment methods, since none of them exist yet (7.2–7.4).
 */
class WallosCrudApiTest {

    @Test
    fun `unwraps the list from under the resource's own key`() = runTest {
        val result = api(GET_ALL_BODY).getAll()

        assertEquals(2, result.size)
        assertEquals("A", result.first().name)
        assertEquals(false, result.first().inUse)
        assertEquals(true, result[1].inUse)
    }

    @Test
    fun `add returns the id from under the resource's alias`() = runTest {
        val id = api(ADD_BODY).add(FormParams().put("name", "New"))

        assertEquals(5, id)
    }

    @Test
    fun `add sends action=add alongside the caller's own fields`() = runTest {
        var body: FormDataContent? = null

        api(ADD_BODY) { body = it.body as FormDataContent }.add(FormParams().put("name", "New"))

        assertEquals("add", body?.formData?.get("action"))
        assertEquals("New", body?.formData?.get("name"))
    }

    @Test
    fun `edit sends action=edit and the id under the resource's alias`() = runTest {
        var body: FormDataContent? = null

        api(OK_BODY) { body = it.body as FormDataContent }.edit(7, FormParams().put("name", "Renamed"))

        assertEquals("edit", body?.formData?.get("action"))
        assertEquals("7", body?.formData?.get(ENDPOINT.idParam))
        assertEquals("Renamed", body?.formData?.get("name"))
    }

    @Test
    fun `delete sends action=delete and the id under the resource's alias`() = runTest {
        var body: FormDataContent? = null

        api(OK_BODY) { body = it.body as FormDataContent }.delete(7)

        assertEquals("delete", body?.formData?.get("action"))
        assertEquals("7", body?.formData?.get(ENDPOINT.idParam))
    }

    @Test
    fun `surfaces the in-use delete failure as a typed WallosError`() = runTest {
        assertFailsWith<WallosError.InUse> { api(IN_USE_BODY).delete(7) }
    }

    private fun api(responseBody: String, onRequest: (HttpRequestData) -> Unit = {}): WallosCrudApi<FakeCrudResource> {
        val engine = MockEngine { request ->
            onRequest(request)
            respond(content = responseBody, status = HttpStatusCode.OK)
        }
        return WallosCrudApi(
            apiClient = WallosApiClient(
                httpClient = HttpClient(engine),
                apiKeyStorage = FakeApiKeyStorage,
                envelopeParser = WallosEnvelopeParser()
            ),
            endpoint = ENDPOINT,
            itemDeserializer = FakeCrudResource.serializer()
        )
    }

    @Serializable
    private data class FakeCrudResource(
        override val id: Int,
        override val name: String,
        @SerialName("in_use") override val inUse: Boolean
    ) : CrudResource

    private object FakeApiKeyStorage : ApiKeyStorage {
        override val isConnected: Flow<Boolean> = flowOf(true)

        override suspend fun getKey(): String = "s3cr3tk3y"

        override suspend fun setKey(key: String) = error("not used by this test")

        override suspend fun clear() = error("not used by this test")
    }

    private companion object {
        val ENDPOINT = CrudEndpoint(
            getPath = "api/fake/get_fake.php",
            setPath = "api/fake/set_fake.php",
            listKey = "items",
            idParam = "fakeId"
        )

        const val GET_ALL_BODY = """
            {"success":true,"title":"fake","items":[
                {"id":1,"name":"A","in_use":false},
                {"id":2,"name":"B","in_use":true}
            ]}
        """

        const val ADD_BODY = """{"success":true,"title":"fake","fakeId":5}"""
        const val OK_BODY = """{"success":true,"title":"fake"}"""
        const val IN_USE_BODY = """{"success":false,"title":"Fake in use","message":"Fake in use"}"""
    }
}

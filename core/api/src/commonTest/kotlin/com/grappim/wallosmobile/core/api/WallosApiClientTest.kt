package com.grappim.wallosmobile.core.api

import com.grappim.wallosmobile.core.domain.WallosError
import com.grappim.wallosmobile.core.storage.ApiKeyStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WallosApiClientTest {

    @Test
    fun `injects the stored key alongside the caller's own parameters`() = runTest {
        var body: FormDataContent? = null
        val client = apiClient(apiKey = "s3cr3tk3y", onRequest = { body = it.body as FormDataContent })

        client.post<VersionEnvelope>(PATH, FormParams().put("sort", "name"))

        assertEquals("s3cr3tk3y", body?.formData?.get("api_key"))
        assertEquals("name", body?.formData?.get("sort"))
    }

    /**
     * Nothing stored means the user isn't connected yet. Omitting the parameter lets the server
     * answer `Missing API key`, which routes back to setup — sending an empty one would not.
     */
    @Test
    fun `omits the key entirely when none is stored`() = runTest {
        var body: FormDataContent? = null
        val client = apiClient(apiKey = null, onRequest = { body = it.body as FormDataContent })

        client.post<VersionEnvelope>(PATH)

        assertNull(body?.formData?.get("api_key"))
    }

    @Test
    fun `posts every call, so the key never lands in an access log or a referrer`() = runTest {
        var method: HttpMethod? = null
        val client = apiClient(apiKey = "s3cr3tk3y", onRequest = { method = it.method })

        client.post<VersionEnvelope>(PATH)

        assertEquals(HttpMethod.Post, method)
    }

    @Test
    fun `decodes a successful envelope`() = runTest {
        val client = apiClient(apiKey = "s3cr3tk3y")

        val version = client.post<VersionEnvelope>(PATH)

        assertEquals("3.1.0", version.version)
    }

    /** The parse pipeline is wired in: a `200` carrying `success: false` still fails the call. */
    @Test
    fun `surfaces an api-level failure as a WallosError despite the 200`() = runTest {
        val client = apiClient(
            apiKey = "stale",
            responseBody = """{"success":false,"title":"Invalid API key"}"""
        )

        assertFailsWith<WallosError.Unauthenticated> { client.post<VersionEnvelope>(PATH) }
    }

    // --- 7.9: multipart -------------------------------------------------------------------------

    @OptIn(InternalAPI::class)
    @Test
    fun `postMultipart sends the file alongside the caller's own fields, plus the stored key`() = runTest {
        var body: MultiPartFormDataContent? = null
        val client = apiClient(apiKey = "s3cr3tk3y", onRequest = { body = it.body as MultiPartFormDataContent })

        client.postMultipart<VersionEnvelope>(
            PATH,
            FormParams().put("action", "add"),
            MultipartFile(
                fieldName = "logo",
                fileName = "logo.png",
                mimeType = "image/png",
                bytes = byteArrayOf(1, 2, 3)
            )
        )

        val parts = body?.parts.orEmpty()
        val fields = parts.filterIsInstance<PartData.FormItem>().associate { it.name to it.value }
        assertEquals("s3cr3tk3y", fields["api_key"])
        assertEquals("add", fields["action"])

        val filePart = parts.filterIsInstance<PartData.BinaryItem>().single()
        assertEquals("logo", filePart.name)
        assertEquals(ContentType.Image.PNG, filePart.contentType)
        // Two `Content-Disposition` values land on the same header (`name=` from `formData {}`
        // itself, `filename=` from the headers this call passed in) — `getAll`, not the `[]`
        // accessor, which only ever returns the first.
        val disposition = filePart.headers.getAll(HttpHeaders.ContentDisposition).orEmpty()
        assertTrue(disposition.any { it.contains("filename=\"logo.png\"") })
    }

    @Test
    fun `postMultipart decodes the same envelope shape as post`() = runTest {
        val client = apiClient(apiKey = "s3cr3tk3y")

        val version = client.postMultipart<VersionEnvelope>(
            PATH,
            FormParams(),
            MultipartFile(fieldName = "logo", fileName = "logo.png", mimeType = "image/png", bytes = byteArrayOf())
        )

        assertEquals("3.1.0", version.version)
    }

    private fun apiClient(
        apiKey: String?,
        responseBody: String = SUCCESS_BODY,
        status: HttpStatusCode = HttpStatusCode.OK,
        onRequest: (HttpRequestData) -> Unit = {}
    ): WallosApiClient {
        val engine = MockEngine { request ->
            onRequest(request)
            respond(content = responseBody, status = status)
        }
        return WallosApiClient(
            httpClient = HttpClient(engine),
            apiKeyStorage = FakeApiKeyStorage(apiKey),
            envelopeParser = WallosEnvelopeParser()
        )
    }

    @Serializable
    private data class VersionEnvelope(val version: String)

    private class FakeApiKeyStorage(private val key: String?) : ApiKeyStorage {
        override val isConnected: Flow<Boolean> = flowOf(key != null)

        override suspend fun getKey(): String? = key

        override suspend fun setKey(key: String) = error("not used by this test")

        override suspend fun clear() = error("not used by this test")
    }

    private companion object {
        const val PATH = "api/status/version.php"
        const val SUCCESS_BODY = """{"success":true,"title":"version","version":"3.1.0"}"""
    }
}

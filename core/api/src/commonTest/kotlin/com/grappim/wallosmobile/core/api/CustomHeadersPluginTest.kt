package com.grappim.wallosmobile.core.api

import com.grappim.wallosmobile.core.storage.customheaders.CustomHeader
import com.grappim.wallosmobile.testing.FakeCustomHeadersStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CustomHeadersPluginTest {

    private val storage = FakeCustomHeadersStorage(listOf(CustomHeader(NAME, VALUE)))
    private val requests = mutableListOf<HttpRequestData>()

    @Test
    fun `sends the headers on a request to the server`() = runTest {
        client().get("api/status/version.php")

        assertEquals(VALUE, requests.single().headers[NAME])
    }

    @Test
    fun `sends nothing to another host`() = runTest {
        client().get("https://logos.example.org/netflix.png")

        assertNull(requests.single().headers[NAME])
    }

    /** A non-standard name on purpose: Ktor's own redirect handling already strips `Authorization`. */
    @Test
    fun `drops the headers when a redirect leaves the server`() = runTest {
        client(
            respondWith = { request ->
                if (request.url.host == SERVER_HOST) {
                    respond(
                        content = "",
                        status = HttpStatusCode.Found,
                        headers = headersOf(HttpHeaders.Location, "https://proxy-login.example.org/")
                    )
                } else {
                    respondOk()
                }
            }
        ).get("api/status/version.php")

        assertEquals(VALUE, requests[0].headers[NAME])
        assertNull(requests[1].headers[NAME])
    }

    @Test
    fun `reads the stored list per request, so an edit applies to the next one`() = runTest {
        val client = client()
        storage.setHeaders(listOf(CustomHeader(NAME, "Basic changed")))

        client.get("api/status/version.php")

        assertEquals("Basic changed", requests.single().headers[NAME])
    }

    @Test
    fun `logs the header name but never its value`() = runTest {
        val lines = mutableListOf<String>()
        client(
            logger = object : Logger {
                override fun log(message: String) {
                    lines += message
                }
            }
        ).get("api/status/version.php")

        val log = lines.joinToString("\n")
        assertTrue(log.contains(NAME, ignoreCase = true), log)
        assertFalse(log.contains(VALUE), log)
    }

    @Test
    fun `matches the server's scheme, host and port, not just its host`() {
        assertTrue(isServerOrigin(Url("https://wallos.example.com/api/x.php"), SERVER_URL))
        assertTrue(isServerOrigin(Url("https://WALLOS.example.com:443/x"), SERVER_URL))
        assertFalse(isServerOrigin(Url("http://wallos.example.com/x"), SERVER_URL))
        assertFalse(isServerOrigin(Url("https://wallos.example.com:8443/x"), SERVER_URL))
        assertFalse(isServerOrigin(Url("https://wallos.example.com.evil.org/x"), SERVER_URL))
    }

    @Test
    fun `matches nothing while no server is stored`() {
        assertFalse(isServerOrigin(Url("https://wallos.example.com/x"), ""))
    }

    private fun client(
        respondWith: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = { respondOk() },
        logger: Logger? = null
    ): HttpClient = HttpClient(
        MockEngine { request ->
            requests += request
            respondWith(request)
        }
    ) {
        defaultRequest { url(SERVER_URL) }
        install(CustomHeaders) {
            storage = this@CustomHeadersPluginTest.storage
            serverUrl = { SERVER_URL }
        }
        if (logger != null) {
            install(Logging) {
                this.logger = logger
                level = LogLevel.ALL
                sanitizeCustomHeaders(this@CustomHeadersPluginTest.storage)
            }
        }
    }

    private companion object {
        const val SERVER_HOST = "wallos.example.com"
        const val SERVER_URL = "https://$SERVER_HOST/"
        const val NAME = "CF-Access-Client-Secret"
        const val VALUE = "s3cr3t-proxy-token"
    }
}

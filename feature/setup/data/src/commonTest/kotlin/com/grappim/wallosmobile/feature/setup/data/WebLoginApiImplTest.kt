package com.grappim.wallosmobile.feature.setup.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.forms.FormDataContent
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WebLoginApiImplTest {

    @Test
    fun `posts the credentials as a form to login php`() = runTest {
        var request: HttpRequestData? = null
        val api = webLoginApi(status = HttpStatusCode.Found, location = ".", onRequest = { request = it })

        api.login(username = "demo", password = "demo")

        assertEquals(HttpMethod.Post, request?.method)
        assertEquals("/login.php", request?.url?.encodedPath)
        val form = (request?.body as FormDataContent).formData
        assertEquals("demo", form["username"])
        assertEquals("demo", form["password"])
    }

    /** The session should live no longer than it has to, so nothing asks for a persistent cookie. */
    @Test
    fun `does not ask to be remembered`() = runTest {
        var request: HttpRequestData? = null
        val api = webLoginApi(status = HttpStatusCode.Found, location = ".", onRequest = { request = it })

        api.login(username = "demo", password = "demo")

        assertNull((request?.body as FormDataContent).formData["remember"])
    }

    @Test
    fun `reads the redirect rather than following it`() = runTest {
        val loggedIn = webLoginApi(status = HttpStatusCode.Found, location = ".")
        val totp = webLoginApi(status = HttpStatusCode.Found, location = "totp.php")
        val rejected = webLoginApi(status = HttpStatusCode.OK, body = LOGIN_HTML)

        assertEquals(WebLoginOutcome.LoggedIn, loggedIn.login("demo", "demo"))
        assertEquals(WebLoginOutcome.NeedsTotp, totp.login("demo", "demo"))
        assertEquals(WebLoginOutcome.InvalidCredentials, rejected.login("demo", "wrong"))
    }

    @Test
    fun `scrapes the key off profile php`() = runTest {
        var request: HttpRequestData? = null
        val api = webLoginApi(body = PROFILE_HTML, onRequest = { request = it })

        assertEquals(SCRAPED_API_KEY, api.fetchApiKey())
        assertEquals(HttpMethod.Get, request?.method)
        assertEquals("/profile.php", request?.url?.encodedPath)
    }

    @Test
    fun `returns no key when the profile page carries none`() = runTest {
        val api = webLoginApi(body = LOGIN_HTML)

        assertNull(api.fetchApiKey())
    }

    private fun webLoginApi(
        status: HttpStatusCode = HttpStatusCode.OK,
        body: String = "",
        location: String? = null,
        onRequest: (HttpRequestData) -> Unit = {}
    ): WebLoginApi {
        val engine = MockEngine { request ->
            onRequest(request)
            respond(
                content = body,
                status = status,
                headers = location?.let { headersOf(HttpHeaders.Location, it) } ?: Headers.Empty
            )
        }
        // Mirrors the part of the `@WebSessionHttpClient` config this class depends on: with
        // redirects followed, Ktor chases the 302 and the outcome table can never be observed.
        return WebLoginApiImpl(
            HttpClient(engine) {
                followRedirects = false
                expectSuccess = false
            }
        )
    }
}

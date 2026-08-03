package com.grappim.wallosmobile.feature.setup.data

import com.grappim.wallosmobile.core.api.WallosApiClient
import com.grappim.wallosmobile.core.api.WallosEnvelopeParser
import com.grappim.wallosmobile.core.domain.PendingCertTrust
import com.grappim.wallosmobile.core.domain.WallosError
import com.grappim.wallosmobile.core.storage.ApiKeyStorage
import com.grappim.wallosmobile.core.storage.ServerUrlStorage
import com.grappim.wallosmobile.feature.setup.domain.model.ApiKeyNotFound
import com.grappim.wallosmobile.feature.setup.domain.model.LoginOutcome
import com.grappim.wallosmobile.testing.FakeTrustedCertStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.forms.FormDataContent
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SetupRepositoryImplTest {

    private val webLoginApi = FakeWebLoginApi()
    private val apiKeyStorage = FakeApiKeyStorage()
    private val serverUrlStorage = FakeServerUrlStorage()
    private val trustedCertStorage = FakeTrustedCertStorage()
    private var validationBody: String = VERSION_SUCCESS
    private val validationRequests = mutableListOf<Map<String, String>>()

    @Test
    fun `stores the scraped key once the instance has accepted it`() = runTest {
        webLoginApi.loginOutcome = WebLoginOutcome.LoggedIn
        webLoginApi.apiKey = SCRAPED_API_KEY

        val result = repository().loginWithPassword(SERVER_URL, "demo", "demo")

        assertEquals(LoginOutcome.Connected, result.getOrNull())
        assertEquals(SCRAPED_API_KEY, apiKeyStorage.key)
    }

    /** Every later call resolves against it, so it has to be persisted before the first one. */
    @Test
    fun `persists the trimmed server url before talking to the instance`() = runTest {
        webLoginApi.loginOutcome = WebLoginOutcome.LoggedIn
        webLoginApi.apiKey = SCRAPED_API_KEY

        repository().loginWithPassword("  $SERVER_URL  ", "demo", "demo")

        assertEquals(SERVER_URL, serverUrlStorage.serverUrl)
    }

    /** The point of validating: prove the scraped string works before it becomes the credential. */
    @Test
    fun `validates the scraped key, not the one already in storage`() = runTest {
        apiKeyStorage.key = "stale-key"
        webLoginApi.loginOutcome = WebLoginOutcome.LoggedIn
        webLoginApi.apiKey = SCRAPED_API_KEY

        repository().loginWithPassword(SERVER_URL, "demo", "demo")

        assertEquals(listOf(SCRAPED_API_KEY), validationRequests.map { it["api_key"] })
    }

    @Test
    fun `reports a rejected credential without storing anything`() = runTest {
        webLoginApi.loginOutcome = WebLoginOutcome.InvalidCredentials

        val result = repository().loginWithPassword(SERVER_URL, "demo", "wrong")

        assertEquals(LoginOutcome.InvalidCredentials, result.getOrNull())
        assertNull(apiKeyStorage.key)
        assertTrue(validationRequests.isEmpty())
    }

    /** v1 stops here: `totp.php` is never driven, the UI points at manual key entry instead. */
    @Test
    fun `stops at a totp challenge`() = runTest {
        webLoginApi.loginOutcome = WebLoginOutcome.NeedsTotp

        val result = repository().loginWithPassword(SERVER_URL, "demo", "demo")

        assertEquals(LoginOutcome.NeedsTotp, result.getOrNull())
        assertNull(apiKeyStorage.key)
    }

    /**
     * A stale key must not survive a fresh attempt: `WallosApiClient` injects the *stored* key over
     * whatever a caller passes, so leaving it would mean validating the wrong string.
     */
    @Test
    fun `drops a previously stored key even when the attempt fails`() = runTest {
        apiKeyStorage.key = "stale-key"
        webLoginApi.loginOutcome = WebLoginOutcome.InvalidCredentials

        repository().loginWithPassword(SERVER_URL, "demo", "wrong")

        assertNull(apiKeyStorage.key)
    }

    /** The markup moved upstream — the recovery route is manual key entry, so name the failure. */
    @Test
    fun `fails with ApiKeyNotFound when the profile page carried no key`() = runTest {
        webLoginApi.loginOutcome = WebLoginOutcome.LoggedIn
        webLoginApi.apiKey = null

        val result = repository().loginWithPassword(SERVER_URL, "demo", "demo")

        assertIs<ApiKeyNotFound>(result.exceptionOrNull())
        assertNull(apiKeyStorage.key)
    }

    @Test
    fun `does not store a key the instance rejects`() = runTest {
        validationBody = """{"success":false,"title":"Invalid API key"}"""
        webLoginApi.loginOutcome = WebLoginOutcome.LoggedIn
        webLoginApi.apiKey = SCRAPED_API_KEY

        val result = repository().loginWithPassword(SERVER_URL, "demo", "demo")

        assertIs<WallosError.Unauthenticated>(result.exceptionOrNull())
        assertNull(apiKeyStorage.key)
    }

    /** Path B is the tail of the bridge: no web session, same validate-then-store. */
    @Test
    fun `stores a pasted key the instance accepts, trimmed`() = runTest {
        val result = repository().connectWithApiKey("  $SERVER_URL  ", "  $PASTED_API_KEY  ")

        assertTrue(result.isSuccess)
        assertEquals(SERVER_URL, serverUrlStorage.serverUrl)
        assertEquals(PASTED_API_KEY, apiKeyStorage.key)
        assertEquals(listOf(PASTED_API_KEY), validationRequests.map { it["api_key"] })
    }

    /** Same trap as the bridge: the injected stored key would be the one actually validated. */
    @Test
    fun `validates the pasted key, not the one already in storage`() = runTest {
        apiKeyStorage.key = "stale-key"

        repository().connectWithApiKey(SERVER_URL, PASTED_API_KEY)

        assertEquals(listOf(PASTED_API_KEY), validationRequests.map { it["api_key"] })
    }

    @Test
    fun `does not store a pasted key the instance rejects`() = runTest {
        validationBody = """{"success":false,"title":"Invalid API key"}"""

        val result = repository().connectWithApiKey(SERVER_URL, PASTED_API_KEY)

        assertIs<WallosError.Unauthenticated>(result.exceptionOrNull())
        assertNull(apiKeyStorage.key)
    }

    /** The web login is never touched on this path — that is the whole point of the fallback. */
    @Test
    fun `never drives the web login`() = runTest {
        repository().connectWithApiKey(SERVER_URL, PASTED_API_KEY)

        assertFalse(webLoginApi.loginCalled)
    }

    /** What makes a re-login after Disconnect one field instead of two (plan §4.7). */
    @Test
    fun `offers back the server url a previous connection stored`() = runTest {
        serverUrlStorage.serverUrl = SERVER_URL

        assertEquals(SERVER_URL, repository().getStoredServerUrl().getOrNull())
    }

    @Test
    fun `has no server url to offer on a fresh install`() = runTest {
        assertEquals("", repository().getStoredServerUrl().getOrNull())
    }

    @Test
    fun `pins an accepted certificate for the host it was shown for`() = runTest {
        repository().trustCertificate(pendingCertTrust())

        assertTrue(trustedCertStorage.isTrusted(CERT_HOST, CERT_FINGERPRINT))
    }

    /** The pin is scoped: the same bytes from a different server are still untrusted. */
    @Test
    fun `does not pin the certificate for any other host`() = runTest {
        repository().trustCertificate(pendingCertTrust())

        assertFalse(trustedCertStorage.isTrusted("other.lan", CERT_FINGERPRINT))
    }

    private fun pendingCertTrust() = PendingCertTrust(
        host = CERT_HOST,
        subject = "CN=$CERT_HOST",
        issuer = "CN=$CERT_HOST",
        notBefore = "2026-01-04",
        notAfter = "2027-01-04",
        sha256Fingerprint = CERT_FINGERPRINT
    )

    private fun repository(): SetupRepositoryImpl {
        val engine = MockEngine { request ->
            validationRequests += (request.body as FormDataContent).formData.entries()
                .associate { (key, values) -> key to values.first() }
            respond(content = validationBody, status = HttpStatusCode.OK)
        }
        return SetupRepositoryImpl(
            webLoginApi = webLoginApi,
            wallosApiClient = WallosApiClient(
                httpClient = HttpClient(engine),
                apiKeyStorage = apiKeyStorage,
                envelopeParser = WallosEnvelopeParser()
            ),
            serverUrlStorage = serverUrlStorage,
            apiKeyStorage = apiKeyStorage,
            trustedCertStorage = trustedCertStorage,
            dispatcher = UnconfinedTestDispatcher()
        )
    }

    private class FakeWebLoginApi : WebLoginApi {
        var loginOutcome: WebLoginOutcome? = null
        var apiKey: String? = null
        var loginCalled = false

        override suspend fun login(username: String, password: String): WebLoginOutcome {
            loginCalled = true
            return loginOutcome ?: error("loginOutcome not set")
        }

        override suspend fun fetchApiKey(): String? = apiKey
    }

    private class FakeApiKeyStorage : ApiKeyStorage {
        var key: String? = null

        override val isConnected: Flow<Boolean> get() = flowOf(key != null)

        override suspend fun getKey(): String? = key

        override suspend fun setKey(key: String) {
            this.key = key
        }

        override suspend fun clear() {
            key = null
        }
    }

    private class FakeServerUrlStorage : ServerUrlStorage {
        override var serverUrl: String = ""

        override suspend fun saveServerUrl(url: String) {
            serverUrl = url
        }
    }

    private companion object {
        const val SERVER_URL = "https://wallos.example.com"
        const val PASTED_API_KEY = "pasted-api-key"
        const val CERT_HOST = "wallos.lan"
        const val CERT_FINGERPRINT = "3A:7B:1C:04"
        const val VERSION_SUCCESS = """{"success":true,"title":"version","version":"3.1.0"}"""
    }
}

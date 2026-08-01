package com.grappim.wallosmobile.feature.setup.data

import com.grappim.wallosmobile.core.api.WallosApiClient
import com.grappim.wallosmobile.core.api.WallosEnvelopeParser
import com.grappim.wallosmobile.core.domain.WallosError
import com.grappim.wallosmobile.core.storage.ApiKeyStorage
import com.grappim.wallosmobile.core.storage.ServerUrlStorage
import com.grappim.wallosmobile.feature.setup.domain.model.ApiKeyNotFound
import com.grappim.wallosmobile.feature.setup.domain.model.LoginOutcome
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
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SetupRepositoryImplTest {

    private val webLoginApi = FakeWebLoginApi()
    private val apiKeyStorage = FakeApiKeyStorage()
    private val serverUrlStorage = FakeServerUrlStorage()
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
            dispatcher = UnconfinedTestDispatcher()
        )
    }

    private class FakeWebLoginApi : WebLoginApi {
        var loginOutcome: WebLoginOutcome? = null
        var apiKey: String? = null

        override suspend fun login(username: String, password: String): WebLoginOutcome =
            loginOutcome ?: error("loginOutcome not set")

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
        const val VERSION_SUCCESS = """{"success":true,"title":"version","version":"3.1.0"}"""
    }
}

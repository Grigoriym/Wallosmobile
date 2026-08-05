package com.grappim.wallosmobile.feature.setup.data

import com.grappim.wallosmobile.core.api.WallosApiClient
import com.grappim.wallosmobile.core.api.WallosEnvelopeParser
import com.grappim.wallosmobile.core.domain.PendingCertTrust
import com.grappim.wallosmobile.core.domain.WallosError
import com.grappim.wallosmobile.core.storage.ApiKeyStorage
import com.grappim.wallosmobile.core.storage.ServerUrlStorage
import com.grappim.wallosmobile.feature.setup.domain.model.ApiKeyNotFound
import com.grappim.wallosmobile.feature.setup.domain.model.LoginOutcome
import com.grappim.wallosmobile.feature.setup.domain.model.PasswordLoginAvailability
import com.grappim.wallosmobile.testing.FakeTrustedCertStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.forms.FormDataContent
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

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

    /** Nothing is stored on the challenge itself — the session is what carries it forward. */
    @Test
    fun `reports a totp challenge without storing anything`() = runTest {
        webLoginApi.loginOutcome = WebLoginOutcome.NeedsTotp

        val result = repository().loginWithPassword(SERVER_URL, "demo", "demo")

        assertEquals(LoginOutcome.NeedsTotp, result.getOrNull())
        assertNull(apiKeyStorage.key)
        assertTrue(validationRequests.isEmpty())
    }

    /** The second factor finishes the same bridge: scrape, validate, store. */
    @Test
    fun `an accepted code completes the bridge`() = runTest {
        webLoginApi.loginOutcome = WebLoginOutcome.NeedsTotp
        webLoginApi.totpOutcome = WebTotpOutcome.LoggedIn
        webLoginApi.apiKey = SCRAPED_API_KEY
        val repository = repository()
        repository.loginWithPassword(SERVER_URL, "demo", "demo")

        val result = repository.submitTotpCode("  123456  ")

        assertEquals(LoginOutcome.Connected, result.getOrNull())
        assertEquals(SCRAPED_API_KEY, apiKeyStorage.key)
        assertEquals(listOf(SCRAPED_API_KEY), validationRequests.map { it["api_key"] })
        assertEquals("123456", webLoginApi.totpCall)
    }

    /** The challenge stands, so this is an answer and not a failure — the next code can work. */
    @Test
    fun `a rejected code is an outcome, not a failure`() = runTest {
        webLoginApi.totpOutcome = WebTotpOutcome.InvalidCode

        val result = repository().submitTotpCode("000000")

        assertEquals(LoginOutcome.InvalidTotpCode, result.getOrNull())
        assertNull(apiKeyStorage.key)
    }

    /** A lost session cannot accept any code — reported as a bad one, it would loop forever. */
    @Test
    fun `a lost session is told apart from a bad code`() = runTest {
        webLoginApi.totpOutcome = WebTotpOutcome.SessionExpired

        val result = repository().submitTotpCode("123456")

        assertEquals(LoginOutcome.TotpSessionExpired, result.getOrNull())
        assertNull(apiKeyStorage.key)
    }

    /** The key already went in [loginWithPassword]; a second clear would drop what was just stored. */
    @Test
    fun `the second factor does not touch the stored url or key on its way in`() = runTest {
        webLoginApi.totpOutcome = WebTotpOutcome.InvalidCode
        apiKeyStorage.key = "stale-key"
        serverUrlStorage.serverUrl = SERVER_URL

        repository().submitTotpCode("000000")

        assertEquals("stale-key", apiKeyStorage.key)
        assertEquals(SERVER_URL, serverUrlStorage.serverUrl)
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
    fun `reports what the login form says about password login`() = runTest {
        webLoginApi.availability = PasswordLoginAvailability.Disabled

        val result = repository().probePasswordLogin(SERVER_URL)

        assertEquals(PasswordLoginAvailability.Disabled, result.getOrNull())
    }

    /** `login.php` is requested relative to it, which is what keeps a subpath install working. */
    @Test
    fun `persists the trimmed server url before probing it`() = runTest {
        repository().probePasswordLogin("  $SERVER_URL  ")

        assertEquals(SERVER_URL, serverUrlStorage.serverUrl)
    }

    /** It is an affordance, not a login — nothing about the stored credential is its business. */
    @Test
    fun `the probe leaves the stored key alone`() = runTest {
        apiKeyStorage.key = "stale-key"

        repository().probePasswordLogin(SERVER_URL)

        assertEquals("stale-key", apiKeyStorage.key)
    }

    /**
     * The server counts no failed attempts of its own (plan §9), so without this the screen is a
     * brute-force tool with a Connect button.
     */
    @Test
    fun `slows down once the instance has refused enough credentials`() = runTest {
        webLoginApi.loginOutcome = WebLoginOutcome.InvalidCredentials
        val repository = repository()
        repeat(3) { repository.loginWithPassword(SERVER_URL, "demo", "wrong") }

        val before = currentTime
        repository.loginWithPassword(SERVER_URL, "demo", "wrong")

        assertTrue(currentTime > before)
    }

    /**
     * The wait is spent in here, so the screen cannot infer it — a login that got slower is all it
     * would otherwise have to go on (5.5). Only the held-back attempts announce anything.
     */
    @Test
    fun `the backoff it imposes is announced to the caller`() = runTest {
        webLoginApi.loginOutcome = WebLoginOutcome.InvalidCredentials
        val repository = repository()
        val announced = mutableListOf<Duration>()

        repeat(5) { repository.loginWithPassword(SERVER_URL, "demo", "wrong") { announced += it } }

        assertEquals(listOf(1.seconds, 2.seconds), announced)
    }

    /** A wide verification window on a server that counts nothing — the sharper of the two. */
    @Test
    fun `rejected codes count towards the same backoff`() = runTest {
        webLoginApi.totpOutcome = WebTotpOutcome.InvalidCode
        val repository = repository()
        repeat(3) { repository.submitTotpCode("000000") }

        val before = currentTime
        var announced: Duration? = null
        repository.submitTotpCode("000000") { announced = it }

        assertTrue(currentTime > before)
        assertEquals(1.seconds, announced)
    }

    /** A typo followed by the right password must not leave the next login slow. */
    @Test
    fun `a completed connection clears the backoff`() = runTest {
        webLoginApi.loginOutcome = WebLoginOutcome.InvalidCredentials
        val repository = repository()
        repeat(3) { repository.loginWithPassword(SERVER_URL, "demo", "wrong") }
        webLoginApi.apiKey = SCRAPED_API_KEY
        webLoginApi.loginOutcome = WebLoginOutcome.LoggedIn
        repository.loginWithPassword(SERVER_URL, "demo", "demo")

        webLoginApi.loginOutcome = WebLoginOutcome.InvalidCredentials
        val before = currentTime
        repository.loginWithPassword(SERVER_URL, "demo", "wrong")

        assertEquals(before, currentTime)
    }

    /** Not a guess: no code was weighed, so nothing was learned and there is nothing to slow. */
    @Test
    fun `a lost session does not count towards the backoff`() = runTest {
        webLoginApi.totpOutcome = WebTotpOutcome.SessionExpired
        val repository = repository()
        repeat(6) { repository.submitTotpCode("123456") }

        val before = currentTime
        repository.submitTotpCode("123456")

        assertEquals(before, currentTime)
    }

    /** Nobody arrives at a pasted key by retrying, and the recovery route must not get slower. */
    @Test
    fun `a rejected api key is never throttled`() = runTest {
        validationBody = """{"success":false,"title":"Invalid API key"}"""
        val repository = repository()
        repeat(6) { repository.connectWithApiKey(SERVER_URL, PASTED_API_KEY) }

        val before = currentTime
        repository.connectWithApiKey(SERVER_URL, PASTED_API_KEY)

        assertEquals(before, currentTime)
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

    /**
     * A `TestScope` extension so the dispatcher can share `runTest`'s scheduler: the throttle's
     * `delay` runs inside the repository's `withContext`, and a dispatcher built with its own
     * scheduler would leave that wait invisible to `currentTime` — or hang on it.
     */
    private fun TestScope.repository(): SetupRepositoryImpl {
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
            dispatcher = UnconfinedTestDispatcher(testScheduler)
        )
    }

    private class FakeWebLoginApi : WebLoginApi {
        var loginOutcome: WebLoginOutcome? = null
        var totpOutcome: WebTotpOutcome? = null
        var apiKey: String? = null
        var loginCalled = false
        var totpCall: String? = null
        var availability = PasswordLoginAvailability.Available

        override suspend fun probePasswordLogin(): PasswordLoginAvailability = availability

        override suspend fun login(username: String, password: String): WebLoginOutcome {
            loginCalled = true
            return loginOutcome ?: error("loginOutcome not set")
        }

        override suspend fun submitTotpCode(code: String): WebTotpOutcome {
            totpCall = code
            return totpOutcome ?: error("totpOutcome not set")
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

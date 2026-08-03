package com.grappim.wallosmobile.feature.setup.ui

import com.grappim.wallosmobile.core.domain.PendingCertTrust
import com.grappim.wallosmobile.core.domain.UntrustedCertificateException
import com.grappim.wallosmobile.core.domain.WallosError
import com.grappim.wallosmobile.feature.setup.domain.model.ApiKeyNotFound
import com.grappim.wallosmobile.feature.setup.domain.model.LoginOutcome
import com.grappim.wallosmobile.feature.setup.domain.repo.SetupRepository
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.error_invalid_api_key
import com.grappim.wallosmobile.strings.generated.resources.error_not_wallos
import com.grappim.wallosmobile.strings.generated.resources.error_unreachable
import com.grappim.wallosmobile.strings.generated.resources.login_error_api_key_missing
import com.grappim.wallosmobile.strings.generated.resources.login_error_cert_not_trusted
import com.grappim.wallosmobile.strings.generated.resources.login_error_invalid_credentials
import com.grappim.wallosmobile.strings.generated.resources.login_error_needs_totp
import com.grappim.wallosmobile.testing.MainDispatcherRule
import com.grappim.wallosmobile.utils.ui.NativeText
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.jetbrains.compose.resources.StringResource
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LoginViewModelTest {

    private val repository = FakeSetupRepository()
    private val mainDispatcherRule = MainDispatcherRule()

    @BeforeTest
    fun setup() {
        mainDispatcherRule.setup()
    }

    @AfterTest
    fun tearDown() {
        mainDispatcherRule.tearDown()
    }

    private fun viewModel() = LoginViewModel(repository)

    private fun LoginViewModel.fillCredentials() {
        with(uiState.value) {
            onServerUrlChange(SERVER_URL)
            onUsernameChange(USERNAME)
            onPasswordChange(PASSWORD)
        }
    }

    private fun LoginViewModel.fillApiKey() {
        with(uiState.value) {
            onServerUrlChange(SERVER_URL)
            onApiKeyModeChange(true)
            onApiKeyChange(API_KEY)
        }
    }

    @Test
    fun `connect is disabled until the visible path's fields are filled`() {
        val sut = viewModel()

        assertFalse(sut.uiState.value.canConnect)

        sut.uiState.value.onServerUrlChange(SERVER_URL)
        sut.uiState.value.onUsernameChange(USERNAME)
        assertFalse(sut.uiState.value.canConnect)

        sut.uiState.value.onPasswordChange(PASSWORD)
        assertTrue(sut.uiState.value.canConnect)
    }

    /** Switching paths must not carry the other path's readiness with it. */
    @Test
    fun `switching to api key mode makes connect depend on the key, not the credentials`() {
        val sut = viewModel()
        sut.fillCredentials()

        sut.uiState.value.onApiKeyModeChange(true)
        assertFalse(sut.uiState.value.canConnect)

        sut.uiState.value.onApiKeyChange(API_KEY)
        assertTrue(sut.uiState.value.canConnect)
    }

    @Test
    fun `connect with credentials drives the password path`() = runTest {
        repository.loginResult = Result.success(LoginOutcome.Connected)
        val sut = viewModel()
        sut.fillCredentials()

        sut.uiState.value.onConnectClick()

        assertEquals(Triple(SERVER_URL, USERNAME, PASSWORD), repository.loginCall)
        assertFalse(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.error.isEmpty())
    }

    /** The password is exchanged for the key and then has no reason to stay in memory. */
    @Test
    fun `a connected session drops the password from state`() = runTest {
        repository.loginResult = Result.success(LoginOutcome.Connected)
        val sut = viewModel()
        sut.fillCredentials()

        sut.uiState.value.onConnectClick()

        assertEquals("", sut.uiState.value.password)
    }

    @Test
    fun `connect in api key mode takes the manual path`() = runTest {
        repository.connectResult = Result.success(Unit)
        val sut = viewModel()
        sut.fillApiKey()

        sut.uiState.value.onConnectClick()

        assertEquals(SERVER_URL to API_KEY, repository.connectCall)
        assertEquals(null, repository.loginCall)
        assertTrue(sut.uiState.value.error.isEmpty())
    }

    @Test
    fun `rejected credentials become a message`() = runTest {
        repository.loginResult = Result.success(LoginOutcome.InvalidCredentials)
        val sut = viewModel()
        sut.fillCredentials()

        sut.uiState.value.onConnectClick()

        assertEquals(resource(RString.login_error_invalid_credentials), sut.uiState.value.error)
        assertFalse(sut.uiState.value.isLoading)
    }

    /** v1 doesn't drive `totp.php` — the message has to send the user to the key field instead. */
    @Test
    fun `a totp challenge points at the api key path`() = runTest {
        repository.loginResult = Result.success(LoginOutcome.NeedsTotp)
        val sut = viewModel()
        sut.fillCredentials()

        sut.uiState.value.onConnectClick()

        assertEquals(resource(RString.login_error_needs_totp), sut.uiState.value.error)
    }

    /** Layer 1: nothing that looked like Wallos answered, so the URL is what is wrong. */
    @Test
    fun `a malformed response blames the server url`() = runTest {
        repository.loginResult = Result.failure(WallosError.Malformed("<html>"))
        val sut = viewModel()
        sut.fillCredentials()

        sut.uiState.value.onConnectClick()

        assertEquals(resource(RString.error_not_wallos), sut.uiState.value.error)
    }

    /** Layer 3: a well-formed refusal means the URL was right and the key is not. */
    @Test
    fun `a rejected api key blames the key`() = runTest {
        repository.connectResult = Result.failure(WallosError.Unauthenticated("Unauthorized"))
        val sut = viewModel()
        sut.fillApiKey()

        sut.uiState.value.onConnectClick()

        assertEquals(resource(RString.error_invalid_api_key), sut.uiState.value.error)
    }

    /** Never reached the envelope at all, so it is transport — the URL again. */
    @Test
    fun `a transport failure blames the connection`() = runTest {
        repository.loginResult = Result.failure(IllegalStateException("connection refused"))
        val sut = viewModel()
        sut.fillCredentials()

        sut.uiState.value.onConnectClick()

        assertEquals(resource(RString.error_unreachable), sut.uiState.value.error)
        assertEquals(null, sut.uiState.value.pendingCertTrust)
    }

    /** The credentials were right and the URL was right — only the account is missing a key. */
    @Test
    fun `a missing api key on the profile page gets its own message`() = runTest {
        repository.loginResult = Result.failure(ApiKeyNotFound)
        val sut = viewModel()
        sut.fillCredentials()

        sut.uiState.value.onConnectClick()

        assertEquals(resource(RString.login_error_api_key_missing), sut.uiState.value.error)
    }

    @Test
    fun `editing a field clears the previous error`() = runTest {
        repository.loginResult = Result.success(LoginOutcome.InvalidCredentials)
        val sut = viewModel()
        sut.fillCredentials()
        sut.uiState.value.onConnectClick()

        sut.uiState.value.onPasswordChange("another")

        assertTrue(sut.uiState.value.error.isEmpty())
    }

    /** An error attributed to fields the user can no longer see is worse than no error. */
    @Test
    fun `switching paths clears the previous error`() = runTest {
        repository.loginResult = Result.success(LoginOutcome.InvalidCredentials)
        val sut = viewModel()
        sut.fillCredentials()
        sut.uiState.value.onConnectClick()

        sut.uiState.value.onApiKeyModeChange(true)

        assertTrue(sut.uiState.value.error.isEmpty())
    }

    /** 2.6 left this open: `clear()` keeps the URL, but nothing offered it back until now. */
    @Test
    fun `the stored server url is offered back`() {
        repository.storedServerUrlResult = Result.success(SERVER_URL)

        val sut = viewModel()

        assertEquals(SERVER_URL, sut.uiState.value.serverUrl)
    }

    @Test
    fun `a fresh install starts with an empty server url`() {
        val sut = viewModel()

        assertEquals("", sut.uiState.value.serverUrl)
    }

    /** The read lands a beat after construction — whatever the user typed meanwhile has to win. */
    @Test
    fun `a url the user has already typed survives the prefill`() = runTest {
        repository.storedServerUrlResult = Result.success(SERVER_URL)
        repository.storedServerUrlGate = CompletableDeferred()
        val sut = viewModel()

        sut.uiState.value.onServerUrlChange(OTHER_SERVER_URL)
        repository.storedServerUrlGate.complete(Unit)

        assertEquals(OTHER_SERVER_URL, sut.uiState.value.serverUrl)
    }

    /** No prefill is a worse screen, not a broken one. */
    @Test
    fun `a failed read leaves the field empty rather than crashing`() {
        repository.storedServerUrlResult = Result.failure(IllegalStateException("datastore"))

        val sut = viewModel()

        assertEquals("", sut.uiState.value.serverUrl)
    }

    @Test
    fun `an http server url warns that the password would cross in the clear`() {
        val sut = viewModel()

        sut.uiState.value.onServerUrlChange(CLEARTEXT_SERVER_URL)

        assertTrue(sut.uiState.value.isCleartextWarningVisible)
    }

    @Test
    fun `an https server url does not warn`() {
        val sut = viewModel()

        sut.uiState.value.onServerUrlChange(SERVER_URL)

        assertFalse(sut.uiState.value.isCleartextWarningVisible)
    }

    /** The warning steers to Path B; once there, the password it was about is out of the picture. */
    @Test
    fun `taking the api key path clears the cleartext warning`() {
        val sut = viewModel()
        sut.uiState.value.onServerUrlChange(CLEARTEXT_SERVER_URL)

        sut.uiState.value.onApiKeyModeChange(true)

        assertFalse(sut.uiState.value.isCleartextWarningVisible)
    }

    /** Warn, don't disable: the only instance every `Verify:` line can use is plain HTTP. */
    @Test
    fun `the cleartext warning does not block the password path`() = runTest {
        repository.loginResult = Result.success(LoginOutcome.Connected)
        val sut = viewModel()
        with(sut.uiState.value) {
            onServerUrlChange(CLEARTEXT_SERVER_URL)
            onUsernameChange(USERNAME)
            onPasswordChange(PASSWORD)
        }
        assertTrue(sut.uiState.value.canConnect)

        sut.uiState.value.onConnectClick()

        assertEquals(Triple(CLEARTEXT_SERVER_URL, USERNAME, PASSWORD), repository.loginCall)
    }

    /** The URL and the connection were both right, so the generic message would misattribute. */
    @Test
    fun `an untrusted certificate raises the trust prompt instead of an error`() = runTest {
        repository.loginResult = Result.failure(handshakeFailure())
        val sut = viewModel()
        sut.fillCredentials()

        sut.uiState.value.onConnectClick()

        assertEquals(PENDING_CERT_TRUST, sut.uiState.value.pendingCertTrust)
        assertTrue(sut.uiState.value.error.isEmpty())
        assertFalse(sut.uiState.value.isLoading)
    }

    @Test
    fun `accepting the certificate pins it and retries the attempt that failed`() = runTest {
        repository.loginResult = Result.failure(handshakeFailure())
        val sut = viewModel()
        sut.fillCredentials()
        sut.uiState.value.onConnectClick()

        repository.loginResult = Result.success(LoginOutcome.Connected)
        sut.uiState.value.onCertTrustConfirm()

        assertEquals(PENDING_CERT_TRUST, repository.trustCall)
        assertEquals(2, repository.loginCallCount)
        assertEquals(null, sut.uiState.value.pendingCertTrust)
        assertTrue(sut.uiState.value.error.isEmpty())
    }

    /** The retry re-reads the state, so it follows whichever path raised the prompt. */
    @Test
    fun `accepting the certificate retries the api key path too`() = runTest {
        repository.connectResult = Result.failure(handshakeFailure())
        val sut = viewModel()
        sut.fillApiKey()
        sut.uiState.value.onConnectClick()

        repository.connectResult = Result.success(Unit)
        sut.uiState.value.onCertTrustConfirm()

        assertEquals(2, repository.connectCallCount)
        assertEquals(0, repository.loginCallCount)
    }

    /** Declining is a decision — Connect did nothing, and the screen has to say why. */
    @Test
    fun `declining the certificate closes the prompt and reports it`() = runTest {
        repository.loginResult = Result.failure(handshakeFailure())
        val sut = viewModel()
        sut.fillCredentials()
        sut.uiState.value.onConnectClick()

        sut.uiState.value.onCertTrustDismiss()

        assertEquals(null, sut.uiState.value.pendingCertTrust)
        assertEquals(resource(RString.login_error_cert_not_trusted), sut.uiState.value.error)
        assertEquals(1, repository.loginCallCount)
        assertEquals(null, repository.trustCall)
    }

    /** Retrying without the pin would raise the same prompt again, forever. */
    @Test
    fun `a pin that could not be stored is reported instead of retried`() = runTest {
        repository.loginResult = Result.failure(handshakeFailure())
        repository.trustResult = Result.failure(IllegalStateException("datastore"))
        val sut = viewModel()
        sut.fillCredentials()
        sut.uiState.value.onConnectClick()

        sut.uiState.value.onCertTrustConfirm()

        assertEquals(1, repository.loginCallCount)
        assertEquals(resource(RString.login_error_cert_not_trusted), sut.uiState.value.error)
    }

    @Test
    fun `connect does nothing while a field is blank`() = runTest {
        val sut = viewModel()
        sut.uiState.value.onServerUrlChange(SERVER_URL)

        sut.uiState.value.onConnectClick()

        assertEquals(null, repository.loginCall)
        assertFalse(sut.uiState.value.isLoading)
    }

    private fun resource(resource: StringResource) = NativeText.Resource(resource)

    /**
     * Nothing throws [UntrustedCertificateException] directly: the TLS stack wraps whatever the
     * trust manager threw, so the ViewModel has to find it down a cause chain (plan §4.5).
     */
    private fun handshakeFailure() = IllegalStateException(
        "handshake failed",
        IllegalStateException("certificate", UntrustedCertificateException(PENDING_CERT_TRUST))
    )

    /**
     * Private to this file on purpose: `:testing` is for doubles *other* modules need, and
     * `feature:setup:ui` is the only consumer of this one (CLAUDE.md).
     */
    private class FakeSetupRepository : SetupRepository {
        var loginResult: Result<LoginOutcome>? = null
        var connectResult: Result<Unit>? = null
        var loginCall: Triple<String, String, String>? = null
        var connectCall: Pair<String, String>? = null
        var loginCallCount = 0
        var connectCallCount = 0
        var storedServerUrlResult: Result<String> = Result.success("")
        var trustResult: Result<Unit> = Result.success(Unit)
        var trustCall: PendingCertTrust? = null

        override suspend fun trustCertificate(pendingCertTrust: PendingCertTrust): Result<Unit> {
            trustCall = pendingCertTrust
            return trustResult
        }

        /** Already complete, so the prefill lands during construction unless a test says otherwise. */
        var storedServerUrlGate = CompletableDeferred(Unit)

        override suspend fun getStoredServerUrl(): Result<String> {
            storedServerUrlGate.await()
            return storedServerUrlResult
        }

        override suspend fun loginWithPassword(
            serverUrl: String,
            username: String,
            password: String
        ): Result<LoginOutcome> {
            loginCall = Triple(serverUrl, username, password)
            loginCallCount++
            return loginResult ?: error("loginResult not set")
        }

        override suspend fun connectWithApiKey(serverUrl: String, apiKey: String): Result<Unit> {
            connectCall = serverUrl to apiKey
            connectCallCount++
            return connectResult ?: error("connectResult not set")
        }
    }

    private companion object {
        const val SERVER_URL = "https://wallos.example.com"
        const val OTHER_SERVER_URL = "https://other.example.com"
        const val CLEARTEXT_SERVER_URL = "http://wallos.lan:8282"
        const val USERNAME = "demo"
        const val PASSWORD = "demo"
        const val API_KEY = "5c1e0b2a9f"

        val PENDING_CERT_TRUST = PendingCertTrust(
            host = "wallos.lan",
            subject = "CN=wallos.lan",
            issuer = "CN=wallos.lan",
            notBefore = "2026-01-04",
            notAfter = "2027-01-04",
            sha256Fingerprint = "3A:7B:1C:04"
        )
    }
}

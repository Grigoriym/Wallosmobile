package com.grappim.wallosmobile.feature.setup.ui

import com.grappim.wallosmobile.core.domain.PendingCertTrust
import com.grappim.wallosmobile.core.domain.UntrustedCertificateException
import com.grappim.wallosmobile.core.domain.WallosError
import com.grappim.wallosmobile.feature.setup.domain.model.ApiKeyNotFound
import com.grappim.wallosmobile.feature.setup.domain.model.LoginOutcome
import com.grappim.wallosmobile.feature.setup.domain.model.PasswordLoginAvailability
import com.grappim.wallosmobile.feature.setup.domain.repo.SetupRepository
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.error_invalid_api_key
import com.grappim.wallosmobile.strings.generated.resources.error_not_wallos
import com.grappim.wallosmobile.strings.generated.resources.error_unreachable
import com.grappim.wallosmobile.strings.generated.resources.login_error_api_key_missing
import com.grappim.wallosmobile.strings.generated.resources.login_error_cert_not_trusted
import com.grappim.wallosmobile.strings.generated.resources.login_error_invalid_credentials
import com.grappim.wallosmobile.strings.generated.resources.login_error_invalid_totp
import com.grappim.wallosmobile.strings.generated.resources.login_error_totp_session_expired
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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

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

    /** The challenge is a second step on this screen, not a dead end — and not an error either. */
    @Test
    fun `a totp challenge turns the screen into a code field`() = runTest {
        repository.loginResult = Result.success(LoginOutcome.NeedsTotp)
        val sut = viewModel()
        sut.fillCredentials()

        sut.uiState.value.onConnectClick()

        assertTrue(sut.uiState.value.isTotpRequired)
        assertTrue(sut.uiState.value.error.isEmpty())
        assertFalse(sut.uiState.value.isLoading)
    }

    /** The session carries the challenge from here, so the password has no further use. */
    @Test
    fun `a totp challenge drops the password from state`() = runTest {
        repository.loginResult = Result.success(LoginOutcome.NeedsTotp)
        val sut = viewModel()
        sut.fillCredentials()

        sut.uiState.value.onConnectClick()

        assertEquals("", sut.uiState.value.password)
    }

    @Test
    fun `connect is disabled until the code is typed, then submits it`() = runTest {
        repository.loginResult = Result.success(LoginOutcome.NeedsTotp)
        val sut = viewModel()
        sut.fillCredentials()
        sut.uiState.value.onConnectClick()
        assertFalse(sut.uiState.value.canConnect)

        sut.uiState.value.onTotpCodeChange(TOTP_CODE)
        assertTrue(sut.uiState.value.canConnect)

        repository.totpResult = Result.success(LoginOutcome.Connected)
        sut.uiState.value.onConnectClick()

        assertEquals(TOTP_CODE, repository.totpCall)
        assertEquals(1, repository.loginCallCount)
    }

    /** The next code off the authenticator is a working answer, so the field has to stay. */
    @Test
    fun `a rejected code keeps the challenge up`() = runTest {
        val sut = totpChallenge()
        repository.totpResult = Result.success(LoginOutcome.InvalidTotpCode)

        sut.uiState.value.onConnectClick()

        assertTrue(sut.uiState.value.isTotpRequired)
        assertEquals(resource(RString.login_error_invalid_totp), sut.uiState.value.error)
    }

    /** No code can complete this attempt — leaving the field up would loop the user forever. */
    @Test
    fun `a lost session sends the user back to the credentials`() = runTest {
        val sut = totpChallenge()
        repository.totpResult = Result.success(LoginOutcome.TotpSessionExpired)

        sut.uiState.value.onConnectClick()

        assertFalse(sut.uiState.value.isTotpRequired)
        assertEquals("", sut.uiState.value.totpCode)
        assertEquals(resource(RString.login_error_totp_session_expired), sut.uiState.value.error)
    }

    /** The key path never answers a challenge, so switching to it abandons the one in flight. */
    @Test
    fun `switching to the api key path abandons the challenge`() = runTest {
        val sut = totpChallenge()

        sut.uiState.value.onApiKeyModeChange(true)

        assertFalse(sut.uiState.value.isTotpRequired)
        assertEquals("", sut.uiState.value.totpCode)
    }

    /** By the code step the password has already crossed the wire; the warning has nothing left. */
    @Test
    fun `the cleartext warning retires once the challenge is up`() = runTest {
        repository.loginResult = Result.success(LoginOutcome.NeedsTotp)
        val sut = viewModel()
        with(sut.uiState.value) {
            onServerUrlChange(CLEARTEXT_SERVER_URL)
            onUsernameChange(USERNAME)
            onPasswordChange(PASSWORD)
        }
        assertTrue(sut.uiState.value.isCleartextWarningVisible)

        sut.uiState.value.onConnectClick()

        assertFalse(sut.uiState.value.isCleartextWarningVisible)
    }

    /** The retry re-reads the state, so a certificate rotated mid-challenge resumes at the code. */
    @Test
    fun `accepting the certificate retries the code, not the credentials`() = runTest {
        val sut = totpChallenge()
        repository.totpResult = Result.failure(handshakeFailure())
        sut.uiState.value.onConnectClick()

        repository.totpResult = Result.success(LoginOutcome.Connected)
        sut.uiState.value.onCertTrustConfirm()

        assertEquals(2, repository.totpCallCount)
        assertEquals(1, repository.loginCallCount)
    }

    /** The screen at the point where the credentials have been accepted and a code is wanted. */
    private fun totpChallenge(): LoginViewModel {
        repository.loginResult = Result.success(LoginOutcome.NeedsTotp)
        val sut = viewModel()
        sut.fillCredentials()
        sut.uiState.value.onConnectClick()
        sut.uiState.value.onTotpCodeChange(TOTP_CODE)
        return sut
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

    /**
     * The point of the probe: it lands before the password does, off the URL field alone.
     * The debounce is spent on the rule's own scheduler — `viewModelScope` dispatches on the main
     * dispatcher this test installs, so that is where the `delay` is parked.
     */
    @Test
    fun `an sso-only instance takes the password path off the screen`() = runTest {
        repository.probeResult = Result.success(PasswordLoginAvailability.Disabled)
        val sut = viewModel()

        sut.uiState.value.onServerUrlChange(SERVER_URL)
        settleProbe()

        assertEquals(listOf(SERVER_URL), repository.probeCalls)
        assertTrue(sut.uiState.value.isPasswordLoginDisabled)
        assertTrue(sut.uiState.value.isApiKeyMode)
    }

    @Test
    fun `an instance that accepts passwords leaves both paths on offer`() = runTest {
        val sut = viewModel()

        sut.uiState.value.onServerUrlChange(SERVER_URL)
        settleProbe()

        assertFalse(sut.uiState.value.isPasswordLoginDisabled)
        assertFalse(sut.uiState.value.isApiKeyMode)
    }

    /** It hides a path, so an unreachable server must not be enough to set it. */
    @Test
    fun `a probe that fails changes nothing`() = runTest {
        repository.probeResult = Result.failure(IllegalStateException("connection refused"))
        val sut = viewModel()

        sut.uiState.value.onServerUrlChange(SERVER_URL)
        settleProbe()

        assertFalse(sut.uiState.value.isPasswordLoginDisabled)
        assertTrue(sut.uiState.value.error.isEmpty())
    }

    /** Same rule, for the answer that says the page could not be read. */
    @Test
    fun `an unreadable login page changes nothing`() = runTest {
        repository.probeResult = Result.success(PasswordLoginAvailability.Unknown)
        val sut = viewModel()

        sut.uiState.value.onServerUrlChange(SERVER_URL)
        settleProbe()

        assertFalse(sut.uiState.value.isPasswordLoginDisabled)
    }

    /** The app infers no scheme, so half a URL is not an instance worth asking. */
    @Test
    fun `a url with no scheme is not probed`() = runTest {
        val sut = viewModel()

        sut.uiState.value.onServerUrlChange("wallos.example")
        settleProbe()

        assertTrue(repository.probeCalls.isEmpty())
    }

    /** One request for a URL the user typed, not one per keystroke. */
    @Test
    fun `typing a url probes it once`() = runTest {
        val sut = viewModel()

        with(sut.uiState.value) {
            onServerUrlChange("https://wallos.exam")
            onServerUrlChange("https://wallos.example")
            onServerUrlChange(SERVER_URL)
        }
        settleProbe()

        assertEquals(listOf(SERVER_URL), repository.probeCalls)
    }

    /** 3.1's prefill is a URL nobody typed, and it is the one most worth probing. */
    @Test
    fun `the prefilled url is probed too`() = runTest {
        repository.storedServerUrlResult = Result.success(SERVER_URL)

        viewModel()
        settleProbe()

        assertEquals(listOf(SERVER_URL), repository.probeCalls)
    }

    /**
     * `login.php` clears `$_SESSION['totp_user_id']` as it renders, so a probe fired from the URL
     * field at the code step would answer the next code with `TotpSessionExpired`.
     */
    @Test
    fun `the probe stays away from a pending totp challenge`() = runTest {
        val sut = totpChallenge()

        sut.uiState.value.onServerUrlChange(OTHER_SERVER_URL)
        settleProbe()

        assertTrue(repository.probeCalls.isEmpty())
    }

    /** The debounce is parked on the main dispatcher's scheduler, so that is what has to move. */
    private fun settleProbe() {
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
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

    /**
     * The state exists only *while* the attempt is in flight — the wait is spent inside the call —
     * and `MainDispatcherRule` is unconfined, so the gate is what holds the attempt open long
     * enough to see it at all (3.4).
     */
    @Test
    fun `a held-back attempt says how long it is waiting`() = runTest {
        repository.loginResult = Result.success(LoginOutcome.InvalidCredentials)
        repository.throttleWait = 4.seconds
        repository.attemptGate = CompletableDeferred()
        val sut = viewModel()
        sut.fillCredentials()

        sut.uiState.value.onConnectClick()

        assertEquals(4, sut.uiState.value.throttleWaitSeconds)
        assertTrue(sut.uiState.value.isThrottled)
        assertTrue(sut.uiState.value.isLoading)
    }

    /** The attempt is over however it went, so the wait it announced is no longer news. */
    @Test
    fun `the wait stops being announced once the attempt lands`() = runTest {
        repository.loginResult = Result.success(LoginOutcome.InvalidCredentials)
        repository.throttleWait = 4.seconds
        repository.attemptGate = CompletableDeferred()
        val sut = viewModel()
        sut.fillCredentials()
        sut.uiState.value.onConnectClick()

        repository.attemptGate.complete(Unit)

        assertFalse(sut.uiState.value.isThrottled)
        assertEquals(resource(RString.login_error_invalid_credentials), sut.uiState.value.error)
    }

    /** The sharper of the two guesses (plan §9), so the code step has to announce it as well. */
    @Test
    fun `a held-back code says how long it is waiting too`() = runTest {
        val sut = totpChallenge()
        repository.totpResult = Result.success(LoginOutcome.InvalidTotpCode)
        repository.throttleWait = 8.seconds
        repository.attemptGate = CompletableDeferred()

        sut.uiState.value.onConnectClick()

        assertEquals(8, sut.uiState.value.throttleWaitSeconds)
    }

    /** Nothing was held back, so there is nothing to say and the spinner stands alone. */
    @Test
    fun `an attempt that is not held back announces nothing`() = runTest {
        repository.loginResult = Result.success(LoginOutcome.InvalidCredentials)
        repository.attemptGate = CompletableDeferred()
        val sut = viewModel()
        sut.fillCredentials()

        sut.uiState.value.onConnectClick()

        assertFalse(sut.uiState.value.isThrottled)
        assertTrue(sut.uiState.value.isLoading)
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
        var totpResult: Result<LoginOutcome>? = null
        var loginCall: Triple<String, String, String>? = null
        var connectCall: Pair<String, String>? = null
        var totpCall: String? = null
        var loginCallCount = 0
        var connectCallCount = 0
        var totpCallCount = 0
        var storedServerUrlResult: Result<String> = Result.success("")
        var trustResult: Result<Unit> = Result.success(Unit)
        var trustCall: PendingCertTrust? = null
        var probeResult: Result<PasswordLoginAvailability> = Result.success(PasswordLoginAvailability.Available)
        val probeCalls = mutableListOf<String>()

        override suspend fun probePasswordLogin(serverUrl: String): Result<PasswordLoginAvailability> {
            probeCalls += serverUrl
            return probeResult
        }

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

        /** Announced from inside the call, exactly where the real throttle spends the wait. */
        var throttleWait: Duration? = null

        /** Open unless a test wants to look at the state an attempt has while it is in flight. */
        var attemptGate = CompletableDeferred(Unit)

        override suspend fun loginWithPassword(
            serverUrl: String,
            username: String,
            password: String,
            onThrottleWait: (Duration) -> Unit
        ): Result<LoginOutcome> {
            loginCall = Triple(serverUrl, username, password)
            loginCallCount++
            throttleWait?.let(onThrottleWait)
            attemptGate.await()
            return loginResult ?: error("loginResult not set")
        }

        override suspend fun connectWithApiKey(serverUrl: String, apiKey: String): Result<Unit> {
            connectCall = serverUrl to apiKey
            connectCallCount++
            return connectResult ?: error("connectResult not set")
        }

        override suspend fun submitTotpCode(code: String, onThrottleWait: (Duration) -> Unit): Result<LoginOutcome> {
            totpCall = code
            totpCallCount++
            throttleWait?.let(onThrottleWait)
            attemptGate.await()
            return totpResult ?: error("totpResult not set")
        }
    }

    private companion object {
        const val SERVER_URL = "https://wallos.example.com"
        const val OTHER_SERVER_URL = "https://other.example.com"
        const val CLEARTEXT_SERVER_URL = "http://wallos.lan:8282"
        const val USERNAME = "demo"
        const val PASSWORD = "demo"
        const val API_KEY = "5c1e0b2a9f"
        const val TOTP_CODE = "123456"

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

package com.grappim.wallosmobile.feature.setup.ui

import com.grappim.wallosmobile.core.domain.WallosError
import com.grappim.wallosmobile.feature.setup.domain.model.ApiKeyNotFound
import com.grappim.wallosmobile.feature.setup.domain.model.LoginOutcome
import com.grappim.wallosmobile.feature.setup.domain.repo.SetupRepository
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.error_invalid_api_key
import com.grappim.wallosmobile.strings.generated.resources.error_not_wallos
import com.grappim.wallosmobile.strings.generated.resources.error_unreachable
import com.grappim.wallosmobile.strings.generated.resources.login_error_api_key_missing
import com.grappim.wallosmobile.strings.generated.resources.login_error_invalid_credentials
import com.grappim.wallosmobile.strings.generated.resources.login_error_needs_totp
import com.grappim.wallosmobile.testing.MainDispatcherRule
import com.grappim.wallosmobile.utils.ui.NativeText
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
     * Private to this file on purpose: `:testing` is for doubles *other* modules need, and
     * `feature:setup:ui` is the only consumer of this one (CLAUDE.md).
     */
    private class FakeSetupRepository : SetupRepository {
        var loginResult: Result<LoginOutcome>? = null
        var connectResult: Result<Unit>? = null
        var loginCall: Triple<String, String, String>? = null
        var connectCall: Pair<String, String>? = null

        override suspend fun loginWithPassword(
            serverUrl: String,
            username: String,
            password: String
        ): Result<LoginOutcome> {
            loginCall = Triple(serverUrl, username, password)
            return loginResult ?: error("loginResult not set")
        }

        override suspend fun connectWithApiKey(serverUrl: String, apiKey: String): Result<Unit> {
            connectCall = serverUrl to apiKey
            return connectResult ?: error("connectResult not set")
        }
    }

    private companion object {
        const val SERVER_URL = "https://wallos.example.com"
        const val USERNAME = "demo"
        const val PASSWORD = "demo"
        const val API_KEY = "5c1e0b2a9f"
    }
}

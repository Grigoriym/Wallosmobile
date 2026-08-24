package com.grappim.wallosmobile.feature.setup.data

import com.grappim.wallosmobile.core.api.FormParams
import com.grappim.wallosmobile.core.api.WallosApiClient
import com.grappim.wallosmobile.core.api.post
import com.grappim.wallosmobile.core.asynckmp.IoDispatcher
import com.grappim.wallosmobile.core.domain.PendingCertTrust
import com.grappim.wallosmobile.core.domain.resultOf
import com.grappim.wallosmobile.core.storage.ApiKeyStorage
import com.grappim.wallosmobile.core.storage.ServerUrlStorage
import com.grappim.wallosmobile.core.storage.cert.TrustedCertStorage
import com.grappim.wallosmobile.feature.setup.domain.model.ApiKeyNotFound
import com.grappim.wallosmobile.feature.setup.domain.model.LoginOutcome
import com.grappim.wallosmobile.feature.setup.domain.model.PasswordLoginAvailability
import com.grappim.wallosmobile.feature.setup.domain.repo.SetupRepository
import com.grappim.wallosmobile.feature.setup.dto.VersionDTO
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory
import kotlin.time.Duration

/**
 * A `@Factory`, for the same reason [WebLoginApiImpl] is: it owns the web session transitively.
 */
@Factory(binds = [SetupRepository::class])
internal class SetupRepositoryImpl(
    private val webLoginApi: WebLoginApi,
    private val wallosApiClient: WallosApiClient,
    private val serverUrlStorage: ServerUrlStorage,
    private val apiKeyStorage: ApiKeyStorage,
    private val trustedCertStorage: TrustedCertStorage,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher
) : SetupRepository {

    /**
     * Constructed rather than injected: it has no dependencies, and the lifetime it wants is
     * exactly this object's — one login screen (plan §1.1). Injecting it would also make this the
     * seventh constructor parameter, which detekt's `allowedConstructorParameters: 6` refuses, and
     * a dependency-free counter is not the seam that limit is drawing.
     */
    private val loginThrottle = LoginThrottle()

    /**
     * The URL is persisted first for the same reason [loginWithPassword] does it: `login.php` is
     * requested relative to it, and that relative form is what keeps a subpath install working.
     */
    override suspend fun probePasswordLogin(serverUrl: String): Result<PasswordLoginAvailability> = resultOf {
        withContext(dispatcher) {
            serverUrlStorage.saveServerUrl(serverUrl.trim())
            webLoginApi.probePasswordLogin()
        }
    }

    override suspend fun loginWithPassword(
        serverUrl: String,
        username: String,
        password: String,
        onThrottleWait: (Duration) -> Unit
    ): Result<LoginOutcome> = resultOf {
        withContext(dispatcher) {
            // Everything below resolves against this, so it has to land before the first call.
            serverUrlStorage.saveServerUrl(serverUrl.trim())
            // A fresh attempt supersedes whatever was stored, and it has to: `WallosApiClient`
            // injects the *stored* key over any `api_key` a caller passes, so a leftover key would
            // be what the validation call below actually validated.
            apiKeyStorage.clear()

            // The server has no lockout of its own, so this is the only thing between a retry
            // loop and the user's own instance (plan §9).
            loginThrottle.awaitTurn(onThrottleWait)
            when (webLoginApi.login(username, password)) {
                WebLoginOutcome.NeedsTotp -> LoginOutcome.NeedsTotp
                WebLoginOutcome.InvalidCredentials -> refused(LoginOutcome.InvalidCredentials)
                WebLoginOutcome.LoggedIn -> takeApiKey()
            }
        }
        // The session cookie dies with this `@Factory`'s `HttpClient`, and `password` is never
        // written anywhere — the key is the only thing that outlives the call.
    }

    /**
     * No `serverUrl` and no `apiKeyStorage.clear()`: both were done by the [loginWithPassword]
     * that raised the challenge, and this runs on that same attempt's session.
     */
    override suspend fun submitTotpCode(code: String, onThrottleWait: (Duration) -> Unit): Result<LoginOutcome> =
        resultOf {
            withContext(dispatcher) {
                // Six digits against a window 31 codes wide, on a server that counts no attempts —
                // the one place the backoff matters more than it does on the password itself.
                loginThrottle.awaitTurn(onThrottleWait)
                when (webLoginApi.submitTotpCode(code.trim())) {
                    WebTotpOutcome.InvalidCode -> refused(LoginOutcome.InvalidTotpCode)

                    // Not a refused guess: no code was ever weighed, so nothing was learned by
                    // sending it and there is nothing to slow down.
                    WebTotpOutcome.SessionExpired -> LoginOutcome.TotpSessionExpired

                    WebTotpOutcome.LoggedIn -> takeApiKey()
                }
            }
        }

    /** The tail both paths through the web login share, once the session is actually logged in. */
    private suspend fun takeApiKey(): LoginOutcome {
        val apiKey = webLoginApi.fetchApiKey() ?: throw ApiKeyNotFound
        validate(apiKey)
        apiKeyStorage.setKey(apiKey)
        loginThrottle.reset()
        return LoginOutcome.Connected
    }

    /** A credential the instance weighed and rejected — the only thing that grows the backoff. */
    private fun refused(outcome: LoginOutcome): LoginOutcome {
        loginThrottle.recordRefusal()
        return outcome
    }

    /**
     * Not throttled, unlike the two paths above. A pasted key is not a guess: nobody arrives at a
     * 32-character credential by retrying, so a backoff here would only punish a paste that went
     * wrong — and the recovery route is the last thing that should get slower.
     */
    override suspend fun connectWithApiKey(serverUrl: String, apiKey: String): Result<Unit> = resultOf {
        withContext(dispatcher) {
            serverUrlStorage.saveServerUrl(serverUrl.trim())
            // Same reason as above, and it matters more here: the key the user just pasted is the
            // one that has to be validated, not whatever `WallosApiClient` would inject over it.
            apiKeyStorage.clear()

            val trimmed = apiKey.trim()
            validate(trimmed)
            apiKeyStorage.setKey(trimmed)
        }
    }

    /**
     * `ServerUrlStorage.serverUrl` is non-suspending and blocks on its very first read (plan §4.7),
     * so it is worth the `withContext` even though nothing here is IO once the value is cached.
     */
    override suspend fun getStoredServerUrl(): Result<String> = resultOf {
        withContext(dispatcher) {
            serverUrlStorage.serverUrl
        }
    }

    /**
     * The pin is scoped to `(host, fingerprint)` (plan §4.5) — a certificate accepted for one
     * instance can't authenticate another — but the full [pendingCertTrust] is what gets stored,
     * so the Settings screen (18.2) can show what it's revoking.
     */
    override suspend fun trustCertificate(pendingCertTrust: PendingCertTrust): Result<Unit> = resultOf {
        withContext(dispatcher) {
            trustedCertStorage.trust(pendingCertTrust)
        }
    }

    /**
     * Proves the string is a working credential before it is stored, so a markup change upstream
     * — or a mistyped key on Path B — surfaces here rather than as an `Unauthenticated` on the
     * first real screen (API doc §9.5). Throws a `WallosError` if not.
     */
    private suspend fun validate(apiKey: String) {
        wallosApiClient.post<VersionDTO>(VERSION_PATH, FormParams().put(API_KEY_PARAM, apiKey))
    }

    private companion object {
        const val VERSION_PATH = "api/status/version.php"
        const val API_KEY_PARAM = "api_key"
    }
}

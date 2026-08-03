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
import com.grappim.wallosmobile.feature.setup.domain.repo.SetupRepository
import com.grappim.wallosmobile.feature.setup.dto.VersionDTO
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory

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

    override suspend fun loginWithPassword(
        serverUrl: String,
        username: String,
        password: String
    ): Result<LoginOutcome> = resultOf {
        withContext(dispatcher) {
            // Everything below resolves against this, so it has to land before the first call.
            serverUrlStorage.saveServerUrl(serverUrl.trim())
            // A fresh attempt supersedes whatever was stored, and it has to: `WallosApiClient`
            // injects the *stored* key over any `api_key` a caller passes, so a leftover key would
            // be what the validation call below actually validated.
            apiKeyStorage.clear()

            when (webLoginApi.login(username, password)) {
                WebLoginOutcome.NeedsTotp -> LoginOutcome.NeedsTotp
                WebLoginOutcome.InvalidCredentials -> LoginOutcome.InvalidCredentials
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
    override suspend fun submitTotpCode(code: String): Result<LoginOutcome> = resultOf {
        withContext(dispatcher) {
            when (webLoginApi.submitTotpCode(code.trim())) {
                WebTotpOutcome.InvalidCode -> LoginOutcome.InvalidTotpCode
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
        return LoginOutcome.Connected
    }

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
     * Only the fingerprint is pinned, not the certificate the prompt displayed (plan §4.5) — the
     * host scopes it, so a certificate accepted for one instance can't authenticate another.
     */
    override suspend fun trustCertificate(pendingCertTrust: PendingCertTrust): Result<Unit> = resultOf {
        withContext(dispatcher) {
            trustedCertStorage.trust(pendingCertTrust.host, pendingCertTrust.sha256Fingerprint)
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

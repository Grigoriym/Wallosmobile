package com.grappim.wallosmobile.feature.setup.domain.repo

import com.grappim.wallosmobile.feature.setup.domain.model.LoginOutcome

interface SetupRepository {

    /**
     * Runs the whole credential bridge (plan §1.1): drive the *web* login, scrape the API key off
     * `profile.php`, validate it against `api/status/version.php`, store it — and drop both the
     * session cookie and [password] on the way out. [serverUrl] is persisted first, since every
     * call in the chain is resolved against it.
     *
     * A [Result] failure here is a transport or instance problem — an unreachable host, a
     * [com.grappim.wallosmobile.core.domain.WallosError] from validation, or
     * [com.grappim.wallosmobile.feature.setup.domain.model.ApiKeyNotFound]. A rejected *credential*
     * is a success carrying [LoginOutcome.InvalidCredentials].
     */
    suspend fun loginWithPassword(serverUrl: String, username: String, password: String): Result<LoginOutcome>

    /**
     * Path B (plan §1.1): the user already has a key, so this is the tail of the bridge — persist
     * [serverUrl], validate [apiKey] against `api/status/version.php`, store it. No web session is
     * involved, so there is no [LoginOutcome] to report: a rejected key is a
     * [com.grappim.wallosmobile.core.domain.WallosError.Unauthenticated] failure, and an address
     * that isn't a Wallos instance is a `Malformed`/`UnsupportedEndpoint` one.
     *
     * This is the permanent recovery route, not a fallback: the bridge scrapes markup, and manual
     * entry is what still works the day that markup changes.
     */
    suspend fun connectWithApiKey(serverUrl: String, apiKey: String): Result<Unit>

    /**
     * The address of the last instance the app was pointed at, or `""` if there has never been
     * one. Disconnect clears the key alone (plan §4.7), so this survives it — which is the whole
     * point: the login screen offers the URL back instead of asking for it a second time.
     *
     * It goes through the repository rather than the storage seam because this feature has a real
     * data layer already; a `ui` module naming `core:storage` would be reaching past it.
     */
    suspend fun getStoredServerUrl(): Result<String>
}

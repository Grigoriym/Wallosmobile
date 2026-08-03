package com.grappim.wallosmobile.feature.setup.domain.repo

import com.grappim.wallosmobile.core.domain.PendingCertTrust
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
     * The second half of the bridge, for an account with 2FA on: answers the challenge
     * [loginWithPassword] came back from as [LoginOutcome.NeedsTotp], then runs the same
     * scrape-validate-store tail (API doc §9.2, §9.3).
     *
     * It **must** run on the web session that challenge was raised on, which is why there is no
     * `serverUrl` parameter and no way to call this out of order: the session belongs to this
     * repository instance, and this repository instance belongs to one login screen. A caller that
     * has not just been told [LoginOutcome.NeedsTotp] gets [LoginOutcome.TotpSessionExpired].
     */
    suspend fun submitTotpCode(code: String): Result<LoginOutcome>

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

    /**
     * Trust-on-first-use (plan §4.5): pins the certificate the user has just been shown, for the
     * host it was shown for. The connection attempt that raised [pendingCertTrust] has already
     * failed by then, so the caller retries it after this returns.
     *
     * A [Result] because the pin is a DataStore write and this is called from a `viewModelScope`
     * — the same reason [getStoredServerUrl] carries one. Routed through the repository for the
     * same reason too: `core:storage` is not this feature's `ui` module's to name.
     */
    suspend fun trustCertificate(pendingCertTrust: PendingCertTrust): Result<Unit>
}

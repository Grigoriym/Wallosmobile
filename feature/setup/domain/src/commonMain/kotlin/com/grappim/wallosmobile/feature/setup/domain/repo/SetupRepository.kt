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
}

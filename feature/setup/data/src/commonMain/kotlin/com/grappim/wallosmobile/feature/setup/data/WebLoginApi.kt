package com.grappim.wallosmobile.feature.setup.data

import com.grappim.wallosmobile.core.api.WebSessionHttpClient
import com.grappim.wallosmobile.feature.setup.domain.model.PasswordLoginAvailability
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.parameters
import org.koin.core.annotation.Factory

enum class WebLoginOutcome { LoggedIn, NeedsTotp, InvalidCredentials }

/**
 * `totp.php` has an outcome `login.php` has not: it refuses outright when the session it needs is
 * gone, and that is a different instruction to the user than a mistyped code.
 */
enum class WebTotpOutcome { LoggedIn, InvalidCode, SessionExpired }

/**
 * The *web* surface of Wallos, not its JSON API: HTML in, a redirect out. It exists only so
 * onboarding can hand a username and password over once and come back with the API key
 * (plan §1.1) — nothing else in the app ever touches it.
 */
interface WebLoginApi {

    /**
     * Whether this instance's `login.php` still carries the credential fields the bridge POSTs to
     * (plan §1.1). A GET, so it costs one request and reveals nothing.
     *
     * **It runs on the same session as [login] and [submitTotpCode], and `login.php` clears
     * `$_SESSION['totp_user_id']` on the way in** — read straight off the PHP, not off the API
     * doc. So probing between [login] and [submitTotpCode] would destroy the challenge that was
     * just raised, and the next code would come back as
     * [com.grappim.wallosmobile.feature.setup.domain.model.LoginOutcome.TotpSessionExpired]. The
     * caller is what keeps it away from that window.
     */
    suspend fun probePasswordLogin(): PasswordLoginAvailability

    suspend fun login(username: String, password: String): WebLoginOutcome

    /**
     * The second factor, on the session [login] opened (API doc §9.2). It is the same cookie jar
     * because it is the same instance of this class — see the note on [WebLoginApiImpl].
     */
    suspend fun submitTotpCode(code: String): WebTotpOutcome

    /** The API key off `profile.php`, or `null` if the page carried none. */
    suspend fun fetchApiKey(): String?
}

/**
 * A `@Factory` all the way down, matching the `@WebSessionHttpClient` it holds: the cookie jar
 * lives in that client, so binding it into a singleton would keep one PHP session alive for the
 * lifetime of the process.
 *
 * That does **not** make the session per-*call*, which is what TOTP would have broken: Koin
 * resolves a `@Factory` once per injection point, and the only injection point in this chain is
 * `LoginViewModel`'s constructor. So one cookie jar spans one login screen — long enough for a
 * human to read six digits off a phone, and no longer. Promoting anything in the chain to
 * `@Single` would silently widen that to the process (`CLAUDE.md`).
 */
@Factory(binds = [WebLoginApi::class])
internal class WebLoginApiImpl(@param:WebSessionHttpClient private val webClient: HttpClient) : WebLoginApi {

    override suspend fun probePasswordLogin(): PasswordLoginAvailability {
        val response = webClient.get(LOGIN_PATH)
        return interpretPasswordLoginAvailability(response.status.value, response.bodyAsText())
    }

    /**
     * `remember` is deliberately not sent (API doc §9.1): the session is thrown away as soon as
     * the key is in hand, so a longer-lived cookie would only widen the window in which one exists.
     */
    override suspend fun login(username: String, password: String): WebLoginOutcome {
        val response = webClient.submitForm(
            url = LOGIN_PATH,
            formParameters = parameters {
                append(PARAM_USERNAME, username)
                append(PARAM_PASSWORD, password)
            }
        )
        return interpretLoginOutcome(response.status.value, response.headers[HttpHeaders.Location])
    }

    override suspend fun submitTotpCode(code: String): WebTotpOutcome {
        val response = webClient.submitForm(
            url = TOTP_PATH,
            formParameters = parameters {
                append(PARAM_ONE_TIME_CODE, code)
            }
        )
        return interpretTotpOutcome(response.status.value, response.headers[HttpHeaders.Location])
    }

    override suspend fun fetchApiKey(): String? = scrapeApiKey(webClient.get(PROFILE_PATH).bodyAsText())

    private companion object {
        // Relative, no leading slash: a leading slash discards the subpath of an install that
        // lives under e.g. `/wallos` (plan §4.1).
        const val LOGIN_PATH = "login.php"
        const val TOTP_PATH = "totp.php"
        const val PROFILE_PATH = "profile.php"

        const val PARAM_USERNAME = "username"
        const val PARAM_PASSWORD = "password"

        // The name of the field in `totp.php`'s own form, hyphens and all.
        const val PARAM_ONE_TIME_CODE = "one-time-code"
    }
}

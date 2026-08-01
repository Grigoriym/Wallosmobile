package com.grappim.wallosmobile.feature.setup.data

import com.grappim.wallosmobile.core.api.WebSessionHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.parameters
import org.koin.core.annotation.Factory

enum class WebLoginOutcome { LoggedIn, NeedsTotp, InvalidCredentials }

/**
 * The *web* surface of Wallos, not its JSON API: HTML in, a redirect out. It exists only so
 * onboarding can hand a username and password over once and come back with the API key
 * (plan §1.1) — nothing else in the app ever touches it.
 */
interface WebLoginApi {

    suspend fun login(username: String, password: String): WebLoginOutcome

    /** The API key off `profile.php`, or `null` if the page carried none. */
    suspend fun fetchApiKey(): String?
}

/**
 * A `@Factory` all the way down, matching the `@WebSessionHttpClient` it holds: the cookie jar
 * lives in that client, so binding it into a singleton would keep one PHP session alive for the
 * lifetime of the process.
 */
@Factory(binds = [WebLoginApi::class])
internal class WebLoginApiImpl(@param:WebSessionHttpClient private val webClient: HttpClient) : WebLoginApi {

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

    override suspend fun fetchApiKey(): String? = scrapeApiKey(webClient.get(PROFILE_PATH).bodyAsText())

    private companion object {
        // Relative, no leading slash: a leading slash discards the subpath of an install that
        // lives under e.g. `/wallos` (plan §4.1).
        const val LOGIN_PATH = "login.php"
        const val PROFILE_PATH = "profile.php"

        const val PARAM_USERNAME = "username"
        const val PARAM_PASSWORD = "password"
    }
}

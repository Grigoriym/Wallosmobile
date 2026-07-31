package com.grappim.wallosmobile.core.api

import com.grappim.wallosmobile.core.appinfoapi.AppInfoProvider
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Qualifier
import org.koin.core.annotation.Single

/**
 * The onboarding-only client that speaks to the **web** surface (`login.php`, `profile.php`).
 * A `@Factory`, not a `@Single`: the session cookie should die with the onboarding attempt
 * rather than linger in a singleton — plan §1.1.
 */
@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class WebSessionHttpClient

@Module
@Configuration
@ComponentScan("com.grappim.wallosmobile.core.api")
class NetworkModule {

    /**
     * Note what is *absent*, versus MealieMobile's equivalent (plan §4.1): no `AuthHeaderPlugin`
     * (the credential is a body parameter), and no `ContentNegotiation` — responses are read as
     * text so [WallosEnvelopeParser] can strip PHP's `display_errors` prefix, and leaving the
     * plugin installed only invites someone to call `.body<T>()` and bypass the parser.
     * For the same reason there is no default JSON content type: Wallos wants form encoding.
     */
    @Single
    fun provideHttpClient(baseUrlProvider: BaseUrlProvider, appInfoProvider: AppInfoProvider): HttpClient = HttpClient {
        // Wallos answers 200 to auth and validation failures alike; the status carries no
        // API-level meaning, so raising on it would be raising on nothing.
        expectSuccess = false
        defaultRequest {
            url(baseUrlProvider.getBaseUrl())
        }
        install(Logging) {
            logger = RedactingLogger(tag = "Ktor")
            level = if (appInfoProvider.isDebug()) LogLevel.ALL else LogLevel.NONE
        }
        install(HttpRequestRetry) {
            retryOnExceptionIf(maxRetries = 1) { request, _ ->
                isReadEndpoint(request.url.encodedPathSegments)
            }
        }
    }

    /** No key injection, no envelope parsing — this one gets HTML and a `302`. */
    @[Factory WebSessionHttpClient]
    fun provideWebSessionHttpClient(baseUrlProvider: BaseUrlProvider, appInfoProvider: AppInfoProvider): HttpClient =
        HttpClient {
            // The redirect *is* the result: 302 means the credentials were accepted, and a
            // `Location` of `totp.php` means a second factor is pending (plan §1.1).
            followRedirects = false
            expectSuccess = false
            defaultRequest {
                url(baseUrlProvider.getBaseUrl())
            }
            install(HttpCookies) {
                storage = AcceptAllCookiesStorage()
            }
            install(Logging) {
                logger = RedactingLogger(tag = "KtorWeb")
                level = if (appInfoProvider.isDebug()) LogLevel.ALL else LogLevel.NONE
            }
        }
}

/**
 * Only `get_*.php` may be retried. `set_subscriptions.php` with `action=add` is a non-idempotent
 * POST: a retry after a *response*-side failure silently creates a duplicate subscription, so
 * MealieMobile's blanket `retryOnException` is a data-corruption bug here — plan §4.1.
 */
internal fun isReadEndpoint(pathSegments: List<String>): Boolean =
    pathSegments.lastOrNull().orEmpty().startsWith("get_")

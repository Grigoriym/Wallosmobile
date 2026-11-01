package com.grappim.wallosmobile.core.api

import io.ktor.http.HttpStatusCode
import io.ktor.http.parseUrl

/**
 * A response that came from whatever guards the server, not from Wallos (plan §4.5b).
 *
 * Wallos answers `200` to everything, auth failures included (plan §4.3), so an HTTP-level `401`
 * or `407` can only be a proxy asking for credentials. And Wallos only ever redirects *relatively*
 * (`.`, `totp.php`, `login.php`), so a redirect to an absolute URL on another host is a proxy
 * sending the app to its own login page.
 *
 * @param requestHost the host the redirected request was sent to — a redirect that stays on it
 * (an `http` → `https` upgrade, say) is not a login page.
 */
fun isProxyRejection(statusCode: Int, location: String?, requestHost: String): Boolean =
    statusCode == HttpStatusCode.Unauthorized.value ||
        statusCode == HttpStatusCode.ProxyAuthenticationRequired.value ||
        isRedirectOffHost(statusCode, location, requestHost)

private fun isRedirectOffHost(statusCode: Int, location: String?, requestHost: String): Boolean {
    if (statusCode !in HTTP_REDIRECT_RANGE || location == null) return false
    val isAbsolute = location.contains(SCHEME_SEPARATOR) || location.startsWith(PROTOCOL_RELATIVE)
    if (!isAbsolute) return false
    val target = parseUrl(if (location.startsWith(PROTOCOL_RELATIVE)) "http:$location" else location)
    return target != null && !target.host.equals(requestHost, ignoreCase = true)
}

private val HTTP_REDIRECT_RANGE = 300..399
private const val SCHEME_SEPARATOR = "://"
private const val PROTOCOL_RELATIVE = "//"

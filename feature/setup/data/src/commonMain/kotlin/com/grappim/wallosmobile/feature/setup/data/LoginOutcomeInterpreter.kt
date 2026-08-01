package com.grappim.wallosmobile.feature.setup.data

import io.ktor.http.HttpStatusCode

/**
 * `login.php` never returns JSON — the *redirect* is the result (API doc §9.2):
 *
 * | Response | Meaning |
 * |---|---|
 * | `302` + `Location: .` | logged in, `PHPSESSID` set |
 * | `302` + `Location: totp.php` | credentials fine, second factor pending |
 * | `200` + the login page HTML | rejected — and the reason is not machine-readable |
 *
 * Pure, so the table above is a unit test rather than a comment.
 */
internal fun interpretLoginOutcome(statusCode: Int, location: String?): WebLoginOutcome = when {
    statusCode != HttpStatusCode.Found.value -> WebLoginOutcome.InvalidCredentials
    location.orEmpty().contains(TOTP_PATH) -> WebLoginOutcome.NeedsTotp
    else -> WebLoginOutcome.LoggedIn
}

private const val TOTP_PATH = "totp.php"

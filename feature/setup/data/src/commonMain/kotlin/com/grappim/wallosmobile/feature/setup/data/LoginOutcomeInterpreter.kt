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

/**
 * `totp.php` answers on the same three-way shape, with one extra branch read off its source:
 *
 * | Response | Meaning |
 * |---|---|
 * | `302` + `Location: .` | the code (or a backup code) verified, the session is logged in |
 * | `302` + `Location: login.php` | the session no longer carries `totp_user_id` — start over |
 * | `200` + the totp page HTML | the code was rejected |
 *
 * The middle row is the one that matters: reported as a bad code, it would have the user typing
 * fresh digits at a session that can never accept any of them.
 */
internal fun interpretTotpOutcome(statusCode: Int, location: String?): WebTotpOutcome = when {
    statusCode != HttpStatusCode.Found.value -> WebTotpOutcome.InvalidCode
    location.orEmpty().contains(LOGIN_PATH) -> WebTotpOutcome.SessionExpired
    else -> WebTotpOutcome.LoggedIn
}

private const val TOTP_PATH = "totp.php"
private const val LOGIN_PATH = "login.php"

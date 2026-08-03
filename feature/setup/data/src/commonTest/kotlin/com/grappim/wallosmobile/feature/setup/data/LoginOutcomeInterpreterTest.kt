package com.grappim.wallosmobile.feature.setup.data

import kotlin.test.Test
import kotlin.test.assertEquals

/** The API doc §9.2 response table, as a test. */
class LoginOutcomeInterpreterTest {

    @Test
    fun `interprets every documented login response`() {
        val cases = listOf(
            // 302 to `.` is what a successful `login.php` actually sends.
            Triple(302, ".", WebLoginOutcome.LoggedIn),
            Triple(302, "totp.php", WebLoginOutcome.NeedsTotp),
            // Bad credentials and an unverified email are the same 200 + HTML.
            Triple(200, null, WebLoginOutcome.InvalidCredentials)
        )

        cases.forEach { (status, location, expected) ->
            assertEquals(expected, interpretLoginOutcome(status, location), "$status → $location")
        }
    }

    /** A subpath install redirects to `/wallos/totp.php`; the marker is the filename, not the path. */
    @Test
    fun `recognises the totp redirect under a subpath install`() {
        assertEquals(WebLoginOutcome.NeedsTotp, interpretLoginOutcome(302, "/wallos/totp.php"))
    }

    @Test
    fun `treats a 302 with no Location as logged in`() {
        assertEquals(WebLoginOutcome.LoggedIn, interpretLoginOutcome(302, null))
    }

    /** `totp.php`'s own three branches, read off its source rather than off the API doc. */
    @Test
    fun `interprets every documented totp response`() {
        val cases = listOf(
            Triple(302, ".", WebTotpOutcome.LoggedIn),
            // `totp.php` bounces here when the session has lost `totp_user_id`.
            Triple(302, "login.php", WebTotpOutcome.SessionExpired),
            // The page re-renders itself with an error box; nothing machine-readable in it.
            Triple(200, null, WebTotpOutcome.InvalidCode)
        )

        cases.forEach { (status, location, expected) ->
            assertEquals(expected, interpretTotpOutcome(status, location), "$status → $location")
        }
    }

    @Test
    fun `recognises the expired-session redirect under a subpath install`() {
        assertEquals(WebTotpOutcome.SessionExpired, interpretTotpOutcome(302, "/wallos/login.php"))
    }
}

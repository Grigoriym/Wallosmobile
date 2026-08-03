package com.grappim.wallosmobile.feature.setup.data

import com.grappim.wallosmobile.feature.setup.domain.model.PasswordLoginAvailability
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals

class PasswordLoginProbeTest {

    @Test
    fun `a form with the credential inputs can be driven`() {
        assertEquals(
            PasswordLoginAvailability.Available,
            interpretPasswordLoginAvailability(HttpStatusCode.OK.value, LOGIN_HTML)
        )
    }

    /** `login.php` wraps both inputs in `if (!$password_login_disabled)`, so the form loses them. */
    @Test
    fun `a form without a password input is an sso-only instance`() {
        assertEquals(
            PasswordLoginAvailability.Disabled,
            interpretPasswordLoginAvailability(HttpStatusCode.OK.value, LOGIN_HTML_SSO_ONLY)
        )
    }

    /**
     * The failure that would matter: reading any old 200 as `Disabled` hides the path that works
     * on a server that was never actually asked.
     */
    @Test
    fun `a page that is not the login form answers nothing`() {
        assertEquals(
            PasswordLoginAvailability.Unknown,
            interpretPasswordLoginAvailability(HttpStatusCode.OK.value, "<html><body>Bad Gateway</body></html>")
        )
        assertEquals(
            PasswordLoginAvailability.Unknown,
            interpretPasswordLoginAvailability(HttpStatusCode.OK.value, PROFILE_HTML)
        )
    }

    /**
     * All three `302`s `login.php` can answer with — an instance with no users yet, a session
     * already logged in, and `login_disabled` — and none of them is about passwords.
     */
    @Test
    fun `a redirect answers nothing either`() {
        assertEquals(
            PasswordLoginAvailability.Unknown,
            interpretPasswordLoginAvailability(HttpStatusCode.Found.value, "")
        )
    }
}

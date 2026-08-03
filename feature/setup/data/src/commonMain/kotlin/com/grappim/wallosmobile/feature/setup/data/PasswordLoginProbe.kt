package com.grappim.wallosmobile.feature.setup.data

import com.grappim.wallosmobile.feature.setup.domain.model.PasswordLoginAvailability
import io.ktor.http.HttpStatusCode

/**
 * Reads `login.php`'s own form to decide whether Path A can work here. Straight off the PHP, which
 * wraps both credential inputs in `if (!$password_login_disabled)`:
 *
 * ```html
 * <form action="login.php" method="post">
 *   <input type="text" id="username" name="username" autocomplete="username" required>
 *   <input type="password" id="password" name="password" autocomplete="current-password" required>
 * ```
 *
 * **The absence of the input is only meaningful on a page that is the login form.** A `200` from a
 * reverse proxy, a captive portal or some other app on that address carries no password input
 * either, and reading that as [PasswordLoginAvailability.Disabled] would hide the working path on
 * a server that was never asked. So the form itself has to be recognised first, and everything
 * unrecognised is [PasswordLoginAvailability.Unknown].
 *
 * A non-`200` is [PasswordLoginAvailability.Unknown] for the same reason and there are three ways
 * to get one, all `302`: no user has registered yet (`registration.php`), the session is already
 * logged in, or the admin turned login off entirely (`login_disabled`, which logs the caller
 * straight in as user 1). None of them says anything about passwords.
 */
internal fun interpretPasswordLoginAvailability(statusCode: Int, html: String): PasswordLoginAvailability = when {
    statusCode != HttpStatusCode.OK.value -> PasswordLoginAvailability.Unknown
    !LOGIN_FORM.containsMatchIn(html) -> PasswordLoginAvailability.Unknown
    PASSWORD_INPUT.containsMatchIn(html) -> PasswordLoginAvailability.Available
    else -> PasswordLoginAvailability.Disabled
}

private val LOGIN_FORM = Regex("""<form[^>]*action="login\.php"""")
private val PASSWORD_INPUT = Regex("""<input[^>]*name="password"""")

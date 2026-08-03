package com.grappim.wallosmobile.feature.setup.data

/**
 * Recorded markup, not invented markup: these are the shapes `profile.php` and `login.php` render
 * (API doc §9.2, §9.3), trimmed to the fragment the scraper walks. Kotlin constants rather than
 * test resources — `commonTest` has no portable way to read a file.
 */
internal const val SCRAPED_API_KEY = "b3f2c1a4d5e6f708192a3b4c5d6e7f80"

internal val PROFILE_HTML = """
    <!DOCTYPE html>
    <html lang="en">
    <body>
      <div class="settings-form">
        <label for="apikey">API Key</label>
        <div class="input-with-button">
          <input type="text" id="apikey" name="apikey" value="$SCRAPED_API_KEY" readonly>
          <button type="button" id="copy-apikey" class="button">Copy</button>
        </div>
        <input type="text" id="username" name="username" value="demo">
      </div>
    </body>
    </html>
""".trimIndent()

/** An account whose key has never been generated still renders the input, with an empty value. */
internal val PROFILE_HTML_WITHOUT_KEY = PROFILE_HTML.replace(SCRAPED_API_KEY, "")

/** What `profile.php` serves without a session: the login form, not the profile. */
internal val LOGIN_HTML = """
    <!DOCTYPE html>
    <html lang="en">
    <body>
      <form action="login.php" method="post">
        <input type="text" id="username" name="username" value="" required>
        <input type="password" id="password" name="password" required>
        <button type="submit">Login</button>
      </form>
    </body>
    </html>
""".trimIndent()

/**
 * The same form with `password_login_disabled` set: `login.php` wraps both credential inputs in
 * `if (!$password_login_disabled)`, leaving the OIDC link as the only way in. Per that same file
 * the flag can only be set when OIDC is enabled *and* configured, so this is what an SSO-only
 * instance renders — there is no variant with neither.
 */
internal val LOGIN_HTML_SSO_ONLY = """
    <!DOCTYPE html>
    <html lang="en">
    <body>
      <form action="login.php" method="post">
        <div class="form-group">
          <a class="button secondary-button" href="https://idp.example.com/authorize?response_type=code">
            Login with Keycloak
          </a>
        </div>
      </form>
    </body>
    </html>
""".trimIndent()

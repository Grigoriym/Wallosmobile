package com.grappim.wallosmobile.feature.setup.domain.model

/**
 * How a username/password onboarding attempt ended.
 *
 * Only [Connected] is terminal-good; the other two are not exceptions, because neither is a
 * failure of *ours* — they are answers the UI has to render as a field-level message.
 */
enum class LoginOutcome {

    /** The API key was scraped, validated against the instance and stored. */
    Connected,

    /**
     * The instance wants a second factor. v1 does not drive `totp.php` — the UI points the user
     * at manual key entry instead (plan §1.1).
     */
    NeedsTotp,

    /**
     * `login.php` re-rendered its own form. Wallos does not say *why* — a wrong password and an
     * unverified email are the same `200` with the same HTML (API doc §9.2).
     */
    InvalidCredentials
}

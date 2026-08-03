package com.grappim.wallosmobile.feature.setup.domain.model

/**
 * How a username/password onboarding attempt ended.
 *
 * Only [Connected] is terminal-good; the rest are not exceptions, because none is a failure of
 * *ours* — they are answers the UI has to render as a field-level message. Each of the three
 * refusals sends the user somewhere different, which is the only reason they are separate values.
 */
enum class LoginOutcome {

    /** The API key was scraped, validated against the instance and stored. */
    Connected,

    /**
     * The credentials were accepted and the instance wants a second factor. The session holding
     * that challenge lives on the screen's repository, so the next step is a code field on the
     * same screen (plan §1.1).
     */
    NeedsTotp,

    /**
     * `login.php` re-rendered its own form. Wallos does not say *why* — a wrong password and an
     * unverified email are the same `200` with the same HTML (API doc §9.2).
     */
    InvalidCredentials,

    /** `totp.php` rejected the code. The challenge stands; the next code can still work. */
    InvalidTotpCode,

    /**
     * `totp.php` bounced back to `login.php`, which it does when the session has lost
     * `totp_user_id`. No code can complete this attempt — the credentials have to go again.
     */
    TotpSessionExpired
}

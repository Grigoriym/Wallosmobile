package com.grappim.wallosmobile.feature.setup.domain.model

/**
 * Whether this instance will accept a username and password at all (plan §1.1, API doc §9.5).
 *
 * `password_login_disabled` — an admin setting, or the `OIDC_DISABLE_PASSWORD_LOGIN` env var —
 * strips the credential fields out of `login.php`'s form, and OIDC itself cannot be bridged: it is
 * a redirect dance against a third-party IdP whose registered `redirect_url` points at the Wallos
 * web app. So there is nothing for Path A to drive on such an instance, and the only useful thing
 * to do is say so before the user types a password that cannot work.
 *
 * Read off `login.php` rather than off a setting endpoint, because the form *is* the contract the
 * bridge drives.
 */
enum class PasswordLoginAvailability {

    /** The form carries the credential inputs — the bridge has something to POST to. */
    Available,

    /**
     * The form is Wallos's own and has no password input. Per `login.php`, that only happens when
     * OIDC is enabled *and* configured, so this doubles as "this instance signs in through SSO".
     */
    Disabled,

    /**
     * The probe could not tell — an unreachable host, a redirect instead of the form, or a page
     * that isn't `login.php` at all. Deliberately **not** collapsed into either answer: this is an
     * affordance, and guessing [Disabled] would hide the path that actually works.
     */
    Unknown
}

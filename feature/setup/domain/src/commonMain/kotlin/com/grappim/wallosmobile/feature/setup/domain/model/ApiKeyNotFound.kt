package com.grappim.wallosmobile.feature.setup.domain.model

/**
 * The session was valid and `profile.php` rendered, but it carried no `id="apikey"` input.
 *
 * `id="apikey"` is markup, not an API contract, so an upstream redesign breaks onboarding without
 * breaking anything else (API doc §9.5). That is precisely the case manual key entry exists to
 * recover from, so this is worth its own type rather than a generic failure.
 */
data object ApiKeyNotFound : Exception()

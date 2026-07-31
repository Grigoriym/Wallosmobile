package com.grappim.wallosmobile.core.api

/**
 * The **instance root**, not an `/api/` prefix: the login bridge hits `/login.php` and
 * `/profile.php` at the root while the JSON API lives under `api/…` — plan §4.1.
 */
interface BaseUrlProvider {
    fun getBaseUrl(): String
}

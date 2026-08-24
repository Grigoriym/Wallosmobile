package com.grappim.wallosmobile.feature.dashboard.ui

import com.grappim.wallosmobile.core.api.BaseUrlProvider

/** The directory Wallos writes uploaded logos to, under the instance root (API doc §4). */
private const val LOGO_PATH = "images/uploads/logos/"

/**
 * Mirrors `feature:subscriptions:ui`'s own `toLogoUrl` — duplicated rather than shared the same
 * way `feature:paymentmethods:ui`'s `toIconUrl` is (precedent: neither of those two shares with
 * the other either). The wire carries a bare filename, so the full URL only exists where the
 * instance root does, which is wherever `BaseUrlProvider` is injected.
 *
 * Blank in, blank out: no logo, or no stored server, yields no URL rather than a relative one the
 * image loader would fail on.
 */
internal fun BaseUrlProvider.toLogoUrl(logo: String): String {
    val baseUrl = getBaseUrl()
    return if (logo.isBlank() || baseUrl.isBlank()) "" else "$baseUrl$LOGO_PATH$logo"
}

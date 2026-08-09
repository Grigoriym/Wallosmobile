package com.grappim.wallosmobile.feature.paymentmethods.ui

import com.grappim.wallosmobile.core.api.BaseUrlProvider

/**
 * Unlike a subscription logo's bare filename, `PaymentMethod.icon` already carries its directory
 * relative to the instance root (`WALLOS_API.md` §4) — so the full URL is just the root in front
 * of it, no path segment to insert.
 *
 * Blank in, blank out: no icon, or no stored server, yields no URL rather than a relative one the
 * image loader would fail on.
 */
internal fun BaseUrlProvider.toIconUrl(icon: String): String {
    val baseUrl = getBaseUrl()
    return if (icon.isBlank() || baseUrl.isBlank()) "" else "$baseUrl$icon"
}

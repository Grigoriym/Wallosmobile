package com.grappim.wallosmobile.feature.subscriptions.ui

import com.grappim.wallosmobile.core.api.BaseUrlProvider

/** The directory Wallos writes uploaded logos to, under the instance root (API doc §4). */
private const val LOGO_PATH = "images/uploads/logos/"

/**
 * The wire carries a bare filename, so the full URL only exists where the instance root does —
 * which is the ViewModel, `BaseUrlProvider` being the one place that root is normalized (plan
 * §7.1). The base URL already carries its trailing slash (`BaseUrlProviderImpl`).
 *
 * Blank in, blank out: no logo, or no stored server, yields no URL rather than a relative one the
 * image loader would fail on. A blank root means the app got here without a stored server, which
 * the startup branch rules out — but a broken image request is worse than none, so it is guarded.
 */
internal fun BaseUrlProvider.toLogoUrl(logo: String): String {
    val baseUrl = getBaseUrl()
    return if (logo.isBlank() || baseUrl.isBlank()) "" else "$baseUrl$LOGO_PATH$logo"
}

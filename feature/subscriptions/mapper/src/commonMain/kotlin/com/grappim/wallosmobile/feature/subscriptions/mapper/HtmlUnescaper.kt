package com.grappim.wallosmobile.feature.subscriptions.mapper

import org.koin.core.annotation.Single

/**
 * Undoes PHP's `htmlspecialchars`, which is what Wallos stored the text with — `1&1 Telekom`
 * comes back off the wire as `1&amp;1 Telekom` and renders as the entity unless someone reverses
 * it (API doc §3.1, verified on the live instance in 2.1).
 *
 * Only the five entities `htmlspecialchars` produces under PHP's default
 * `ENT_QUOTES | ENT_HTML401`, plus the unpadded `&#39;` older PHP wrote. This is deliberately not
 * a general HTML decoder: anything else in the text is text.
 */
@Single
class HtmlUnescaper {

    fun unescape(text: String): String = if (text.contains(AMPERSAND)) {
        text.replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&#39;", "'")
            // Last, always: `&amp;lt;` is the *text* `&lt;`, and decoding the ampersand first
            // would turn it into a `<`.
            .replace("&amp;", "&")
    } else {
        text
    }

    private companion object {
        const val AMPERSAND = '&'
    }
}

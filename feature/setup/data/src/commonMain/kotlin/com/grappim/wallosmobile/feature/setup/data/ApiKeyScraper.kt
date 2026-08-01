package com.grappim.wallosmobile.feature.setup.data

/**
 * `profile.php` renders the key into a readonly input (API doc §9.3):
 *
 * ```html
 * <input type="text" id="apikey" name="apikey" value="THE_KEY_IS_HERE" readonly>
 * ```
 *
 * Returns `null` when the input isn't there — a logged-out page, or upstream markup that moved.
 * A blank `value` counts as absent: an account whose key has never been generated renders the
 * input empty, and storing `""` would look connected while failing every call.
 */
internal fun scrapeApiKey(profileHtml: String): String? =
    API_KEY_INPUT.find(profileHtml)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }

private val API_KEY_INPUT = Regex("""id="apikey"[^>]*value="([^"]*)"""")

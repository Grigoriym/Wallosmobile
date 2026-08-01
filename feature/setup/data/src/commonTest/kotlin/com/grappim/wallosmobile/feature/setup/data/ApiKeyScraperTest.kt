package com.grappim.wallosmobile.feature.setup.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ApiKeyScraperTest {

    @Test
    fun `pulls the key out of the profile page`() {
        assertEquals(SCRAPED_API_KEY, scrapeApiKey(PROFILE_HTML))
    }

    /**
     * The very next input in the page carries a `value` of its own, so a match that ran past the
     * closing `>` would store the username as the API key.
     */
    @Test
    fun `does not spill into the next input when the apikey tag has no value`() {
        val html = PROFILE_HTML.replace("""value="$SCRAPED_API_KEY" readonly""", "readonly")

        assertNull(scrapeApiKey(html))
    }

    /**
     * An empty `value` is not a key. Storing `""` would leave the app looking connected while
     * every call answered `Missing API key`.
     */
    @Test
    fun `reads an empty value as no key at all`() {
        assertNull(scrapeApiKey(PROFILE_HTML_WITHOUT_KEY))
    }

    /** Session expired between login and scrape: `profile.php` serves the login form instead. */
    @Test
    fun `returns null when the page is not the profile page`() {
        assertNull(scrapeApiKey(LOGIN_HTML))
    }
}

package com.grappim.wallosmobile.feature.subscriptions.mapper

import kotlin.test.Test
import kotlin.test.assertEquals

class HtmlUnescaperTest {

    private val unescaper = HtmlUnescaper()

    @Test
    fun `decodes the ampersand the live instance actually sends`() {
        assertEquals("1&1 Telekom", unescaper.unescape("1&amp;1 Telekom"))
    }

    @Test
    fun `decodes every entity htmlspecialchars produces`() {
        assertEquals(
            "<a href=\"x\"> & 'quoted'",
            unescaper.unescape("&lt;a href=&quot;x&quot;&gt; &amp; &#039;quoted&#039;")
        )
    }

    @Test
    fun `decodes the unpadded apostrophe older PHP wrote`() {
        assertEquals("Bob's", unescaper.unescape("Bob&#39;s"))
    }

    @Test
    fun `decodes the ampersand last, so a double-escaped entity stays text`() {
        // The user typed the literal string `&lt;`; the server escaped its ampersand. Decoding
        // `&amp;` first would turn this into a `<`, which the user never wrote.
        assertEquals("&lt;", unescaper.unescape("&amp;lt;"))
    }

    @Test
    fun `leaves text without entities alone`() {
        assertEquals("Disney+", unescaper.unescape("Disney+"))
        assertEquals("", unescaper.unescape(""))
    }

    @Test
    fun `leaves an entity this app does not decode as it is`() {
        assertEquals("caf&eacute;", unescaper.unescape("caf&eacute;"))
    }
}

package com.grappim.wallosmobile.core.api

import io.ktor.http.URLBuilder
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NetworkModuleTest {

    @Test
    fun `retries a read`() {
        assertTrue(isReadEndpoint(segmentsOf("https://example.com/api/subscriptions/get_subscriptions.php")))
    }

    @Test
    fun `retries a read on a subpath install`() {
        assertTrue(isReadEndpoint(segmentsOf("https://example.com/wallos/api/status/get_user.php")))
    }

    @Test
    fun `never retries a write, which would duplicate the row`() {
        assertFalse(isReadEndpoint(segmentsOf("https://example.com/api/subscriptions/set_subscriptions.php")))
    }

    @Test
    fun `is not fooled by a get_ directory earlier in the path`() {
        assertFalse(isReadEndpoint(segmentsOf("https://example.com/get_things/set_budget.php")))
    }

    @Test
    fun `does not retry the web login bridge`() {
        assertFalse(isReadEndpoint(segmentsOf("https://example.com/login.php")))
    }

    private fun segmentsOf(url: String): List<String> = URLBuilder(url).encodedPathSegments
}

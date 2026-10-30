package com.grappim.wallosmobile.core.api

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProxyRejectionTest {

    @Test
    fun `401 and 407 are a proxy whatever they carry`() {
        assertTrue(isProxyRejection(401, null, HOST))
        assertTrue(isProxyRejection(407, null, HOST))
    }

    @Test
    fun `a redirect to an absolute URL on another host is a proxy login page`() {
        assertTrue(isProxyRejection(302, "https://auth.example.com/login?redirect=x", HOST))
        assertTrue(isProxyRejection(307, "//auth.example.com/login", HOST))
    }

    /** Every redirect Wallos itself sends is relative — `.`, `totp.php`, `login.php`. */
    @Test
    fun `Wallos's own relative redirects are not a proxy`() {
        listOf(".", "totp.php", "login.php", "/wallos/totp.php").forEach { location ->
            assertFalse(isProxyRejection(302, location, HOST), location)
        }
    }

    @Test
    fun `an absolute redirect that stays on the server's host is not a proxy`() {
        assertFalse(isProxyRejection(301, "https://WALLOS.example.com/login.php", HOST))
    }

    @Test
    fun `a non-redirect status ignores its Location`() {
        assertFalse(isProxyRejection(200, "https://auth.example.com/login", HOST))
        assertFalse(isProxyRejection(302, null, HOST))
    }

    private companion object {
        const val HOST = "wallos.example.com"
    }
}

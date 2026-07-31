package com.grappim.wallosmobile.core.api

import com.grappim.wallosmobile.core.storage.ServerUrlStorage
import kotlin.test.Test
import kotlin.test.assertEquals

class BaseUrlProviderImplTest {

    @Test
    fun `appends the trailing slash Ktor needs to resolve relative paths`() {
        assertEquals("https://wallos.example.com/", baseUrlOf("https://wallos.example.com"))
    }

    @Test
    fun `keeps a subpath install intact`() {
        assertEquals("https://example.com/wallos/", baseUrlOf("https://example.com/wallos"))
    }

    @Test
    fun `collapses repeated trailing slashes`() {
        assertEquals("https://example.com/wallos/", baseUrlOf("https://example.com/wallos///"))
    }

    @Test
    fun `trims surrounding whitespace, which pasted URLs carry`() {
        assertEquals("https://example.com/", baseUrlOf("  https://example.com/  "))
    }

    @Test
    fun `leaves an unconfigured server as the empty string rather than a bare slash`() {
        assertEquals("", baseUrlOf("   "))
    }

    private fun baseUrlOf(stored: String): String = BaseUrlProviderImpl(FakeServerUrlStorage(stored)).getBaseUrl()

    private class FakeServerUrlStorage(override val serverUrl: String) : ServerUrlStorage
}

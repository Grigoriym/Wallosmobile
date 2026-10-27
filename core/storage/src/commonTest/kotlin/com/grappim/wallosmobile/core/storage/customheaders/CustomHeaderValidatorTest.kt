package com.grappim.wallosmobile.core.storage.customheaders

import kotlin.test.Test
import kotlin.test.assertEquals

class CustomHeaderValidatorTest {

    private val sut = CustomHeaderValidator()

    private fun errorFor(name: String, value: String = "x"): CustomHeaderError? =
        sut.validate(listOf(CustomHeader(name, value))).single()

    @Test
    fun `the headers proxies actually ask for are accepted`() {
        val headers = listOf(
            CustomHeader("Authorization", "Basic cGFuZ29saW46c2VjcmV0"),
            CustomHeader("CF-Access-Client-Id", "abc.access"),
            CustomHeader("X-Token", "a b\tc")
        )

        assertEquals(listOf(null, null, null), sut.validate(headers))
    }

    @Test
    fun `an unfilled row is not an error`() {
        assertEquals(listOf(null), sut.validate(listOf(CustomHeader(" ", ""))))
    }

    @Test
    fun `a value without a name is missing its name`() {
        assertEquals(CustomHeaderError.MissingName, errorFor(name = ""))
    }

    @Test
    fun `names refuse whitespace, delimiters and non-ascii`() {
        listOf("X Token", "X:Token", "X(Token)", "Tökén", "X/Token").forEach { name ->
            assertEquals(CustomHeaderError.InvalidName, errorFor(name), name)
        }
    }

    @Test
    fun `names the stack or the web login set themselves are refused, ignoring case`() {
        listOf("Host", "content-length", "Content-Type", "transfer-encoding", "Upgrade", "Connection", "COOKIE")
            .forEach { name -> assertEquals(CustomHeaderError.ReservedName, errorFor(name), name) }
    }

    /** OkHttp throws on non-ASCII at request time, so it has to be caught here. */
    @Test
    fun `values refuse control characters and non-ascii`() {
        listOf("a\nb", "a\u0000b", "pässword").forEach { value ->
            assertEquals(CustomHeaderError.InvalidValue, errorFor("X-Token", value), value)
        }
    }

    @Test
    fun `an empty value is allowed`() {
        assertEquals(null, errorFor("X-Token", ""))
    }

    @Test
    fun `a repeated name is flagged on the repeat, ignoring case`() {
        val headers = listOf(CustomHeader("X-Token", "a"), CustomHeader("x-token", "b"))

        assertEquals(listOf(null, CustomHeaderError.DuplicateName), sut.validate(headers))
    }
}

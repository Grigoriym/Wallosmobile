package com.grappim.wallosmobile.core.api

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class FormParamsTest {

    @Test
    fun `put keeps the value verbatim`() {
        val params = FormParams().put("action", "add")

        assertEquals(mapOf("action" to "add"), params.asMap())
    }

    @Test
    fun `flag encodes 1 and 0, never true and false`() {
        val params = FormParams()
            .flag("notify", true)
            .flag("inactive", false)

        assertEquals(mapOf("notify" to "1", "inactive" to "0"), params.asMap())
    }

    @Test
    fun `literalTrue encodes the literal string true`() {
        val params = FormParams()
            .literalTrue("convert_currency", true)
            .literalTrue("disabled_to_bottom", false)

        assertEquals(
            mapOf("convert_currency" to "true", "disabled_to_bottom" to "false"),
            params.asMap()
        )
    }

    @Test
    fun `date encodes zero-padded YYYY-MM-DD`() {
        val params = FormParams().date("next_payment", LocalDate(2025, 3, 7))

        assertEquals(mapOf("next_payment" to "2025-03-07"), params.asMap())
    }

    @Test
    fun `the last value written for a key wins`() {
        val params = FormParams()
            .flag("notify", true)
            .flag("notify", false)

        assertEquals(mapOf("notify" to "0"), params.asMap())
    }

    @Test
    fun `asMap is a snapshot, not a view`() {
        val params = FormParams().put("action", "add")

        val snapshot = params.asMap()
        params.put("name", "Netflix")

        assertEquals(mapOf("action" to "add"), snapshot)
    }
}

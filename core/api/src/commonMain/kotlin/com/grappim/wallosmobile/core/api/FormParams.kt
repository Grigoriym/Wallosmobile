package com.grappim.wallosmobile.core.api

import kotlinx.datetime.LocalDate
import kotlinx.datetime.format

/**
 * The body of a Wallos form POST — every endpoint takes `application/x-www-form-urlencoded`.
 *
 * The named setters exist because `Boolean.toString()` is never what this API wants and a
 * hand-formatted date is a silent `Invalid date` waiting to happen. Encoding each quirk once,
 * here, makes it unrepresentable at the call sites — plan §4.4.
 */
class FormParams {

    private val values = mutableMapOf<String, String>()

    fun put(key: String, value: String): FormParams = apply { values[key] = value }

    /** `"1"` / `"0"` — `notify`, `inactive`, `auto_renew`, and every other toggle. */
    fun flag(key: String, value: Boolean): FormParams = put(key, if (value) "1" else "0")

    /**
     * The literal string `"true"` — `convert_currency` and `disabled_to_bottom` compare against
     * it and read anything else as false.
     */
    fun literalTrue(key: String, value: Boolean): FormParams = put(key, if (value) "true" else "false")

    /** Strict `YYYY-MM-DD` — `next_payment`, `start_date`, `cancellation_date`. */
    fun date(key: String, value: LocalDate): FormParams = put(key, value.format(LocalDate.Formats.ISO))

    fun asMap(): Map<String, String> = values.toMap()
}

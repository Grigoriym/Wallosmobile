package com.grappim.wallosmobile.utils.formatter.decimal

import org.koin.core.annotation.Single
import kotlin.math.abs
import kotlin.math.floor

private const val CENTS_PER_UNIT = 100
private const val HALF = 0.5
private const val FRACTION_DIGITS = 2
private const val GROUP_SIZE = 3
private const val GROUP_SEPARATOR = ","
private const val DECIMAL_SEPARATOR = "."

/**
 * Money in and out of Wallos, in the one shape the server itself speaks.
 *
 * The grouping and the decimal point are **fixed** (`1,234.56`), not taken from the device
 * locale, because that is what the instance sends back: `monthly_cost` is PHP `number_format`
 * with its defaults, and `localized_monthly_cost` is `en_US` "regardless of the user's language"
 * (API doc §3.5). Formatting a price the device's way would make the app disagree with every
 * total the same server renders. A locale-aware formatter is possible later — it needs
 * `expect`/`actual` over the platform `NumberFormat`, which also puts it out of reach of a host
 * test — and is not what v1 wants.
 *
 * The symbol is whatever the instance stores for the currency (`€`, `$`, `zł`), so it is passed
 * in rather than derived from a currency code: Wallos lets it be any string (API doc §3.6).
 */
@Single
class MoneyFormatter {

    /**
     * `12.5` + `"€"` → `"€12.50"`. Always two decimals, half-up, sign ahead of the symbol.
     */
    fun format(amount: Double, symbol: String): String {
        // Half-up, matching `number_format`. `kotlin.math.round` breaks ties towards the *even*
        // integer, which would render an exact 0.125 as 0.12 while the server says 0.13.
        val cents = floor(abs(amount) * CENTS_PER_UNIT + HALF).toLong()
        val whole = groupThousands((cents / CENTS_PER_UNIT).toString())
        val fraction = (cents % CENTS_PER_UNIT).toString().padStart(FRACTION_DIGITS, '0')
        val sign = if (amount < 0) "-" else ""
        return "$sign$symbol$whole$DECIMAL_SEPARATOR$fraction"
    }

    /**
     * The inverse, for the fields Wallos sends pre-formatted — `monthly_cost` is a *string* with
     * thousands separators in it, and `toDouble()` on it throws (API doc §7.4).
     *
     * `null` for anything that isn't a number, blank included: a total the app can't read is a
     * total it must not display as `0`.
     */
    fun parse(text: String): Double? = text.replace(GROUP_SEPARATOR, "").trim().toDoubleOrNull()

    private fun groupThousands(digits: String): String = digits
        .reversed()
        .chunked(GROUP_SIZE)
        .joinToString(GROUP_SEPARATOR)
        .reversed()
}

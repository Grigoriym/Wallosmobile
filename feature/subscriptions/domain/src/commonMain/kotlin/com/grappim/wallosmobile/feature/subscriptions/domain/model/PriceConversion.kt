package com.grappim.wallosmobile.feature.subscriptions.domain.model

/**
 * Whether the prices in the cache are the instance's own or converted into its main currency —
 * the state 3.11 exists to make visible.
 *
 * Wallos will not tell you. `get_subscriptions.php` overwrites `price` when it converts and leaves
 * `currency_id` naming the currency it converted *from*, so a converted row is indistinguishable
 * from an unconverted one in the response. (API doc §5.5 suggests comparing `currency_id` against
 * `main_currency` to detect this; that comparison says conversion was *attempted*, never that it
 * happened.) These three facts, gathered from `get_settings.php` and `get_currencies.php` in the
 * same refresh, are the only thing that decides it — hence [converts], which the repository asks
 * before choosing the symbol a price is stored with.
 *
 * The default is every answer that cannot mislabel a price: nothing converted, in the instance's
 * own currencies.
 *
 * @param isEnabled the user's own `convert_currency` setting on their instance. Honoured rather
 *   than overridden — a user who turned conversion off is asking to see real currencies.
 * @param mainCurrencyId what conversion converts into, `null` when the instance didn't say.
 * @param hasRates whether exchange rates have ever been fetched. Wallos silently skips conversion
 *   without them and puts **nothing** in `notes` (API doc §5.5); the flag no endpoint exposes is
 *   `last_exchange_update`, so this stands in for it — see the repository for how it is read.
 */
data class PriceConversion(
    val isEnabled: Boolean = false,
    val mainCurrencyId: Int? = null,
    val hasRates: Boolean = false
) {

    /** Asked for and actually happening, so a price fetched under it is in [mainCurrencyId]. */
    val isActive: Boolean
        get() = isEnabled && hasRates && mainCurrencyId != null

    /**
     * The silent failure itself: the instance was asked to convert and cannot. Prices are correct
     * and in their own currencies — nothing is wrong with them — but a user who expected one
     * currency is looking at several, and no response said so.
     */
    val isEnabledWithoutRates: Boolean
        get() = isEnabled && !hasRates

    /**
     * Whether a price fetched for [currencyId] came back converted. A row already in the main
     * currency is left alone by the server, so it keeps its own symbol either way.
     */
    fun converts(currencyId: Int): Boolean = isActive && currencyId != mainCurrencyId
}

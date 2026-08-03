package com.grappim.wallosmobile.feature.subscriptions.data

import com.grappim.wallosmobile.core.api.FormParams
import com.grappim.wallosmobile.core.api.WallosApiClient
import com.grappim.wallosmobile.core.api.post
import com.grappim.wallosmobile.feature.subscriptions.dto.CurrenciesResponse
import com.grappim.wallosmobile.feature.subscriptions.dto.CurrencyDTO
import com.grappim.wallosmobile.feature.subscriptions.dto.SettingsResponse
import com.grappim.wallosmobile.feature.subscriptions.dto.SubscriptionDTO
import com.grappim.wallosmobile.feature.subscriptions.dto.SubscriptionResponse
import com.grappim.wallosmobile.feature.subscriptions.dto.SubscriptionsResponse
import org.koin.core.annotation.Single

/**
 * The four read endpoints v1 needs (API doc §3.2, §3.3, §3.10, §3.11). Unwrapping the envelope is
 * the whole job — everything above this works in DTOs, not responses.
 */
interface SubscriptionsApi {

    suspend fun getSubscriptions(convertCurrency: Boolean): List<SubscriptionDTO>

    suspend fun getSubscription(id: Int, convertCurrency: Boolean): SubscriptionDTO

    suspend fun getCurrencies(): CurrenciesPayload

    /** The user's own `convert_currency` setting, `false` on an instance that doesn't send one. */
    suspend fun isCurrencyConversionEnabled(): Boolean
}

/**
 * `get_currencies.php`'s two halves. The list alone is no longer enough (3.11): which of these a
 * converted price is denominated in is only in the envelope's `main_currency`, so the two travel
 * together rather than through a second call.
 */
data class CurrenciesPayload(val currencies: List<CurrencyDTO>, val mainCurrencyId: Int?)

@Single(binds = [SubscriptionsApi::class])
internal class SubscriptionsApiImpl(private val apiClient: WallosApiClient) : SubscriptionsApi {

    /**
     * `api_key`, and `convert_currency` when the instance asks for it. Filtering and sorting stay
     * client-side (API doc §7), and the server's default sort is already `next_payment` — which is
     * the order the list wants.
     *
     * Sending no filters is also what keeps the §3.2 `all-user-subscription` bug unreachable:
     * combining that flag with `member`/`category`/`payment`/`state` builds
     * `SELECT * FROM subscriptions AND …`, which fails to prepare. Neither side of that
     * combination exists here, and a filter added later has to keep it that way.
     */
    override suspend fun getSubscriptions(convertCurrency: Boolean): List<SubscriptionDTO> =
        apiClient.post<SubscriptionsResponse>(
            SUBSCRIPTIONS_PATH,
            conversionParams(convertCurrency)
        ).subscriptions

    override suspend fun getSubscription(id: Int, convertCurrency: Boolean): SubscriptionDTO =
        apiClient.post<SubscriptionResponse>(
            SUBSCRIPTION_PATH,
            conversionParams(convertCurrency).put(PARAM_ID, id.toString())
        ).subscription

    override suspend fun getCurrencies(): CurrenciesPayload =
        apiClient.post<CurrenciesResponse>(CURRENCIES_PATH).let { response ->
            CurrenciesPayload(currencies = response.currencies, mainCurrencyId = response.mainCurrency)
        }

    override suspend fun isCurrencyConversionEnabled(): Boolean =
        apiClient.post<SettingsResponse>(SETTINGS_PATH).settings.convertCurrency == ENABLED

    /**
     * Omitted rather than sent as `"false"` when off, the same way `withApiKey(null)` drops the key
     * instead of sending a blank one (1.3): the server compares against the literal string `"true"`
     * and everything else — including absence — is false, so the shorter body says the same thing.
     */
    private fun conversionParams(convertCurrency: Boolean): FormParams = FormParams().apply {
        if (convertCurrency) {
            literalTrue(PARAM_CONVERT_CURRENCY, true)
        }
    }

    private companion object {
        // Relative, no leading slash — a leading one discards the subpath of an install that
        // lives under e.g. `/wallos` (plan §4.1).
        const val SUBSCRIPTIONS_PATH = "api/subscriptions/get_subscriptions.php"
        const val SUBSCRIPTION_PATH = "api/subscriptions/get_subscription.php"
        const val CURRENCIES_PATH = "api/currencies/get_currencies.php"
        const val SETTINGS_PATH = "api/settings/get_settings.php"

        const val PARAM_ID = "id"
        const val PARAM_CONVERT_CURRENCY = "convert_currency"

        const val ENABLED = 1
    }
}

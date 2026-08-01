package com.grappim.wallosmobile.feature.subscriptions.data

import com.grappim.wallosmobile.core.api.FormParams
import com.grappim.wallosmobile.core.api.WallosApiClient
import com.grappim.wallosmobile.core.api.post
import com.grappim.wallosmobile.feature.subscriptions.dto.CurrenciesResponse
import com.grappim.wallosmobile.feature.subscriptions.dto.CurrencyDTO
import com.grappim.wallosmobile.feature.subscriptions.dto.SubscriptionDTO
import com.grappim.wallosmobile.feature.subscriptions.dto.SubscriptionResponse
import com.grappim.wallosmobile.feature.subscriptions.dto.SubscriptionsResponse
import org.koin.core.annotation.Single

/**
 * The three read endpoints v1 needs (API doc §3.2, §3.3, §3.10). Unwrapping the envelope is the
 * whole job — everything above this works in DTOs, not responses.
 */
interface SubscriptionsApi {

    suspend fun getSubscriptions(): List<SubscriptionDTO>

    suspend fun getSubscription(id: Int): SubscriptionDTO

    suspend fun getCurrencies(): List<CurrencyDTO>
}

@Single(binds = [SubscriptionsApi::class])
internal class SubscriptionsApiImpl(private val apiClient: WallosApiClient) : SubscriptionsApi {

    /**
     * `api_key` and nothing else. Filtering and sorting are client-side (API doc §7), and the
     * server's default sort is already `next_payment` — which is the order the list wants.
     *
     * Sending no filters is also what keeps the §3.2 `all-user-subscription` bug unreachable:
     * combining that flag with `member`/`category`/`payment`/`state` builds
     * `SELECT * FROM subscriptions AND …`, which fails to prepare. Neither side of that
     * combination exists here, and a filter added later has to keep it that way.
     */
    override suspend fun getSubscriptions(): List<SubscriptionDTO> =
        apiClient.post<SubscriptionsResponse>(SUBSCRIPTIONS_PATH).subscriptions

    override suspend fun getSubscription(id: Int): SubscriptionDTO = apiClient.post<SubscriptionResponse>(
        SUBSCRIPTION_PATH,
        FormParams().put(PARAM_ID, id.toString())
    ).subscription

    override suspend fun getCurrencies(): List<CurrencyDTO> =
        apiClient.post<CurrenciesResponse>(CURRENCIES_PATH).currencies

    private companion object {
        // Relative, no leading slash — a leading one discards the subpath of an install that
        // lives under e.g. `/wallos` (plan §4.1).
        const val SUBSCRIPTIONS_PATH = "api/subscriptions/get_subscriptions.php"
        const val SUBSCRIPTION_PATH = "api/subscriptions/get_subscription.php"
        const val CURRENCIES_PATH = "api/currencies/get_currencies.php"

        const val PARAM_ID = "id"
    }
}

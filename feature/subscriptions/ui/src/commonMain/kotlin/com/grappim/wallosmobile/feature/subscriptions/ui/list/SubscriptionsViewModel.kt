package com.grappim.wallosmobile.feature.subscriptions.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.wallosmobile.core.api.BaseUrlProvider
import com.grappim.wallosmobile.core.logger.LogPriority
import com.grappim.wallosmobile.core.logger.logcat
import com.grappim.wallosmobile.feature.subscriptions.domain.model.Subscription
import com.grappim.wallosmobile.feature.subscriptions.domain.repo.SubscriptionsRepository
import com.grappim.wallosmobile.utils.formatter.datetime.DateFormatter
import com.grappim.wallosmobile.utils.formatter.decimal.MoneyFormatter
import com.grappim.wallosmobile.utils.ui.NativeText
import com.grappim.wallosmobile.utils.ui.getErrorMessage
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

/** The directory Wallos writes uploaded logos to, under the instance root (API doc §4). */
private const val LOGO_PATH = "images/uploads/logos/"

/**
 * Fetch-on-open: there is no cache in v1 (plan §7.2), so the list is whatever the last call
 * returned and every refresh is two round trips (subscriptions, then currencies — the repository
 * joins them).
 */
@KoinViewModel
class SubscriptionsViewModel(
    private val subscriptionsRepository: SubscriptionsRepository,
    private val baseUrlProvider: BaseUrlProvider,
    private val moneyFormatter: MoneyFormatter,
    private val dateFormatter: DateFormatter
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SubscriptionsUiState(
            onRefresh = ::onRefresh,
            onRetryClick = ::onRetryClick
        )
    )
    val uiState: StateFlow<SubscriptionsUiState> = _uiState.asStateFlow()

    init {
        load(isRefresh = false)
    }

    private fun onRefresh() {
        load(isRefresh = true)
    }

    private fun onRetryClick() {
        load(isRefresh = false)
    }

    private fun load(isRefresh: Boolean) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = !isRefresh, isRefreshing = isRefresh, error = NativeText.Empty)
            }

            subscriptionsRepository.getSubscriptions()
                .onSuccess(::onLoaded)
                .onFailure(::onFailure)
        }
    }

    private fun onLoaded(subscriptions: List<Subscription>) {
        val items = subscriptions.map(::toUiItem).toPersistentList()
        _uiState.update {
            it.copy(items = items, isLoading = false, isRefreshing = false)
        }
    }

    private fun onFailure(throwable: Throwable) {
        logcat(priority = LogPriority.WARN, throwable = throwable) { "Loading subscriptions failed" }
        _uiState.update {
            it.copy(
                items = persistentListOf(),
                isLoading = false,
                isRefreshing = false,
                error = getErrorMessage(throwable)
            )
        }
    }

    private fun toUiItem(subscription: Subscription): SubscriptionUiItem = SubscriptionUiItem(
        id = subscription.id,
        name = subscription.name,
        logoUrl = toLogoUrl(subscription.logo),
        price = moneyFormatter.format(subscription.price, subscription.currencySymbol),
        nextPayment = subscription.nextPayment?.let(dateFormatter::formatDisplayDate).orEmpty(),
        cycle = subscription.cycle,
        frequency = subscription.frequency,
        isActive = subscription.isActive
    )

    /**
     * The base URL already carries its trailing slash (`BaseUrlProviderImpl`). It is blank only
     * if the app got here with no stored server, which the startup branch rules out — but a
     * relative URL would be a broken image request rather than none, so it is guarded anyway.
     */
    private fun toLogoUrl(logo: String): String {
        val baseUrl = baseUrlProvider.getBaseUrl()
        return if (logo.isBlank() || baseUrl.isBlank()) "" else "$baseUrl$LOGO_PATH$logo"
    }
}

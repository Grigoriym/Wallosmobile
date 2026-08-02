package com.grappim.wallosmobile.feature.subscriptions.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.wallosmobile.core.api.BaseUrlProvider
import com.grappim.wallosmobile.core.logger.LogPriority
import com.grappim.wallosmobile.core.logger.logcat
import com.grappim.wallosmobile.feature.subscriptions.domain.model.Subscription
import com.grappim.wallosmobile.feature.subscriptions.domain.repo.SubscriptionsRepository
import com.grappim.wallosmobile.feature.subscriptions.ui.toLogoUrl
import com.grappim.wallosmobile.utils.formatter.datetime.DateFormatter
import com.grappim.wallosmobile.utils.formatter.decimal.MoneyFormatter
import com.grappim.wallosmobile.utils.ui.NativeText
import com.grappim.wallosmobile.utils.ui.getErrorMessage
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

/**
 * Cache-first (3.4): the list is whatever the database holds, and the fetch on open is a refresh
 * behind it rather than the thing the screen waits for. So the spinner is only ever the *empty*
 * cache's, and a refresh that fails leaves the rows it couldn't replace on screen.
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
        observeCache()
        load(isRefresh = false)
    }

    private fun onRefresh() {
        load(isRefresh = true)
    }

    private fun onRetryClick() {
        load(isRefresh = false)
    }

    /**
     * Runs for the life of the ViewModel: a refresh writes to the database and the rows arrive
     * back through here, so this is the only thing that ever sets [SubscriptionsUiState.items].
     */
    private fun observeCache() {
        subscriptionsRepository.observeSubscriptions()
            .onEach(::onCached)
            .launchIn(viewModelScope)
    }

    private fun load(isRefresh: Boolean) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !isRefresh && it.items.isEmpty(),
                    isRefreshing = isRefresh,
                    error = NativeText.Empty
                )
            }

            subscriptionsRepository.refreshSubscriptions()
                .onSuccess { onRefreshed() }
                .onFailure(::onFailure)
        }
    }

    private fun onCached(subscriptions: List<Subscription>) {
        val items = subscriptions.map(::toUiItem).toPersistentList()
        _uiState.update {
            // Cached rows dismiss the first-load spinner the moment they arrive; an empty cache
            // leaves it up until the refresh answers, which is the only honest use left for it.
            it.copy(items = items, isLoading = it.isLoading && items.isEmpty())
        }
    }

    private fun onRefreshed() {
        _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
    }

    /**
     * Leaves [SubscriptionsUiState.items] alone — 2.4 cleared them because there was nothing
     * behind the error; with a cache there is, and it is still the truth as of its last refresh.
     */
    private fun onFailure(throwable: Throwable) {
        logcat(priority = LogPriority.WARN, throwable = throwable) { "Refreshing subscriptions failed" }
        _uiState.update {
            it.copy(isLoading = false, isRefreshing = false, error = getErrorMessage(throwable))
        }
    }

    private fun toUiItem(subscription: Subscription): SubscriptionUiItem = SubscriptionUiItem(
        id = subscription.id,
        name = subscription.name,
        logoUrl = baseUrlProvider.toLogoUrl(subscription.logo),
        price = moneyFormatter.format(subscription.price, subscription.currencySymbol),
        nextPayment = subscription.nextPayment?.let(dateFormatter::formatDisplayDate).orEmpty(),
        cycle = subscription.cycle,
        frequency = subscription.frequency,
        isActive = subscription.isActive
    )
}

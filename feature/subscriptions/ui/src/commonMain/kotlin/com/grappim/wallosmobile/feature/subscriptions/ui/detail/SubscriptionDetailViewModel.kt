package com.grappim.wallosmobile.feature.subscriptions.ui.detail

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

/**
 * Cache-first, like the list (3.4): the row the list cached is on screen immediately and the
 * re-read is a refresh behind it — one round trip now, not 2.5's two, because the currency symbol
 * comes from the cached currency table.
 *
 * [subscriptionId] arrives through `parametersOf` at the call site — Koin's `verify()` whitelists
 * `Int` on its own, so [InjectedParam] is here for the compiler plugin, which would otherwise look
 * for an `Int` definition in the graph.
 */
@KoinViewModel
class SubscriptionDetailViewModel(
    @InjectedParam private val subscriptionId: Int,
    private val subscriptionsRepository: SubscriptionsRepository,
    private val baseUrlProvider: BaseUrlProvider,
    private val moneyFormatter: MoneyFormatter,
    private val dateFormatter: DateFormatter
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionDetailUiState(onRetryClick = ::load))
    val uiState: StateFlow<SubscriptionDetailUiState> = _uiState.asStateFlow()

    init {
        observeCache()
        load()
    }

    /** The same row the list refresh rewrites, so this outlives the one-shot refresh below. */
    private fun observeCache() {
        subscriptionsRepository.observeSubscription(subscriptionId)
            .onEach(::onCached)
            .launchIn(viewModelScope)
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.subscription == null, error = NativeText.Empty) }

            subscriptionsRepository.refreshSubscription(subscriptionId)
                .onSuccess { onRefreshed() }
                .onFailure(::onFailure)
        }
    }

    /**
     * The cache is the source of truth, including when it stops holding this row: a list refresh
     * that dropped it means the server no longer has it, and keeping the last copy on screen
     * would be the lie. A row absent because nothing has cached it yet is the same `null`, and
     * the refresh below is already on its way.
     */
    private fun onCached(subscription: Subscription?) {
        _uiState.update {
            it.copy(
                subscription = subscription?.let(::toUiItem),
                isLoading = it.isLoading && subscription == null
            )
        }
    }

    private fun onRefreshed() {
        _uiState.update { it.copy(isLoading = false) }
    }

    /**
     * Leaves the row on screen — the cached one is real (3.4), where 2.5 had nothing behind the
     * error to keep.
     */
    private fun onFailure(throwable: Throwable) {
        logcat(priority = LogPriority.WARN, throwable = throwable) {
            "Refreshing subscription $subscriptionId failed"
        }
        _uiState.update {
            it.copy(isLoading = false, error = getErrorMessage(throwable))
        }
    }

    private fun toUiItem(subscription: Subscription) = SubscriptionDetailUiItem(
        name = subscription.name,
        logoUrl = baseUrlProvider.toLogoUrl(subscription.logo),
        price = moneyFormatter.format(subscription.price, subscription.currencySymbol),
        cycle = subscription.cycle,
        frequency = subscription.frequency,
        nextPayment = subscription.nextPayment?.let(dateFormatter::formatDisplayDate).orEmpty(),
        startDate = subscription.startDate?.let(dateFormatter::formatDisplayDate).orEmpty(),
        categoryName = subscription.categoryName,
        paymentMethodName = subscription.paymentMethodName,
        payerName = subscription.payerName,
        notes = subscription.notes,
        url = subscription.url,
        isActive = subscription.isActive
    )
}

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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

/**
 * Reads its own row rather than being handed one: the list holds no cache, so a snapshot taken
 * when the list was last refreshed would be the only other option and it can already be stale.
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
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = NativeText.Empty) }

            subscriptionsRepository.getSubscription(subscriptionId)
                .onSuccess(::onLoaded)
                .onFailure(::onFailure)
        }
    }

    private fun onLoaded(subscription: Subscription) {
        _uiState.update {
            it.copy(subscription = toUiItem(subscription), isLoading = false)
        }
    }

    private fun onFailure(throwable: Throwable) {
        logcat(priority = LogPriority.WARN, throwable = throwable) {
            "Loading subscription $subscriptionId failed"
        }
        _uiState.update {
            it.copy(subscription = null, isLoading = false, error = getErrorMessage(throwable))
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

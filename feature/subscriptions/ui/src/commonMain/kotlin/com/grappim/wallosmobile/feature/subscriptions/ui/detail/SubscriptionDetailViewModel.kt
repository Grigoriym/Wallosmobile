package com.grappim.wallosmobile.feature.subscriptions.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.wallosmobile.core.api.BaseUrlProvider
import com.grappim.wallosmobile.core.logger.LogPriority
import com.grappim.wallosmobile.core.logger.logcat
import com.grappim.wallosmobile.feature.subscriptions.domain.model.Currency
import com.grappim.wallosmobile.feature.subscriptions.domain.model.PriceConversion
import com.grappim.wallosmobile.feature.subscriptions.domain.model.Subscription
import com.grappim.wallosmobile.feature.subscriptions.domain.repo.SubscriptionsRepository
import com.grappim.wallosmobile.feature.subscriptions.ui.toLogoUrl
import com.grappim.wallosmobile.utils.formatter.datetime.DateFormatter
import com.grappim.wallosmobile.utils.formatter.decimal.MoneyFormatter
import com.grappim.wallosmobile.utils.ui.NativeText
import com.grappim.wallosmobile.utils.ui.getErrorMessage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.receiveAsFlow
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

    private val _uiState = MutableStateFlow(
        SubscriptionDetailUiState(
            onRetryClick = ::load,
            onDeleteClick = ::onDeleteClick,
            onDeleteDialogDismiss = ::onDeleteDialogDismiss,
            onDeleteConfirm = ::onDeleteConfirm
        )
    )
    val uiState: StateFlow<SubscriptionDetailUiState> = _uiState.asStateFlow()

    /** Bumped only by a refresh that succeeds (5.6) — see the list ViewModel's field of the same name. */
    private val refreshGeneration = MutableStateFlow(0)

    /** One-off, per plan: a successful delete is a signal the screen acts on, never UI state. */
    private val _deleted = Channel<Unit>()
    val deleted = _deleted.receiveAsFlow()

    init {
        observeCache()
        load()
    }

    /**
     * The same row the list refresh rewrites, so this outlives the one-shot refresh below.
     *
     * The conversion state and the currency table come along it because the row alone cannot say
     * what its price *means* (5.4): they are cached by the same refresh, so combining them keeps
     * one render path rather than letting a second collector disagree with this one.
     */
    private fun observeCache() {
        combine(
            subscriptionsRepository.observeSubscription(subscriptionId),
            subscriptionsRepository.observePriceConversion(),
            subscriptionsRepository.observeCurrencies(),
            refreshGeneration,
            ::onCached
        ).launchIn(viewModelScope)
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
    private fun onCached(
        subscription: Subscription?,
        conversion: PriceConversion,
        currencies: List<Currency>,
        refreshGeneration: Int
    ) {
        _uiState.update {
            it.copy(
                subscription = subscription?.let { row -> toUiItem(row, conversion, currencies, refreshGeneration) },
                isLoading = it.isLoading && subscription == null
            )
        }
    }

    private fun onRefreshed() {
        _uiState.update { it.copy(isLoading = false) }
        refreshGeneration.update { it + 1 }
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

    private fun onDeleteClick() {
        _uiState.update { it.copy(isDeleteDialogOpen = true) }
    }

    private fun onDeleteDialogDismiss() {
        _uiState.update { it.copy(isDeleteDialogOpen = false) }
    }

    private fun onDeleteConfirm() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, deleteError = NativeText.Empty) }

            subscriptionsRepository.deleteSubscription(subscriptionId)
                .onSuccess {
                    _uiState.update { it.copy(isDeleting = false, isDeleteDialogOpen = false) }
                    _deleted.send(Unit)
                }
                .onFailure { throwable ->
                    logcat(priority = LogPriority.WARN, throwable = throwable) {
                        "Deleting subscription $subscriptionId failed"
                    }
                    _uiState.update { it.copy(isDeleting = false, deleteError = getErrorMessage(throwable)) }
                }
        }
    }

    /**
     * The currency this row's price was converted **from**, blank when it wasn't converted (5.4).
     *
     * [PriceConversion.converts] is the same question the repository asked when it chose the symbol
     * the price is denominated in, so the label cannot contradict the amount above it: a row already
     * in the main currency, an instance not converting, and one that cannot convert all answer
     * `false` and say nothing.
     *
     * The **code** rather than the name — "USD" is what a price is labelled with everywhere else,
     * and it is unambiguous where a symbol isn't (five currencies here share `$`). Blank for a
     * `currency_id` the cached table doesn't hold, on the same reasoning as the missing symbol it
     * would have had: no label beats naming the wrong currency.
     */
    private fun convertedFrom(
        subscription: Subscription,
        conversion: PriceConversion,
        currencies: List<Currency>
    ): String {
        if (!conversion.converts(subscription.currencyId)) return ""
        val source = currencies.firstOrNull { it.id == subscription.currencyId } ?: return ""
        return source.code.ifBlank { source.name }
    }

    private fun toUiItem(
        subscription: Subscription,
        conversion: PriceConversion,
        currencies: List<Currency>,
        refreshGeneration: Int
    ) = SubscriptionDetailUiItem(
        name = subscription.name,
        logoUrl = baseUrlProvider.toLogoUrl(subscription.logo),
        price = moneyFormatter.format(subscription.price, subscription.currencySymbol),
        convertedFrom = convertedFrom(subscription, conversion, currencies),
        cycle = subscription.cycle,
        frequency = subscription.frequency,
        nextPayment = subscription.nextPayment?.let(dateFormatter::formatDisplayDate).orEmpty(),
        startDate = subscription.startDate?.let(dateFormatter::formatDisplayDate).orEmpty(),
        categoryName = subscription.categoryName,
        paymentMethodName = subscription.paymentMethodName,
        payerName = subscription.payerName,
        notes = subscription.notes,
        url = subscription.url,
        isActive = subscription.isActive,
        logoRefreshToken = refreshGeneration
    )
}

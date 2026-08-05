package com.grappim.wallosmobile.feature.subscriptions.ui.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.wallosmobile.core.api.BaseUrlProvider
import com.grappim.wallosmobile.core.logger.LogPriority
import com.grappim.wallosmobile.core.logger.logcat
import com.grappim.wallosmobile.feature.subscriptions.domain.model.PriceConversion
import com.grappim.wallosmobile.feature.subscriptions.domain.model.Subscription
import com.grappim.wallosmobile.feature.subscriptions.domain.repo.SubscriptionsRepository
import com.grappim.wallosmobile.feature.subscriptions.ui.toLogoUrl
import com.grappim.wallosmobile.utils.formatter.datetime.DateFormatter
import com.grappim.wallosmobile.utils.formatter.decimal.MoneyFormatter
import com.grappim.wallosmobile.utils.ui.NativeText
import com.grappim.wallosmobile.utils.ui.getErrorMessage
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

/**
 * Cache-first (3.4): the list is whatever the database holds, and the fetch on open is a refresh
 * behind it rather than the thing the screen waits for. So the spinner is only ever the *empty*
 * cache's, and a refresh that fails leaves the rows it couldn't replace on screen.
 *
 * Filtering and sorting (3.6) are client-side over that cache and live in [filter] and [sort]
 * rather than in the UI state: they are *inputs* to what the screen shows, combined with the rows
 * on one path, so a changed filter and an arriving refresh render through the same code.
 *
 * Since 5.2 those two outlive the process. [savedStateHandle] is the vehicle — the same one the
 * nav back stack rides, so a kill that restores the detail screen restores the list behind it in
 * the state the user left it. It arrives as an [InjectedParam] because Koin resolves a
 * `SavedStateHandle` from the `CreationExtras`, not from the graph.
 */
@KoinViewModel
class SubscriptionsViewModel(
    private val subscriptionsRepository: SubscriptionsRepository,
    private val baseUrlProvider: BaseUrlProvider,
    private val moneyFormatter: MoneyFormatter,
    private val dateFormatter: DateFormatter,
    private val subscriptionSorter: SubscriptionSorter,
    @InjectedParam private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val restoredCriteria = restoreCriteria()
    private val filter = MutableStateFlow(restoredCriteria.toFilter())
    private val sort = MutableStateFlow(restoredCriteria.sort)

    private val _uiState = MutableStateFlow(
        SubscriptionsUiState(
            filters = SubscriptionsFilterUiState(
                // Seeded rather than left on the defaults: `onCached` sets these too, but the first
                // frame after a restore is drawn before the cache has emitted anything.
                filter = filter.value,
                sort = sort.value,
                onFilterChange = ::onFilterChange,
                onSortChange = ::onSortChange,
                onOpen = ::onFilterOpen,
                onDismiss = ::onFilterDismiss,
                onClear = ::onFilterClear
            ),
            onRefresh = ::onRefresh,
            onRetryClick = ::onRetryClick
        )
    )
    val uiState: StateFlow<SubscriptionsUiState> = _uiState.asStateFlow()

    init {
        observeCache()
        persistCriteria()
        load(isRefresh = false)
    }

    private fun onRefresh() {
        load(isRefresh = true)
    }

    private fun onRetryClick() {
        load(isRefresh = false)
    }

    private fun onFilterChange(newFilter: SubscriptionFilter) {
        filter.value = newFilter
    }

    private fun onSortChange(newSort: SubscriptionSort) {
        sort.value = newSort
    }

    /** Sort is not a filter: clearing what was narrowed leaves the chosen order alone. */
    private fun onFilterClear() {
        filter.value = SubscriptionFilter()
    }

    private fun onFilterOpen() {
        setFilterVisible(isVisible = true)
    }

    private fun onFilterDismiss() {
        setFilterVisible(isVisible = false)
    }

    private fun setFilterVisible(isVisible: Boolean) {
        _uiState.update { it.copy(filters = it.filters.copy(isVisible = isVisible)) }
    }

    /**
     * The one place the criteria are written down, for the same reason [observeCache] is the one
     * place the rows are read: a save driven off the flows themselves cannot disagree with them,
     * where a write next to each of the three setters could miss one.
     */
    private fun persistCriteria() {
        combine(filter, sort, SubscriptionFilter::toSaved)
            .onEach { savedStateHandle[KEY_CRITERIA] = Json.encodeToString(it) }
            .launchIn(viewModelScope)
    }

    private fun restoreCriteria(): SavedCriteria {
        val stored = savedStateHandle.get<String>(KEY_CRITERIA) ?: return SavedCriteria()
        return try {
            Json.decodeFromString<SavedCriteria>(stored)
        } catch (e: IllegalArgumentException) {
            // A build that renamed a sort or a status leaves the previous one's state unreadable.
            // Pre-v1 that state is disposable, but discarding it is not the same as crashing on it.
            logcat(priority = LogPriority.WARN, throwable = e) { "Stored filter and sort were unreadable" }
            SavedCriteria()
        }
    }

    /**
     * Runs for the life of the ViewModel: a refresh writes to the database and the rows arrive
     * back through here, so this is the only thing that ever sets [SubscriptionsUiState.items].
     */
    private fun observeCache() {
        combine(
            subscriptionsRepository.observeSubscriptions(),
            subscriptionsRepository.observePriceConversion(),
            filter,
            sort,
            ::onCached
        ).launchIn(viewModelScope)
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

    private fun onCached(
        subscriptions: List<Subscription>,
        conversion: PriceConversion,
        filter: SubscriptionFilter,
        sort: SubscriptionSort
    ) {
        val matching = subscriptions.filter(filter::matches)
        val items = subscriptionSorter.sort(matching, sort)
            .map(::toUiItem)
            .toPersistentList()

        _uiState.update {
            // Cached rows dismiss the first-load spinner the moment they arrive; an empty cache
            // leaves it up until the refresh answers, which is the only honest use left for it.
            // The question is asked of the *cache*, so a filter matching nothing keeps no spinner.
            it.copy(
                items = items,
                hasCachedRows = subscriptions.isNotEmpty(),
                // Asked of the rows on screen, not of the cache: what the banner warns about is
                // comparing them, and a filter down to one currency has nothing left to warn about.
                isConversionUnavailable = conversion.isEnabledWithoutRates && matching.spanCurrencies(),
                isLoading = it.isLoading && subscriptions.isEmpty(),
                filters = it.filters.copy(
                    filter = filter,
                    sort = sort,
                    payers = subscriptions.optionsOf(Subscription::payerName),
                    categories = subscriptions.optionsOf(Subscription::categoryName),
                    paymentMethods = subscriptions.optionsOf(Subscription::paymentMethodName)
                )
            )
        }
    }

    /**
     * `currencyId` and not `currencySymbol`: two currencies can share a sign — the instance ships
     * four different dollars and three different kroner — and rows in USD and AUD are no more
     * comparable for both saying `$`.
     */
    private fun List<Subscription>.spanCurrencies(): Boolean = distinctBy(Subscription::currencyId).size > 1

    /**
     * The sheet's options, straight off the rows: Wallos resolves these names server-side and never
     * sends a blank one, but a blank would be an unpickable chip, so it is dropped rather than shown.
     */
    private fun List<Subscription>.optionsOf(selector: (Subscription) -> String) = map(selector)
        .filter(String::isNotBlank)
        .distinct()
        .sortedBy(String::lowercase)
        .toPersistentList()

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

    private companion object {
        const val KEY_CRITERIA = "subscriptions_criteria"
    }
}

/**
 * 3.6's two criteria in the shape a [SavedStateHandle] can carry across a process. The handle's
 * values have to be Bundle-safe on Android and [SubscriptionFilter]'s `ImmutableSet`s have no
 * serializer, so what is stored is one JSON string under one key — which is also the only form a
 * host test can read back, since `SavedState` is `Bundle` and no Android runtime is available there.
 */
@Serializable
private data class SavedCriteria(
    val payers: Set<String> = emptySet(),
    val categories: Set<String> = emptySet(),
    val paymentMethods: Set<String> = emptySet(),
    val status: SubscriptionStatusFilter = SubscriptionStatusFilter.ALL,
    val sort: SubscriptionSort = SubscriptionSort.NEXT_PAYMENT
)

private fun SubscriptionFilter.toSaved(sort: SubscriptionSort) = SavedCriteria(
    payers = payers,
    categories = categories,
    paymentMethods = paymentMethods,
    status = status,
    sort = sort
)

private fun SavedCriteria.toFilter() = SubscriptionFilter(
    payers = payers.toPersistentSet(),
    categories = categories.toPersistentSet(),
    paymentMethods = paymentMethods.toPersistentSet(),
    status = status
)

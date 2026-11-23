package com.grappim.wallosmobile.feature.subscriptions.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.kit.logger.LogPriority
import com.grappim.kit.logger.logcat
import com.grappim.kit.uikit.NativeText
import com.grappim.wallosmobile.core.api.BaseUrlProvider
import com.grappim.wallosmobile.core.storage.subscriptions.SubscriptionCriteriaStorage
import com.grappim.wallosmobile.feature.subscriptions.domain.model.PriceConversion
import com.grappim.wallosmobile.feature.subscriptions.domain.model.Subscription
import com.grappim.wallosmobile.feature.subscriptions.domain.repo.SubscriptionsRepository
import com.grappim.wallosmobile.feature.subscriptions.ui.toLogoUrl
import com.grappim.wallosmobile.utils.formatter.datetime.DateFormatter
import com.grappim.wallosmobile.utils.formatter.decimal.MoneyFormatter
import com.grappim.wallosmobile.utils.ui.getErrorMessage
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
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
 * Those two outlive the screen, the process and the launch: [criteriaStorage] keeps them on disk,
 * and a disconnect drops them with the rest of the account's data.
 */
@KoinViewModel
class SubscriptionsViewModel(
    private val subscriptionsRepository: SubscriptionsRepository,
    private val baseUrlProvider: BaseUrlProvider,
    private val moneyFormatter: MoneyFormatter,
    private val dateFormatter: DateFormatter,
    private val subscriptionSorter: SubscriptionSorter,
    private val criteriaStorage: SubscriptionCriteriaStorage,
    private val json: Json
) : ViewModel() {

    private val filter = MutableStateFlow(SubscriptionFilter())
    private val sort = MutableStateFlow(SubscriptionSort.NEXT_PAYMENT)

    /**
     * Bumped only by a refresh that succeeds (5.6) — never a failed one, which left the rows and
     * their logos exactly as they were. Combined into [observeCache] so every [SubscriptionUiItem]
     * carries the current value, which is what forces Coil to retry a logo stuck in `Error`.
     */
    private val refreshGeneration = MutableStateFlow(0)

    private val _uiState = MutableStateFlow(
        SubscriptionsUiState(
            // Seeded true, not left on the default false: `init` below always calls `load`, so the
            // screen's first-ever frame is genuinely loading, never "checked and found nothing" —
            // the default would make `isEmpty` true for that one frame and flash the empty-state
            // text before `load` gets a chance to say otherwise.
            isLoading = true,
            filters = SubscriptionsFilterUiState(
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

    /**
     * The stored criteria are read before anything else starts, so the first rows drawn are already
     * narrowed and ordered — an unfiltered list that then jumps to the filtered one would be the
     * flicker this avoids — and so [persistCriteria] never writes the defaults over them.
     */
    init {
        viewModelScope.launch {
            restoreCriteria(criteriaStorage.criteria.first())
            observeCache()
            persistCriteria()
            load(isRefresh = false)
        }
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
     * where a write next to each of the three setters could miss one. The first emission is what
     * was just restored, so it is skipped: only a change the user made is written.
     */
    private fun persistCriteria() {
        combine(filter, sort, SubscriptionFilter::toSaved)
            .drop(1)
            .onEach { criteriaStorage.setCriteria(json.encodeToString(it)) }
            .launchIn(viewModelScope)
    }

    private fun restoreCriteria(stored: String?) {
        val criteria = stored?.let(::decodeCriteria) ?: return
        filter.value = criteria.toFilter()
        sort.value = criteria.sort
    }

    private fun decodeCriteria(stored: String): SavedCriteria? = try {
        json.decodeFromString<SavedCriteria>(stored)
    } catch (e: IllegalArgumentException) {
        // A build that renamed a sort or a status leaves the previous one's state unreadable.
        // Pre-v1 that state is disposable, but discarding it is not the same as crashing on it.
        logcat(priority = LogPriority.WARN, throwable = e) { "Stored filter and sort were unreadable" }
        null
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
            refreshGeneration,
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
        sort: SubscriptionSort,
        refreshGeneration: Int
    ) {
        val matching = subscriptions.filter(filter::matches)
        val items = subscriptionSorter.sort(matching, sort)
            .map { toUiItem(it, refreshGeneration) }
            .toPersistentList()
        val spansCurrencies = matching.spanDenominations(conversion)

        _uiState.update {
            // Cached rows dismiss the first-load spinner the moment they arrive; an empty cache
            // leaves it up until the refresh answers, which is the only honest use left for it.
            // The question is asked of the *cache*, so a filter matching nothing keeps no spinner.
            it.copy(
                items = items,
                hasCachedRows = subscriptions.isNotEmpty(),
                // Asked of the rows on screen, not of the cache: what the banner warns about is
                // comparing them, and a filter down to one currency has nothing left to warn about.
                // The banner is the narrower of the two questions — an instance that was *asked* to
                // convert and couldn't — so it can only fire where the sort is incomparable too.
                isConversionUnavailable = conversion.isEnabledWithoutRates && spansCurrencies,
                isLoading = it.isLoading && subscriptions.isEmpty(),
                filters = it.filters.copy(
                    filter = filter,
                    sort = sort,
                    payers = subscriptions.optionsOf(Subscription::payerName),
                    categories = subscriptions.optionsOf(Subscription::categoryName),
                    paymentMethods = subscriptions.optionsOf(Subscription::paymentMethodName),
                    spansCurrencies = spansCurrencies
                )
            )
        }
    }

    /**
     * Whether these rows are denominated in more than one currency — which is what decides both the
     * conversion banner and whether sorting by price means anything (5.3).
     *
     * The question is asked of what each `price` is denominated **in**, and that is the repository's
     * `symbolFor` decision, not either field on the row: a converted row carries the amount in the
     * main currency while `currencyId` still names the one it was converted *from* (3.11). So a
     * working conversion puts every row in one denomination however many `currencyId`s there are,
     * and only where it isn't running does the row's own currency decide.
     *
     * `currencyId` and not `currencySymbol` for that second half: two currencies can share a sign —
     * the instance ships four different dollars and three different kroner — and rows in USD and AUD
     * are no more comparable for both saying `$`.
     */
    private fun List<Subscription>.spanDenominations(conversion: PriceConversion): Boolean =
        !conversion.isActive && distinctBy(Subscription::currencyId).size > 1

    /**
     * The sheet's options, straight off the rows: Wallos resolves these names server-side and never
     * sends a blank one, but a blank would be an unpickable chip, so it is dropped rather than shown.
     */
    private fun List<Subscription>.optionsOf(selector: (Subscription) -> String) = map(selector)
        .filter(String::isNotBlank)
        .distinct()
        .sortedBy(String::lowercase)
        .toPersistentList()

    /**
     * This call landing only proves the write reached Room — not that [observeCache]'s own
     * long-lived collector has re-run against it yet, which needs an invalidation notice to
     * cross back from Room's own executor. Clearing [SubscriptionsUiState.isLoading] off that
     * alone flashes the empty state for however long that hop takes (confirmed 2026-08-10: a
     * real, reported delay, not a theoretical one). A **fresh** collection of the same query
     * doesn't have that gap — Room re-runs a cold `Flow`'s query against current state the
     * moment it's collected, invalidation or not — so waiting on one here, before declaring
     * done, gives the long-lived collector's own notification (triggered by the same write, and
     * typically delivered to every collector in the same round) a real chance to have landed too.
     */
    private suspend fun onRefreshed() {
        subscriptionsRepository.observeSubscriptions().first()
        _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
        refreshGeneration.update { it + 1 }
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

    private fun toUiItem(subscription: Subscription, refreshGeneration: Int): SubscriptionUiItem = SubscriptionUiItem(
        id = subscription.id,
        name = subscription.name,
        logoUrl = baseUrlProvider.toLogoUrl(subscription.logo),
        price = moneyFormatter.format(subscription.price, subscription.currencySymbol),
        nextPayment = subscription.nextPayment?.let(dateFormatter::formatDisplayDate).orEmpty(),
        cycle = subscription.cycle,
        frequency = subscription.frequency,
        isActive = subscription.isActive,
        logoRefreshToken = refreshGeneration
    )
}

/**
 * 3.6's two criteria in the shape [SubscriptionCriteriaStorage] keeps: one JSON string, since
 * [SubscriptionFilter]'s `ImmutableSet`s have no serializer and `core:storage` cannot name these
 * types anyway.
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

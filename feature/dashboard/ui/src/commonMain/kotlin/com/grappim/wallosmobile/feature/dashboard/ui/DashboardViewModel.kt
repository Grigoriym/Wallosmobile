package com.grappim.wallosmobile.feature.dashboard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.wallosmobile.core.domain.WallosError
import com.grappim.wallosmobile.feature.dashboard.domain.model.MonthlyCost
import com.grappim.wallosmobile.feature.dashboard.domain.model.PeriodBudget
import com.grappim.wallosmobile.feature.dashboard.domain.usecase.DashboardHomeUseCase
import com.grappim.wallosmobile.feature.subscriptions.domain.model.Subscription
import com.grappim.wallosmobile.utils.formatter.datetime.DateFormatter
import com.grappim.wallosmobile.utils.formatter.decimal.MoneyFormatter
import com.grappim.wallosmobile.utils.ui.getErrorMessage
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Clock

/**
 * Nothing on this screen writes and nothing refreshes a cache behind it (M8 preamble), so unlike
 * `SubscriptionsViewModel` there is no `observe*` to combine with — one [load] call per open (and
 * per [DashboardUiState.onRetryClick]) is the whole lifecycle.
 */
@KoinViewModel
class DashboardViewModel(
    private val dashboardHomeUseCase: DashboardHomeUseCase,
    private val moneyFormatter: MoneyFormatter,
    private val dateFormatter: DateFormatter
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState(isLoading = true, onRetryClick = ::load))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val data = dashboardHomeUseCase.getDashboardHomeData(today)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    monthlyCost = data.monthlyCost.toCardState(),
                    periodBudget = data.periodBudget.toCardState(),
                    upcomingPayments = data.upcomingPayments.map(::toUiItem).toPersistentList()
                )
            }
        }
    }

    private fun Result<MonthlyCost>.toCardState(): MonthlyCostCardUiState = fold(
        onSuccess = { cost ->
            MonthlyCostCardUiState(title = cost.title, amount = moneyFormatter.format(cost.amount, cost.currencySymbol))
        },
        onFailure = { throwable -> MonthlyCostCardUiState(error = getErrorMessage(throwable)) }
    )

    private fun Result<PeriodBudget>.toCardState(): PeriodBudgetCardUiState = fold(
        onSuccess = { budget ->
            PeriodBudgetCardUiState(
                periodLabel = budget.periodLabel,
                budgetAmount = moneyFormatter.format(budget.periodBudget, budget.currencySymbol),
                remainingAmount = moneyFormatter.format(budget.amountRemainingThisPeriod, budget.currencySymbol),
                isOverBudget = budget.isOverBudget,
                amountOverBudget = moneyFormatter.format(budget.amountOverBudget, budget.currencySymbol)
            )
        },
        onFailure = { throwable ->
            if (throwable == WallosError.UnsupportedEndpoint) {
                PeriodBudgetCardUiState(isHidden = true)
            } else {
                PeriodBudgetCardUiState(error = getErrorMessage(throwable))
            }
        }
    )

    private fun toUiItem(subscription: Subscription) = UpcomingPaymentUiItem(
        id = subscription.id,
        name = subscription.name,
        price = moneyFormatter.format(subscription.price, subscription.currencySymbol),
        nextPayment = subscription.nextPayment?.let(dateFormatter::formatDisplayDate).orEmpty()
    )
}

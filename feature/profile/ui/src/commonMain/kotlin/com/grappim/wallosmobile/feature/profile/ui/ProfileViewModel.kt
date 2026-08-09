package com.grappim.wallosmobile.feature.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.wallosmobile.core.logger.LogPriority
import com.grappim.wallosmobile.core.logger.logcat
import com.grappim.wallosmobile.feature.profile.domain.model.BudgetPeriodType
import com.grappim.wallosmobile.feature.profile.domain.repo.ProfileRepository
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.profile_error_invalid
import com.grappim.wallosmobile.utils.ui.NativeText
import com.grappim.wallosmobile.utils.ui.getErrorMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.KoinViewModel

/**
 * Deliberately no `init { load() }`, mirroring `CategoriesViewModel`/`CurrenciesViewModel`: this
 * screen has no cache to fall back on, and `ProfileScreen`'s own `LaunchedEffect(Unit)` is the
 * single load path for both the first open and a return trip after process death disposes and
 * restarts this Nav3 entry.
 *
 * [periodType]/[anchorDate] hold whatever [ProfileRepository.getUser] last reported, never shown or
 * edited on this screen — `setBudget` resets both server-side the moment they're omitted (plan
 * §3.8, `ProfileRepository.setBudget`'s own doc comment), so a save has to resend them unchanged.
 */
@KoinViewModel
class ProfileViewModel(private val profileRepository: ProfileRepository) : ViewModel() {

    private var periodType: BudgetPeriodType? = null
    private var anchorDate: LocalDate? = null

    private val _uiState = MutableStateFlow(
        ProfileUiState(
            onRetryClick = ::load,
            onBudgetChange = ::onBudgetChange,
            onPeriodBudgetChange = ::onPeriodBudgetChange,
            onSaveClick = ::onSaveClick
        )
    )
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = NativeText.Empty) }

            profileRepository.getUser()
                .onSuccess { user ->
                    periodType = user.budgetPeriodType
                    anchorDate = user.budgetPeriodAnchorDate
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            budget = user.budget.toString(),
                            periodBudget = user.periodBudget.toString()
                        )
                    }
                }
                .onFailure { throwable ->
                    logcat(priority = LogPriority.WARN, throwable = throwable) { "Loading the profile failed" }
                    _uiState.update { it.copy(isLoading = false, error = getErrorMessage(throwable)) }
                }
        }
    }

    private fun onBudgetChange(value: String) {
        _uiState.update { it.copy(budget = value) }
    }

    private fun onPeriodBudgetChange(value: String) {
        _uiState.update { it.copy(periodBudget = value) }
    }

    private fun onSaveClick() {
        val state = _uiState.value
        val budget = state.budget.toDoubleOrNull()
        val periodBudget = state.periodBudget.toDoubleOrNull()
        val type = periodType
        val anchor = anchorDate

        if (budget == null || periodBudget == null || type == null || anchor == null) {
            _uiState.update { it.copy(saveError = NativeText.Resource(RString.profile_error_invalid)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = NativeText.Empty) }

            profileRepository.setBudget(budget, periodBudget, type, anchor)
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false) }
                }
                .onFailure { throwable ->
                    logcat(priority = LogPriority.WARN, throwable = throwable) { "Saving the budget failed" }
                    _uiState.update { it.copy(isSaving = false, saveError = getErrorMessage(throwable)) }
                }
        }
    }
}

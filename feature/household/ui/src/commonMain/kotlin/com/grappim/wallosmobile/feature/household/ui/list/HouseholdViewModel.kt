package com.grappim.wallosmobile.feature.household.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.wallosmobile.core.logger.LogPriority
import com.grappim.wallosmobile.core.logger.logcat
import com.grappim.wallosmobile.feature.household.domain.repo.HouseholdRepository
import com.grappim.wallosmobile.utils.ui.NativeText
import com.grappim.wallosmobile.utils.ui.getErrorMessage
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

/**
 * Deliberately no `init { load() }`, mirroring `CategoriesViewModel`: this list has no cache to
 * fall back on (this milestone's preamble), and it needs to reload after a return trip from
 * [com.grappim.wallosmobile.feature.household.ui.editor.HouseholdMemberEditorRoute] as much as on
 * first open. Nav3 disposes a covered entry's composition and restarts it once it's on top again,
 * so [HouseholdUiState.onRetryClick], fired from the screen's own `LaunchedEffect`, is the single
 * load path for both — see `HouseholdScreen`.
 */
@KoinViewModel
class HouseholdViewModel(private val householdRepository: HouseholdRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(HouseholdUiState(onRetryClick = ::load))
    val uiState: StateFlow<HouseholdUiState> = _uiState.asStateFlow()

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = NativeText.Empty) }

            householdRepository.getMembers()
                .onSuccess { members ->
                    val items = members.map { HouseholdMemberUiItem(id = it.id, name = it.name, email = it.email) }
                    _uiState.update { it.copy(isLoading = false, items = items.toPersistentList()) }
                }
                .onFailure { throwable ->
                    logcat(priority = LogPriority.WARN, throwable = throwable) { "Loading household members failed" }
                    _uiState.update { it.copy(isLoading = false, error = getErrorMessage(throwable)) }
                }
        }
    }
}

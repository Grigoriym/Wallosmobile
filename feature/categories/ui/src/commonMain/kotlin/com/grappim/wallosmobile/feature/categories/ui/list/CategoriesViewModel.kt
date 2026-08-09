package com.grappim.wallosmobile.feature.categories.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.wallosmobile.core.logger.LogPriority
import com.grappim.wallosmobile.core.logger.logcat
import com.grappim.wallosmobile.feature.categories.domain.repo.CategoriesRepository
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
 * Deliberately no `init { load() }`, unlike `DashboardViewModel`/`SubscriptionDetailViewModel`:
 * this list has no cache to fall back on (this milestone's preamble), and it needs to reload after
 * a return trip from [com.grappim.wallosmobile.feature.categories.ui.editor.CategoryEditorRoute]
 * as much as on first open. Nav3 disposes a covered entry's composition and restarts it once it's
 * on top again, so [CategoriesUiState.onRetryClick], fired from the screen's own `LaunchedEffect`,
 * is the single load path for both — see `CategoriesScreen`.
 */
@KoinViewModel
class CategoriesViewModel(private val categoriesRepository: CategoriesRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoriesUiState(onRetryClick = ::load))
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = NativeText.Empty) }

            categoriesRepository.getCategories()
                .onSuccess { categories ->
                    val items = categories.map { CategoryUiItem(id = it.id, name = it.name) }
                    _uiState.update { it.copy(isLoading = false, items = items.toPersistentList()) }
                }
                .onFailure { throwable ->
                    logcat(priority = LogPriority.WARN, throwable = throwable) { "Loading categories failed" }
                    _uiState.update { it.copy(isLoading = false, error = getErrorMessage(throwable)) }
                }
        }
    }
}

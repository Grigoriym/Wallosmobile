package com.grappim.wallosmobile.feature.settings.ui.startdestination

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.wallosmobile.core.domain.resultOf
import com.grappim.wallosmobile.core.logger.LogPriority
import com.grappim.wallosmobile.core.logger.logcat
import com.grappim.wallosmobile.core.storage.startdestination.StartDestination
import com.grappim.wallosmobile.core.storage.startdestination.StartDestinationStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

/**
 * Takes [StartDestinationStorage] straight, with no repository between — same single-seam case as
 * `InterfaceViewModel`.
 */
@KoinViewModel
class StartDestinationViewModel(private val startDestinationStorage: StartDestinationStorage) : ViewModel() {

    private val _uiState = MutableStateFlow(StartDestinationUiState(onSelect = ::onSelect))
    val uiState: StateFlow<StartDestinationUiState> = _uiState.asStateFlow()

    init {
        startDestinationStorage.startDestination
            .onEach { destination -> _uiState.update { it.copy(selected = destination) } }
            .launchIn(viewModelScope)
    }

    private fun onSelect(destination: StartDestination) {
        viewModelScope.launch {
            // A failed write leaves the previous destination selected, which is the honest
            // outcome — the flow above never emits, so the radio group keeps showing what is
            // actually stored.
            resultOf { startDestinationStorage.setStartDestination(destination) }
                .onFailure {
                    logcat(priority = LogPriority.WARN, throwable = it) { "Storing the start destination failed" }
                }
        }
    }
}

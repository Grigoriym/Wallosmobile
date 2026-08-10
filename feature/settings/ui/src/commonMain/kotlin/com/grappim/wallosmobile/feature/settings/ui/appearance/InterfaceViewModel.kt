package com.grappim.wallosmobile.feature.settings.ui.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.wallosmobile.core.crashreportingapi.CrashReporter
import com.grappim.wallosmobile.core.domain.resultOf
import com.grappim.wallosmobile.core.logger.LogPriority
import com.grappim.wallosmobile.core.logger.logcat
import com.grappim.wallosmobile.core.storage.crashreporting.CrashReportingStorage
import com.grappim.wallosmobile.core.storage.theme.ThemeMode
import com.grappim.wallosmobile.core.storage.theme.ThemeStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

/**
 * Takes [ThemeStorage] and [CrashReportingStorage] straight, with no repository between:
 * `feature:settings` is `ui`-only (2.6), and this is the same single-seam case Disconnect is.
 * [crashReporter] is read once for [InterfaceUiState.isCrashReportingAvailable] — it is a
 * build/flavor fact, not something that changes while the screen is open.
 */
@KoinViewModel
class InterfaceViewModel(
    private val themeStorage: ThemeStorage,
    private val crashReportingStorage: CrashReportingStorage,
    crashReporter: CrashReporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        InterfaceUiState(
            onModeSelect = ::onModeSelect,
            isCrashReportingAvailable = crashReporter.isAvailable,
            onCrashReportingToggle = ::onCrashReportingToggle
        )
    )
    val uiState: StateFlow<InterfaceUiState> = _uiState.asStateFlow()

    init {
        themeStorage.themeMode
            .onEach { mode -> _uiState.update { it.copy(selectedMode = mode) } }
            .launchIn(viewModelScope)

        crashReportingStorage.crashReportingEnabled
            .onEach { enabled -> _uiState.update { it.copy(crashReportingEnabled = enabled) } }
            .launchIn(viewModelScope)
    }

    private fun onModeSelect(mode: ThemeMode) {
        viewModelScope.launch {
            // A failed write leaves the previous mode selected, which is the honest outcome — the
            // flow above never emits, so the radio group keeps showing what is actually stored.
            resultOf { themeStorage.setThemeMode(mode) }
                .onFailure { logcat(priority = LogPriority.WARN, throwable = it) { "Storing the theme mode failed" } }
        }
    }

    private fun onCrashReportingToggle(enabled: Boolean) {
        viewModelScope.launch {
            resultOf { crashReportingStorage.setCrashReportingEnabled(enabled) }
                .onFailure {
                    logcat(priority = LogPriority.WARN, throwable = it) { "Storing crash-reporting consent failed" }
                }
        }
    }
}

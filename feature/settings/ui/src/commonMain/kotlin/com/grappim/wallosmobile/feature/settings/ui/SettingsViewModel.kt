package com.grappim.wallosmobile.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.wallosmobile.core.domain.resultOf
import com.grappim.wallosmobile.core.logger.LogPriority
import com.grappim.wallosmobile.core.logger.logcat
import com.grappim.wallosmobile.core.storage.ApiKeyStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

/**
 * Disconnect goes straight to storage: there is no server-side session to end (plan §1.2), so
 * "log out" is clearing the one stored credential. `clear()` removes the key alone (plan §4.7),
 * which is what leaves the server URL in the login field.
 */
@KoinViewModel
class SettingsViewModel(private val apiKeyStorage: ApiKeyStorage) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(onDisconnectClick = ::onDisconnectClick))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private fun onDisconnectClick() {
        viewModelScope.launch {
            // A failed write leaves the user connected and on this screen, which is the honest
            // outcome; there is no state to show for it, so the log is the whole report.
            resultOf { apiKeyStorage.clear() }
                .onFailure { logcat(priority = LogPriority.WARN, throwable = it) { "Disconnect failed" } }
        }
    }
}

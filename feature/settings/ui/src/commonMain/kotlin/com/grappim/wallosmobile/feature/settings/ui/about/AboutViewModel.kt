package com.grappim.wallosmobile.feature.settings.ui.about

import androidx.lifecycle.ViewModel
import com.grappim.wallosmobile.core.appinfoapi.AppInfoProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.KoinViewModel

/**
 * Takes [AppInfoProvider] straight, as `SettingsViewModel` takes `ApiKeyStorage` (2.6): one call on
 * one `core` seam, with nothing for a `domain` interface to hide. Reading it in the constructor is
 * the whole ViewModel — these values are fixed at build time, so there is no flow to collect.
 */
@KoinViewModel
class AboutViewModel(appInfoProvider: AppInfoProvider) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AboutUiState(
            versionName = appInfoProvider.versionName(),
            versionCode = appInfoProvider.versionCode(),
            isDebug = appInfoProvider.isDebug()
        )
    )
    val uiState: StateFlow<AboutUiState> = _uiState.asStateFlow()
}

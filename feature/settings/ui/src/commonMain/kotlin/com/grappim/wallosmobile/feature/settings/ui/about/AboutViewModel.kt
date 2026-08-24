package com.grappim.wallosmobile.feature.settings.ui.about

import androidx.lifecycle.ViewModel
import com.grappim.wallosmobile.core.appinfoapi.AppInfoProvider
import com.grappim.wallosmobile.core.crashreportingapi.CrashReporter
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.privacy_policy_url
import com.grappim.wallosmobile.strings.generated.resources.privacy_policy_url_gplay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.KoinViewModel

/**
 * Takes [AppInfoProvider] and [CrashReporter] straight, as `SettingsViewModel` takes
 * `ApiKeyStorage` (2.6): one call on one `core` seam each, with nothing for a `domain` interface
 * to hide. Reading them in the constructor is the whole ViewModel — these values are fixed at
 * build time, so there is no flow to collect.
 */
@KoinViewModel
class AboutViewModel(appInfoProvider: AppInfoProvider, crashReporter: CrashReporter) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AboutUiState(
            versionName = appInfoProvider.versionName(),
            versionCode = appInfoProvider.versionCode(),
            isDebug = appInfoProvider.isDebug(),
            privacyPolicyLink = if (crashReporter.isAvailable) {
                RString.privacy_policy_url_gplay
            } else {
                RString.privacy_policy_url
            }
        )
    )
    val uiState: StateFlow<AboutUiState> = _uiState.asStateFlow()
}

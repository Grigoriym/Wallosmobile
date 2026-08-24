package com.grappim.wallosmobile.feature.settings.ui.appearance

import com.grappim.wallosmobile.core.storage.theme.ThemeMode

/**
 * [selectedMode] is whatever storage last emitted, never a local selection: the write goes to
 * DataStore and comes back through the same flow the shell reads, so the radio group and the
 * app's palette can't disagree. Same reasoning for [crashReportingEnabled].
 * [isCrashReportingAvailable] is `crashReporter.isAvailable` (16.4) — runtime-gated via DI rather
 * than a compile-time flavor check, so the toggle renders on gplay and not at all on fdroid.
 */
data class InterfaceUiState(
    val selectedMode: ThemeMode = ThemeMode.default(),
    val onModeSelect: (ThemeMode) -> Unit = {},
    val isCrashReportingAvailable: Boolean = false,
    val crashReportingEnabled: Boolean = false,
    val onCrashReportingToggle: (Boolean) -> Unit = {}
)

package com.grappim.wallosmobile.feature.settings.ui.appearance

import com.grappim.wallosmobile.core.storage.theme.ThemeMode

/**
 * [selectedMode] is whatever storage last emitted, never a local selection: the write goes to
 * DataStore and comes back through the same flow the shell reads, so the radio group and the
 * app's palette can't disagree.
 */
data class InterfaceUiState(
    val selectedMode: ThemeMode = ThemeMode.default(),
    val onModeSelect: (ThemeMode) -> Unit = {}
)

package com.grappim.wallosmobile.feature.settings.ui

/**
 * A stub screen's state: one action and nothing to show. Disconnect has no loading or error state
 * because there is nothing to wait for — clearing the key flips `ApiKeyStorage.isConnected` and
 * the whole tree swaps to login (plan §7.1), so this screen is gone before it could render either.
 */
data class SettingsUiState(val onDisconnectClick: () -> Unit = {})

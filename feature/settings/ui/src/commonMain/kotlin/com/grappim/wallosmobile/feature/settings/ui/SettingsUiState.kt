package com.grappim.wallosmobile.feature.settings.ui

/**
 * Disconnect has no loading or error state because there is nothing to wait for — clearing the key
 * flips `ApiKeyStorage.isConnected` and the whole tree swaps to login (plan §7.1), so this screen is
 * gone before it could render either. [serverUrl] is the same fixed-at-construction case as
 * `AboutUiState`'s version fields — `BaseUrlProvider.getBaseUrl()` is a synchronous, cached read,
 * not a network call.
 */
data class SettingsUiState(val serverUrl: String = "", val onDisconnectClick: () -> Unit = {})

package com.grappim.wallosmobile.feature.subscriptions.dto

import kotlinx.serialization.Serializable

/**
 * `get_settings.php` (API doc §3.11). No default on [settings], like the other envelopes (2.3):
 * every user has a settings row, so an absent key is a broken response and `Malformed` beats a
 * silent set of defaults.
 */
@Serializable
data class SettingsResponse(val settings: SettingsDTO)

package com.grappim.wallosmobile.feature.subscriptions.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `get_settings.php`'s `settings` object (API doc §3.11), narrowed to the one field that changes
 * what this app *requests*. The rest — themes, `hide_disabled`, `monthly_price`,
 * `show_subscription_progress` — describe the web UI's own rendering and are dropped by
 * `ignoreUnknownKeys`; a step that wants one adds it here.
 *
 * @param convertCurrency `1` when the user has asked their instance to show prices in the main
 *   currency. An **int**, not a bool and not `"true"` — the same setting is written back to
 *   `set_settings.php` in yet another shape (API doc §6's "booleans are inconsistent"). Defaults
 *   to off, so a version that predates the column reads as "don't convert", which is the answer
 *   that cannot mislabel a price.
 */
@Serializable
data class SettingsDTO(@SerialName("convert_currency") val convertCurrency: Int = 0)

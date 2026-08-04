package com.grappim.wallosmobile.core.storage.theme

/**
 * Which palette the app draws with. [System] follows the device's night mode.
 *
 * Persisted as [value] rather than as the ordinal, so reordering the entries can't repoint a
 * stored preference at a different mode.
 */
enum class ThemeMode(val value: String) {
    System("system"),
    Light("light"),
    Dark("dark")
    ;

    companion object {

        fun fromValue(value: String?): ThemeMode? = entries.firstOrNull { it.value == value }

        fun default(): ThemeMode = System
    }
}

package com.grappim.wallosmobile.feature.settings.ui.about

/**
 * Build-time facts, read once — nothing here can change while the screen is open, so there is no
 * callback and no loading state. The fields are raw rather than a rendered line: the formatting is
 * a string resource, which a platform impl has no way to reach.
 */
data class AboutUiState(val versionName: String = "", val versionCode: Int = 0, val isDebug: Boolean = false)

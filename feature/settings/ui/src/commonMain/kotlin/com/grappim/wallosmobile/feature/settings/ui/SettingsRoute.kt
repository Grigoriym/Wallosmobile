package com.grappim.wallosmobile.feature.settings.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Lives with its screen (plan §5.3). It started in `composeApp/nav/` because the shell was built
 * (1.8) before either of its sections existed; this was the second and last of those, so the
 * placeholder file is gone.
 */
@Serializable
data object SettingsRoute : NavKey

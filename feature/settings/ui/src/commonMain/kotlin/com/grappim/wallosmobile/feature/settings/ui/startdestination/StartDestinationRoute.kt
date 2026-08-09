package com.grappim.wallosmobile.feature.settings.ui.startdestination

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Lives with its screen (plan §5.3). Not a drawer destination — same shape as
 * [com.grappim.wallosmobile.feature.settings.ui.appearance.InterfaceRoute]: only a process-death
 * restore on this screen can tell whether it was registered in the polymorphic module.
 */
@Serializable
data object StartDestinationRoute : NavKey

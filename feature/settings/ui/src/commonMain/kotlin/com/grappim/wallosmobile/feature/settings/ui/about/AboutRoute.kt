package com.grappim.wallosmobile.feature.settings.ui.about

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Lives with its screen (plan §5.3). Like [com.grappim.wallosmobile.feature.settings.ui.appearance.InterfaceRoute]
 * this is not a drawer destination, so `NavKeySerializersTest` cannot tell whether it was
 * registered in the polymorphic module — only a process-death restore on this screen can.
 */
@Serializable
data object AboutRoute : NavKey

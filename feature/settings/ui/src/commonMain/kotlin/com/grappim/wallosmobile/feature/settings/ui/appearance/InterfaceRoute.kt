package com.grappim.wallosmobile.feature.settings.ui.appearance

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Lives with its screen (plan §5.3). Unlike [com.grappim.wallosmobile.feature.settings.ui.SettingsRoute]
 * this is not a drawer destination, so `NavKeySerializersTest` — which walks `DrawerDestination`
 * — cannot tell whether it was registered in the polymorphic module. Only a process-death restore
 * on this screen can.
 */
@Serializable
data object InterfaceRoute : NavKey

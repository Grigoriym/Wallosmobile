package com.grappim.wallosmobile.feature.profile.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Reached from a `SettingsRow`, not a drawer destination — same shape as
 * `feature.settings.ui.appearance.InterfaceRoute`. `NavKeySerializersTest` doesn't cover this route
 * either, for the same reason: it walks `DrawerDestination`, and only a process-death restore on
 * this screen can tell whether it was registered in the polymorphic module.
 */
@Serializable
data object ProfileRoute : NavKey

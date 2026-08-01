package com.grappim.wallosmobile.feature.subscriptions.ui.list

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * The list is a drawer section, so it is the start destination and always the root of its own
 * sub-stack. It lived in `composeApp/nav/Routes.kt` until this screen existed (1.8); a route
 * belongs next to its screen (plan §5.3).
 *
 * Registered in the shell's `navKeySerializersModule` — a route missing from there survives every
 * gate and only breaks back-stack restore after process death.
 */
@Serializable
data object SubscriptionsRoute : NavKey

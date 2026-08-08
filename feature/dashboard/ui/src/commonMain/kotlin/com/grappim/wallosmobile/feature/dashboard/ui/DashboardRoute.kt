package com.grappim.wallosmobile.feature.dashboard.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * The landing screen (8.4), so it is the drawer's start destination and always the root of its
 * own sub-stack — same shape as `SubscriptionsRoute` before it.
 *
 * Registered in the shell's `navKeySerializersModule` — a route missing from there survives every
 * gate and only breaks back-stack restore after process death.
 */
@Serializable
data object DashboardRoute : NavKey

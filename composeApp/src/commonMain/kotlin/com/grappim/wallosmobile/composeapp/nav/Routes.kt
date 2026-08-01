package com.grappim.wallosmobile.composeapp.nav

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * The two drawer sections the shell needs to exist before either feature does. Routes belong in
 * the feature's own `ui` module (plan §5.3) — these move there with their screens: 2.4 takes
 * [SubscriptionsRoute] to `feature:subscriptions:ui`, 2.6 takes [SettingsRoute] to settings.
 * Only the imports in [DrawerDestination], [RouteConfigProvider], `NavKeySerializers.kt` and
 * `MainNavHost.kt` change when they do.
 */
@Serializable
data object SubscriptionsRoute : NavKey

@Serializable
data object SettingsRoute : NavKey

package com.grappim.wallosmobile.composeapp.nav

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * A route for a feature that doesn't exist yet — what `Routes.kt` held until 2.4 took
 * `SubscriptionsRoute` to `feature:subscriptions:ui`, where a route belongs (plan §5.3). 2.6 does
 * the same for this one and deletes the file. Anything added here in the meantime is a route
 * without a home, which is a smell, not a pattern.
 */
@Serializable
data object SettingsRoute : NavKey

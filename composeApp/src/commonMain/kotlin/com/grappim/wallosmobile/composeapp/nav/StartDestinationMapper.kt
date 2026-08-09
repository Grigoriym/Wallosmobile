package com.grappim.wallosmobile.composeapp.nav

import androidx.navigation3.runtime.NavKey
import com.grappim.wallosmobile.core.storage.startdestination.StartDestination

/**
 * `core:storage` cannot depend on any route type, so [StartDestination] mirrors [DrawerDestination]
 * by name only — this is where a stored value becomes the actual route.
 */
object StartDestinationMapper {

    fun toNavKey(startDestination: StartDestination): NavKey = when (startDestination) {
        StartDestination.Dashboard -> DrawerDestination.Dashboard.route
        StartDestination.Subscriptions -> DrawerDestination.Subscriptions.route
        StartDestination.Settings -> DrawerDestination.Settings.route
        StartDestination.Categories -> DrawerDestination.Categories.route
        StartDestination.Household -> DrawerDestination.Household.route
        StartDestination.PaymentMethods -> DrawerDestination.PaymentMethods.route
        StartDestination.Currencies -> DrawerDestination.Currencies.route
    }
}

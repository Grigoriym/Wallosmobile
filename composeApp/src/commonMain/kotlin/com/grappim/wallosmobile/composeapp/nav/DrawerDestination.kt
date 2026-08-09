package com.grappim.wallosmobile.composeapp.nav

import androidx.navigation3.runtime.NavKey
import com.grappim.wallosmobile.feature.categories.ui.list.CategoriesRoute
import com.grappim.wallosmobile.feature.currencies.ui.list.CurrenciesRoute
import com.grappim.wallosmobile.feature.dashboard.ui.DashboardRoute
import com.grappim.wallosmobile.feature.household.ui.list.HouseholdRoute
import com.grappim.wallosmobile.feature.paymentmethods.ui.list.PaymentMethodsRoute
import com.grappim.wallosmobile.feature.settings.ui.SettingsRoute
import com.grappim.wallosmobile.feature.subscriptions.ui.list.SubscriptionsRoute
import kotlinx.collections.immutable.persistentSetOf

/**
 * The top-level sections, one per drawer entry. Each owns an independent sub-stack, so every
 * destination here must also appear in [DRAWER_NAV_ITEMS] — `NavigationState.currentSubStack`
 * `error()`s on a section it has no stack for. The four catalog screens (9.7) are top-level
 * sections too, despite being grouped under "Manage" in the drawer's rendering — that grouping is
 * purely a [DrawerItem.Group] presentation choice, not a different navigation shape.
 */
enum class DrawerDestination(val route: NavKey) {
    Dashboard(DashboardRoute),
    Subscriptions(SubscriptionsRoute),
    Settings(SettingsRoute),
    Categories(CategoriesRoute),
    Household(HouseholdRoute),
    PaymentMethods(PaymentMethodsRoute),
    Currencies(CurrenciesRoute)
}

/** The set `rememberNavigationState` builds a sub-stack for. */
val DRAWER_NAV_ITEMS = persistentSetOf<NavKey>(
    DashboardRoute,
    SubscriptionsRoute,
    SettingsRoute,
    CategoriesRoute,
    HouseholdRoute,
    PaymentMethodsRoute,
    CurrenciesRoute
)

/** Where the drawer shell opens. Navigating here from another section clears the section stack. */
val START_DESTINATION: NavKey = DashboardRoute

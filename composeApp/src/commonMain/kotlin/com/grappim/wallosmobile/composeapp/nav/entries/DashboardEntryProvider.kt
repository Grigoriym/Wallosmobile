package com.grappim.wallosmobile.composeapp.nav.entries

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.grappim.wallosmobile.core.navigation.Navigator
import com.grappim.wallosmobile.feature.dashboard.ui.DashboardRoute
import com.grappim.wallosmobile.feature.dashboard.ui.DashboardScreen
import com.grappim.wallosmobile.feature.subscriptions.ui.detail.SubscriptionDetailRoute

/**
 * A row's own detail screen already exists (`feature:subscriptions:ui`), so upcoming payments
 * navigate there rather than to a new dashboard-owned surface (8.4's "no new detail surface").
 */
fun EntryProviderScope<NavKey>.dashboardEntry(navigator: Navigator) {
    entry<DashboardRoute> {
        DashboardScreen(onSubscriptionClick = { id -> navigator.navigate(SubscriptionDetailRoute(id)) })
    }
}

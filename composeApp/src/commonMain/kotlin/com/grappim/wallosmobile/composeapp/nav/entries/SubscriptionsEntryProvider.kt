package com.grappim.wallosmobile.composeapp.nav.entries

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.grappim.wallosmobile.feature.subscriptions.ui.list.SubscriptionsRoute
import com.grappim.wallosmobile.feature.subscriptions.ui.list.SubscriptionsScreen

/**
 * The one place that knows both a subscriptions route and its screen (plan §5.3), which is what
 * keeps features from depending on each other. No `Navigator` parameter yet: the list has nothing
 * to navigate to until 2.5 adds the detail screen.
 */
fun EntryProviderScope<NavKey>.subscriptionsEntry() {
    entry<SubscriptionsRoute> { SubscriptionsScreen() }
}

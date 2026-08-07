package com.grappim.wallosmobile.composeapp.nav.entries

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.grappim.wallosmobile.core.navigation.Navigator
import com.grappim.wallosmobile.feature.subscriptions.ui.detail.SubscriptionDetailRoute
import com.grappim.wallosmobile.feature.subscriptions.ui.detail.SubscriptionDetailScreen
import com.grappim.wallosmobile.feature.subscriptions.ui.editor.SubscriptionEditorRoute
import com.grappim.wallosmobile.feature.subscriptions.ui.editor.SubscriptionEditorScreen
import com.grappim.wallosmobile.feature.subscriptions.ui.list.SubscriptionsRoute
import com.grappim.wallosmobile.feature.subscriptions.ui.list.SubscriptionsScreen

/**
 * The one place that knows both a subscriptions route and its screen (plan §5.3), which is what
 * keeps features from depending on each other — and the only place a screen's navigation callbacks
 * are wired, since they belong to the shell rather than to any ViewModel.
 */
fun EntryProviderScope<NavKey>.subscriptionsEntry(navigator: Navigator) {
    entry<SubscriptionsRoute> {
        SubscriptionsScreen(
            onSubscriptionClick = { id -> navigator.navigate(SubscriptionDetailRoute(id)) }
        )
    }
    entry<SubscriptionDetailRoute> { route ->
        SubscriptionDetailScreen(
            subscriptionId = route.subscriptionId,
            onBackClick = { navigator.goBack() }
        )
    }
    entry<SubscriptionEditorRoute> {
        SubscriptionEditorScreen(onBackClick = { navigator.goBack() })
    }
}

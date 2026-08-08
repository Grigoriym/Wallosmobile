package com.grappim.wallosmobile.composeapp.nav

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import com.grappim.wallosmobile.feature.dashboard.ui.DashboardRoute
import com.grappim.wallosmobile.feature.settings.ui.SettingsRoute
import com.grappim.wallosmobile.feature.settings.ui.about.AboutRoute
import com.grappim.wallosmobile.feature.settings.ui.appearance.InterfaceRoute
import com.grappim.wallosmobile.feature.subscriptions.ui.detail.SubscriptionDetailRoute
import com.grappim.wallosmobile.feature.subscriptions.ui.editor.SubscriptionEditorRoute
import com.grappim.wallosmobile.feature.subscriptions.ui.list.SubscriptionsRoute
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/**
 * Every route in the app, listed once. `rememberNavBackStack` `require`s a configuration whose
 * `serializersModule` is not the default one and throws on the *first* composition if given
 * `SavedStateConfiguration.DEFAULT`; a route *missing* from here is the quiet failure — the app
 * runs and only back-stack restore after process death breaks.
 *
 * `internal` so `NavKeySerializersTest` can check the list against [DrawerDestination].
 */
internal val navKeySerializersModule = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(DashboardRoute::class)
        subclass(SubscriptionsRoute::class)
        subclass(SubscriptionDetailRoute::class)
        subclass(SubscriptionEditorRoute::class)
        subclass(SettingsRoute::class)
        subclass(InterfaceRoute::class)
        subclass(AboutRoute::class)
    }
}

val navSavedStateConfiguration = SavedStateConfiguration {
    serializersModule = navKeySerializersModule
}

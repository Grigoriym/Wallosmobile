package com.grappim.wallosmobile.composeapp.nav

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import com.grappim.wallosmobile.feature.categories.ui.editor.CategoryEditorRoute
import com.grappim.wallosmobile.feature.categories.ui.list.CategoriesRoute
import com.grappim.wallosmobile.feature.currencies.ui.editor.CurrencyEditorRoute
import com.grappim.wallosmobile.feature.currencies.ui.list.CurrenciesRoute
import com.grappim.wallosmobile.feature.dashboard.ui.DashboardRoute
import com.grappim.wallosmobile.feature.household.ui.editor.HouseholdMemberEditorRoute
import com.grappim.wallosmobile.feature.household.ui.list.HouseholdRoute
import com.grappim.wallosmobile.feature.paymentmethods.ui.editor.PaymentMethodEditorRoute
import com.grappim.wallosmobile.feature.paymentmethods.ui.list.PaymentMethodsRoute
import com.grappim.wallosmobile.feature.profile.ui.ProfileRoute
import com.grappim.wallosmobile.feature.settings.ui.SettingsRoute
import com.grappim.wallosmobile.feature.settings.ui.about.AboutRoute
import com.grappim.wallosmobile.feature.settings.ui.appearance.InterfaceRoute
import com.grappim.wallosmobile.feature.settings.ui.startdestination.StartDestinationRoute
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
        subclass(StartDestinationRoute::class)
        subclass(AboutRoute::class)
        subclass(ProfileRoute::class)
        subclass(CategoriesRoute::class)
        subclass(CategoryEditorRoute::class)
        subclass(HouseholdRoute::class)
        subclass(HouseholdMemberEditorRoute::class)
        subclass(PaymentMethodsRoute::class)
        subclass(PaymentMethodEditorRoute::class)
        subclass(CurrenciesRoute::class)
        subclass(CurrencyEditorRoute::class)
    }
}

val navSavedStateConfiguration = SavedStateConfiguration {
    serializersModule = navKeySerializersModule
}

package com.grappim.wallosmobile.core.storage.startdestination

/**
 * Which drawer section the app opens on. `core:storage` has no dependency on any route type — the
 * `NavKey` subclasses live in `composeApp` and feature `ui` modules, layers above this one — so
 * this mirrors `DrawerDestination`'s seven cases by name only; `composeApp` owns mapping a value
 * here to the actual route.
 *
 * Persisted as [value] rather than as the ordinal, so reordering the entries can't repoint a
 * stored preference at a different section.
 */
enum class StartDestination(val value: String) {
    Dashboard("dashboard"),
    Subscriptions("subscriptions"),
    Settings("settings"),
    Categories("categories"),
    Household("household"),
    PaymentMethods("payment_methods"),
    Currencies("currencies")
    ;

    companion object {

        fun fromValue(value: String?): StartDestination? = entries.firstOrNull { it.value == value }

        fun default(): StartDestination = Dashboard
    }
}

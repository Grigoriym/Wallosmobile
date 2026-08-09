package com.grappim.wallosmobile.feature.currencies.ui.list

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * The list is one of the "Manage" drawer group's own sub-stacks (9.7), so it is the root of that
 * sub-stack the same way `CategoriesRoute`/`HouseholdRoute` are for their own resources.
 *
 * Registered in the shell's `navKeySerializersModule` — a route missing from there survives every
 * gate and only breaks back-stack restore after process death.
 */
@Serializable
data object CurrenciesRoute : NavKey

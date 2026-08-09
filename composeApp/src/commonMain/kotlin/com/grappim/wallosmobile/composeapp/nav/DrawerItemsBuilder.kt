package com.grappim.wallosmobile.composeapp.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.categories_title
import com.grappim.wallosmobile.strings.generated.resources.currencies_title
import com.grappim.wallosmobile.strings.generated.resources.dashboard_title
import com.grappim.wallosmobile.strings.generated.resources.household_title
import com.grappim.wallosmobile.strings.generated.resources.manage_group_title
import com.grappim.wallosmobile.strings.generated.resources.payment_methods_title
import com.grappim.wallosmobile.strings.generated.resources.settings_title
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_title
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.koin.core.annotation.Factory

/**
 * A class rather than a constant so the item list can later depend on state (plan §5.4).
 */
@Factory
class DrawerItemsBuilder {

    fun build(): ImmutableList<DrawerItem> = persistentListOf(
        DrawerItem.Destination(
            destination = DrawerDestination.Dashboard,
            label = RString.dashboard_title,
            icon = IconSource.Vector(Icons.Filled.Home)
        ),
        DrawerItem.Destination(
            destination = DrawerDestination.Subscriptions,
            label = RString.subscriptions_title,
            // material-icons-core carries ~50 icons and no `Subscriptions`; a closer icon needs
            // material-icons-extended, which no module declares.
            icon = IconSource.Vector(Icons.AutoMirrored.Filled.List)
        ),
        DrawerItem.Destination(
            destination = DrawerDestination.Settings,
            label = RString.settings_title,
            icon = IconSource.Vector(Icons.Filled.Settings)
        ),
        DrawerItem.Group(
            label = RString.manage_group_title,
            items = listOf(
                DrawerItem.Destination(
                    destination = DrawerDestination.Categories,
                    label = RString.categories_title,
                    // No `Category`/`Label` icon in material-icons-core (CLAUDE.md's icon-set
                    // note) — `Star` is unused elsewhere in the drawer, picked for that reason
                    // rather than any semantic fit.
                    icon = IconSource.Vector(Icons.Filled.Star)
                ),
                DrawerItem.Destination(
                    destination = DrawerDestination.Household,
                    label = RString.household_title,
                    icon = IconSource.Vector(Icons.Filled.Person)
                ),
                DrawerItem.Destination(
                    destination = DrawerDestination.PaymentMethods,
                    label = RString.payment_methods_title,
                    icon = IconSource.Vector(Icons.Filled.ShoppingCart)
                ),
                DrawerItem.Destination(
                    destination = DrawerDestination.Currencies,
                    label = RString.currencies_title,
                    icon = IconSource.Vector(Icons.Filled.Refresh)
                )
            )
        )
    )
}

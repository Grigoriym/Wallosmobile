package com.grappim.wallosmobile.composeapp.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.dashboard_title
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
        )
    )
}

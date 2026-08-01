package com.grappim.wallosmobile.composeapp.nav

import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

sealed interface IconSource {
    data class Vector(val imageVector: ImageVector) : IconSource

    data class Resource(val resourceId: DrawableResource) : IconSource
}

/**
 * The drawer renders a list of these rather than a flat list of destinations, so that later
 * phases can group the management screens under a header without touching the widget (plan §5.4).
 */
sealed interface DrawerItem {
    data class Group(val label: StringResource, val items: List<Destination>) : DrawerItem

    data class Destination(val destination: DrawerDestination, val label: StringResource, val icon: IconSource) :
        DrawerItem

    data object Divider : DrawerItem
}

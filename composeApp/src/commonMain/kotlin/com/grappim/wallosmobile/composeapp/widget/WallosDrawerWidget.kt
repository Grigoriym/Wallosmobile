package com.grappim.wallosmobile.composeapp.widget

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grappim.wallosmobile.composeapp.nav.DrawerDestination
import com.grappim.wallosmobile.composeapp.nav.DrawerItem
import com.grappim.wallosmobile.composeapp.nav.DrawerItemsBuilder
import com.grappim.wallosmobile.composeapp.nav.IconSource
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.app_name
import com.grappim.wallosmobile.uikit.WallosMobilePreviewTheme
import com.grappim.wallosmobile.uikit.utils.PreviewWallosDarkLight
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun WallosDrawerWidget(
    drawerItems: ImmutableList<DrawerItem>,
    currentTopLevelDestination: DrawerDestination?,
    onDrawerItemClick: (DrawerDestination) -> Unit,
    drawerState: DrawerState,
    modifier: Modifier = Modifier,
    gesturesEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    ModalNavigationDrawer(
        modifier = modifier,
        gesturesEnabled = gesturesEnabled,
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerState = drawerState) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(RString.app_name),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleLarge
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    drawerItems.forEach { drawerItem ->
                        when (drawerItem) {
                            is DrawerItem.Group -> {
                                Text(
                                    text = stringResource(drawerItem.label),
                                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                drawerItem.items.forEach { destination ->
                                    DrawerDestinationItem(
                                        item = destination,
                                        isSelected = currentTopLevelDestination == destination.destination,
                                        onClick = { onDrawerItemClick(destination.destination) }
                                    )
                                }
                            }

                            is DrawerItem.Destination -> {
                                DrawerDestinationItem(
                                    item = drawerItem,
                                    isSelected = currentTopLevelDestination == drawerItem.destination,
                                    onClick = { onDrawerItemClick(drawerItem.destination) }
                                )
                            }

                            is DrawerItem.Divider -> {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                    }
                }
            }
        },
        content = content
    )
}

@Composable
private fun DrawerDestinationItem(
    item: DrawerItem.Destination,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val label = stringResource(item.label)

    NavigationDrawerItem(
        modifier = modifier,
        label = { Text(text = label) },
        selected = isSelected,
        icon = {
            when (val iconSource = item.icon) {
                is IconSource.Vector -> Icon(
                    imageVector = iconSource.imageVector,
                    contentDescription = label
                )

                is IconSource.Resource -> Icon(
                    painter = painterResource(iconSource.resourceId),
                    contentDescription = label
                )
            }
        },
        onClick = onClick
    )
}

@PreviewWallosDarkLight
@Composable
private fun WallosDrawerWidgetPreview() = WallosMobilePreviewTheme {
    WallosDrawerWidget(
        drawerItems = DrawerItemsBuilder().build(),
        currentTopLevelDestination = DrawerDestination.Subscriptions,
        onDrawerItemClick = {},
        drawerState = rememberDrawerState(DrawerValue.Open),
        content = {}
    )
}

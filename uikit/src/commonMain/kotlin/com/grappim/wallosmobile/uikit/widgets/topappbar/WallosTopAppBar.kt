package com.grappim.wallosmobile.uikit.widgets.topappbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextOverflow
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.uikit_back_content_description
import com.grappim.wallosmobile.strings.generated.resources.uikit_menu_content_description
import com.grappim.wallosmobile.uikit.WallosMobilePreviewTheme
import com.grappim.wallosmobile.uikit.utils.PreviewWallosDarkLight
import com.grappim.wallosmobile.utils.ui.NativeText
import com.grappim.wallosmobile.utils.ui.asString
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallosTopAppBar(
    isVisible: Boolean,
    drawerState: DrawerState,
    topBarConfig: TopBarConfig,
    defaultGoBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { -it }),
        exit = slideOutVertically(targetOffsetY = { -it })
    ) {
        CenterAlignedTopAppBar(
            modifier = modifier,
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = topBarConfig.title.asString(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (topBarConfig.subtitle.isNotEmpty()) {
                        Text(
                            text = topBarConfig.subtitle.asString(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            navigationIcon = {
                when (val navIcon = topBarConfig.navigationIcon) {
                    is NavigationIconConfig.Back -> {
                        IconButton(onClick = navIcon.onBackClick ?: defaultGoBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(RString.uikit_back_content_description)
                            )
                        }
                    }

                    is NavigationIconConfig.Menu -> {
                        IconButton(
                            onClick = {
                                keyboardController?.hide()
                                scope.launch {
                                    if (drawerState.isClosed) {
                                        drawerState.open()
                                    } else {
                                        drawerState.close()
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = stringResource(RString.uikit_menu_content_description)
                            )
                        }
                    }

                    is NavigationIconConfig.Custom -> {
                        IconButton(onClick = navIcon.onClick) {
                            Icon(
                                painter = painterResource(navIcon.icon),
                                contentDescription = navIcon.contentDescription
                            )
                        }
                    }

                    NavigationIconConfig.None -> {}
                }
            },
            actions = {
                topBarConfig.actions.forEach { action ->
                    when (action) {
                        is TopBarActionIconButton -> {
                            IconButton(onClick = action.onClick) {
                                Icon(
                                    painter = painterResource(action.drawable),
                                    contentDescription = action.contentDescription
                                )
                            }
                        }

                        is TopBarActionVectorButton -> {
                            IconButton(onClick = action.onClick) {
                                Icon(
                                    imageVector = action.imageVector,
                                    contentDescription = action.contentDescription
                                )
                            }
                        }

                        is TopBarActionTextButton -> {
                            TextButton(onClick = action.onClick) {
                                Text(text = action.text.asString())
                            }
                        }
                    }
                }
            }
        )
    }
}

@PreviewWallosDarkLight
@Composable
private fun WallosTopAppBarPreview() = WallosMobilePreviewTheme {
    WallosTopAppBar(
        isVisible = true,
        drawerState = rememberDrawerState(DrawerValue.Closed),
        topBarConfig = TopBarConfig(
            title = NativeText.Simple("Subscriptions"),
            subtitle = NativeText.Simple("12 active"),
            navigationIcon = NavigationIconConfig.Menu,
            actions = persistentListOf(
                TopBarActionVectorButton(imageVector = Icons.Filled.Refresh, onClick = {})
            )
        ),
        defaultGoBack = {}
    )
}

package com.grappim.wallosmobile.uikit.widgets.topappbar

import com.grappim.wallosmobile.utils.ui.NativeText
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class TopBarConfig(
    val title: NativeText = NativeText.Empty,
    val subtitle: NativeText = NativeText.Empty,
    val navigationIcon: NavigationIconConfig = NavigationIconConfig.None,
    val actions: ImmutableList<TopBarAction> = persistentListOf()
)

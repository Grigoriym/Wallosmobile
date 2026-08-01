package com.grappim.wallosmobile.uikit.widgets.topappbar

import org.jetbrains.compose.resources.DrawableResource

sealed interface NavigationIconConfig {
    data object None : NavigationIconConfig

    data class Back(val onBackClick: (() -> Unit)? = null) : NavigationIconConfig

    data object Menu : NavigationIconConfig

    data class Custom(val icon: DrawableResource, val contentDescription: String, val onClick: () -> Unit) :
        NavigationIconConfig
}

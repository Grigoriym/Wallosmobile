package com.grappim.wallosmobile.uikit.widgets.topappbar

import androidx.compose.ui.graphics.vector.ImageVector
import com.grappim.wallosmobile.utils.ui.NativeText
import org.jetbrains.compose.resources.DrawableResource

sealed interface TopBarAction {
    val onClick: () -> Unit
}

data class TopBarActionIconButton(
    val drawable: DrawableResource,
    val contentDescription: String = "",
    override val onClick: () -> Unit
) : TopBarAction

data class TopBarActionVectorButton(
    val imageVector: ImageVector,
    val contentDescription: String = "",
    override val onClick: () -> Unit
) : TopBarAction

data class TopBarActionTextButton(val text: NativeText, override val onClick: () -> Unit) : TopBarAction

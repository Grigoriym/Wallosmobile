package com.grappim.wallosmobile.utils.ui

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * A string a non-UI layer can produce without touching a resource loader: the ViewModel picks the
 * variant, the Composable resolves it with [asString].
 */
sealed class NativeText {
    data object Empty : NativeText()

    data class Simple(val text: String) : NativeText()

    data class Resource(val stringResource: StringResource) : NativeText()

    fun isEmpty(): Boolean = this is Empty

    fun isNotEmpty(): Boolean = this !is Empty
}

@Composable
fun NativeText.asString(): String = when (this) {
    is NativeText.Empty -> ""
    is NativeText.Simple -> text
    is NativeText.Resource -> stringResource(stringResource)
}

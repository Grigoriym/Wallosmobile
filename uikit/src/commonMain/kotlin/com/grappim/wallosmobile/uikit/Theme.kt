package com.grappim.wallosmobile.uikit

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.grappim.wallosmobile.uikit.widgets.topappbar.LocalTopBarConfig
import com.grappim.wallosmobile.uikit.widgets.topappbar.TopBarController

internal val LightColorScheme = lightColorScheme(
    primary = Navy40,
    onPrimary = Color.White,
    primaryContainer = NavyContainerLight,
    onPrimaryContainer = OnNavyLight,
    secondary = SlateBlue40,
    onSecondary = Color.White,
    secondaryContainer = SlateBlueContainerLight,
    onSecondaryContainer = OnSlateBlueLight,
    tertiary = Mauve40,
    onTertiary = Color.White,
    tertiaryContainer = MauveContainerLight,
    onTertiaryContainer = OnMauveLight,
    error = Red40,
    onError = Color.White,
    errorContainer = RedContainerLight,
    onErrorContainer = OnRedLight,
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight
)

internal val DarkColorScheme = darkColorScheme(
    primary = Navy80,
    onPrimary = OnNavyDark,
    primaryContainer = NavyContainerDark,
    onPrimaryContainer = NavyContainerLight,
    secondary = SlateBlue80,
    onSecondary = OnSlateBlueDark,
    secondaryContainer = SlateBlueContainerDark,
    onSecondaryContainer = SlateBlueContainerLight,
    tertiary = Mauve80,
    onTertiary = OnMauveDark,
    tertiaryContainer = MauveContainerDark,
    onTertiaryContainer = MauveContainerLight,
    error = Red80,
    onError = OnRedDark,
    errorContainer = RedContainerDark,
    onErrorContainer = RedContainerLight,
    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark
)

@Composable
fun WallosMobileTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = WallosTypography,
        content = content
    )
}

/**
 * The theme every `@Preview` goes through. `Surface` is what gives previews a themed background
 * instead of a transparent one, and [LocalTopBarConfig] is provided because any screen that
 * declares its own top bar reads it — without it every screen preview crashes.
 */
@Composable
fun WallosMobilePreviewTheme(content: @Composable () -> Unit) {
    WallosMobileTheme {
        CompositionLocalProvider(LocalTopBarConfig provides remember { TopBarController() }) {
            Surface {
                content()
            }
        }
    }
}

package com.grappim.wallosmobile.uikit

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.grappim.wallosmobile.uikit.widgets.network.LocalIsOffline
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
    inversePrimary = Navy80,
    inverseSurface = InverseSurfaceLight,
    inverseOnSurface = InverseOnSurfaceLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    surfaceBright = SurfaceLight,
    surfaceDim = SurfaceDimLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight
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
    inversePrimary = Navy40,
    inverseSurface = InverseSurfaceDark,
    inverseOnSurface = InverseOnSurfaceDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    surfaceBright = SurfaceBrightDark,
    surfaceDim = SurfaceDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark
)

/**
 * The `Surface` is the theme's, not a caller's. It is the only thing that paints
 * `colorScheme.surface` and provides `LocalContentColor` on a screen that has no `Scaffold` — and
 * the login screen is exactly that screen. Without it the visible background is the *window's*
 * (see `androidApp`'s `themes.xml`) and Compose's default black wins for any text that doesn't
 * name a colour, which in dark mode is black on white.
 */
@Composable
fun WallosMobileTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = WallosTypography
    ) {
        Surface(modifier = Modifier.fillMaxSize(), content = content)
    }
}

/**
 * The theme every `@Preview` goes through. It adds only the composition locals the shell would
 * otherwise provide: [LocalTopBarConfig], because any screen that declares its own top bar reads it
 * and would crash without it, and [LocalIsOffline] for the same reason — a preview of the offline
 * variant provides that one again itself.
 *
 * It deliberately adds no `Surface` of its own: [WallosMobileTheme] owns that now, so a preview
 * renders on the same background and with the same `LocalContentColor` as the running app.
 */
@Composable
fun WallosMobilePreviewTheme(content: @Composable () -> Unit) {
    WallosMobileTheme {
        CompositionLocalProvider(
            LocalTopBarConfig provides remember { TopBarController() },
            LocalIsOffline provides false
        ) {
            content()
        }
    }
}

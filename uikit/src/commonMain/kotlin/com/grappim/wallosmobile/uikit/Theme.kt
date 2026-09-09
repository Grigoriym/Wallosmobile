package com.grappim.wallosmobile.uikit

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.grappim.kit.uikit.KitPreviewTheme
import com.grappim.kit.uikit.KitTheme
import com.grappim.wallosmobile.uikit.widgets.network.LocalIsOffline
import com.grappim.wallosmobile.uikit.widgets.snackbar.LocalSnackbarHostController
import com.grappim.wallosmobile.uikit.widgets.snackbar.SnackbarHostController

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
 * [KitTheme] owns the `Surface` that paints `colorScheme.surface` and provides
 * `LocalContentColor` on a screen that has no `Scaffold` — and the login screen is exactly that
 * screen. Without it the visible background is the *window's* (see `androidApp`'s `themes.xml`)
 * and Compose's default black wins for any text that doesn't name a colour, which in dark mode is
 * black on white. It also hardens [androidx.compose.ui.platform.LocalUriHandler] against
 * non-http(s) schemes (`SafeUriHandler`) — a superset of what this app needed on its own
 * (`docs/security/masvs.md`'s MASVS-CODE-4), but free and harmless since both existing
 * `openUri` call sites are already build-time `https://` URLs.
 */
@Composable
fun WallosMobileTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    KitTheme(
        lightColorScheme = LightColorScheme,
        darkColorScheme = DarkColorScheme,
        typography = WallosTypography,
        darkTheme = darkTheme,
        content = content
    )
}

/**
 * The theme every `@Preview` goes through. [KitPreviewTheme] wires `LocalTopBarConfig` — any
 * screen that declares its own top bar reads it and would crash without it. This wraps it with the
 * two composition locals `grappim-kit-uikit` has no reason to know about: [LocalIsOffline] for the
 * same crash-without-it reason (a preview of the offline variant provides that one again itself),
 * and [LocalSnackbarHostController] for a screen that ever calls into it directly rather than only
 * through the shell's own collector.
 */
@Composable
fun WallosMobilePreviewTheme(content: @Composable () -> Unit) {
    KitPreviewTheme(
        lightColorScheme = LightColorScheme,
        darkColorScheme = DarkColorScheme,
        typography = WallosTypography
    ) {
        CompositionLocalProvider(
            LocalIsOffline provides false,
            LocalSnackbarHostController provides remember { SnackbarHostController() }
        ) {
            content()
        }
    }
}

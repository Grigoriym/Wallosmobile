package com.grappim.wallosmobile.uikit

import androidx.compose.ui.graphics.Color
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * WCAG 2.x relative luminance / contrast ratio, computed directly from the [Color.kt] palette
 * constants rather than eyeballed on device — every pair here is normal-text UI (labels, body
 * text), so all twenty are held to the 4.5:1 AA bar rather than the 3.0:1 large-text one.
 */
private const val AA_NORMAL_TEXT_MIN_CONTRAST = 4.5

private fun linearize(channel: Float): Double {
    val c = channel.toDouble()
    return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
}

private fun relativeLuminance(color: Color): Double {
    val r = linearize(color.red)
    val g = linearize(color.green)
    val b = linearize(color.blue)
    return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

private fun contrastRatio(a: Color, b: Color): Double {
    val lumA = relativeLuminance(a)
    val lumB = relativeLuminance(b)
    val lighter = maxOf(lumA, lumB)
    val darker = minOf(lumA, lumB)
    return (lighter + 0.05) / (darker + 0.05)
}

class ContrastTest {

    private val lightPairs = listOf(
        "primary/onPrimary" to (Navy40 to Color.White),
        "primaryContainer/onPrimaryContainer" to (NavyContainerLight to OnNavyLight),
        "secondary/onSecondary" to (SlateBlue40 to Color.White),
        "secondaryContainer/onSecondaryContainer" to (SlateBlueContainerLight to OnSlateBlueLight),
        "tertiary/onTertiary" to (Mauve40 to Color.White),
        "tertiaryContainer/onTertiaryContainer" to (MauveContainerLight to OnMauveLight),
        "error/onError" to (Red40 to Color.White),
        "errorContainer/onErrorContainer" to (RedContainerLight to OnRedLight),
        "surface/onSurface" to (SurfaceLight to OnSurfaceLight),
        "surfaceVariant/onSurfaceVariant" to (SurfaceVariantLight to OnSurfaceVariantLight)
    )

    private val darkPairs = listOf(
        "primary/onPrimary" to (Navy80 to OnNavyDark),
        "primaryContainer/onPrimaryContainer" to (NavyContainerDark to NavyContainerLight),
        "secondary/onSecondary" to (SlateBlue80 to OnSlateBlueDark),
        "secondaryContainer/onSecondaryContainer" to (SlateBlueContainerDark to SlateBlueContainerLight),
        "tertiary/onTertiary" to (Mauve80 to OnMauveDark),
        "tertiaryContainer/onTertiaryContainer" to (MauveContainerDark to MauveContainerLight),
        "error/onError" to (Red80 to OnRedDark),
        "errorContainer/onErrorContainer" to (RedContainerDark to RedContainerLight),
        "surface/onSurface" to (SurfaceDark to OnSurfaceDark),
        "surfaceVariant/onSurfaceVariant" to (SurfaceVariantDark to OnSurfaceVariantDark)
    )

    @Test
    fun `LightColorScheme pairs meet WCAG AA normal-text contrast`() {
        lightPairs.forEach { (name, pair) ->
            val (background, foreground) = pair
            val ratio = contrastRatio(background, foreground)
            assertTrue(
                ratio >= AA_NORMAL_TEXT_MIN_CONTRAST,
                "light $name contrast is $ratio, below $AA_NORMAL_TEXT_MIN_CONTRAST:1"
            )
        }
    }

    @Test
    fun `DarkColorScheme pairs meet WCAG AA normal-text contrast`() {
        darkPairs.forEach { (name, pair) ->
            val (background, foreground) = pair
            val ratio = contrastRatio(background, foreground)
            assertTrue(
                ratio >= AA_NORMAL_TEXT_MIN_CONTRAST,
                "dark $name contrast is $ratio, below $AA_NORMAL_TEXT_MIN_CONTRAST:1"
            )
        }
    }
}

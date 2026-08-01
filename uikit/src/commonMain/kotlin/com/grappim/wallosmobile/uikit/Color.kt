package com.grappim.wallosmobile.uikit

import androidx.compose.ui.graphics.Color

/**
 * A Material 3 palette seeded from the Wallos logo navy (`#233E67`).
 *
 * The names are Material's tonal ones — `40` is the light-theme tone, `80` the dark-theme one.
 */

// Primary — the logo navy.
val Navy40 = Color(0xFF2A4A7C)
val Navy80 = Color(0xFFA9C7FF)
val NavyContainerLight = Color(0xFFD6E3FF)
val NavyContainerDark = Color(0xFF264777)
val OnNavyLight = Color(0xFF001B3D)
val OnNavyDark = Color(0xFF0A305F)

// Secondary — the navy desaturated.
val SlateBlue40 = Color(0xFF565E71)
val SlateBlue80 = Color(0xFFBEC6DC)
val SlateBlueContainerLight = Color(0xFFDAE2F9)
val SlateBlueContainerDark = Color(0xFF3E4759)
val OnSlateBlueLight = Color(0xFF131C2B)
val OnSlateBlueDark = Color(0xFF283141)

// Tertiary — the accent Material pairs with this hue.
val Mauve40 = Color(0xFF6F5575)
val Mauve80 = Color(0xFFDCBCE1)
val MauveContainerLight = Color(0xFFF9D8FF)
val MauveContainerDark = Color(0xFF553F5C)
val OnMauveLight = Color(0xFF28132E)
val OnMauveDark = Color(0xFF3E2845)

// Error — Material's baseline reds; nothing in the brand maps to them.
val Red40 = Color(0xFFBA1A1A)
val Red80 = Color(0xFFFFB4AB)
val RedContainerLight = Color(0xFFFFDAD6)
val RedContainerDark = Color(0xFF93000A)
val OnRedLight = Color(0xFF410002)
val OnRedDark = Color(0xFF690005)

// Neutrals.
val SurfaceLight = Color(0xFFFDFBFF)
val SurfaceDark = Color(0xFF1A1B1F)
val SurfaceVariantLight = Color(0xFFE1E2EC)
val SurfaceVariantDark = Color(0xFF44474F)
val OnSurfaceLight = Color(0xFF1A1B1F)
val OnSurfaceDark = Color(0xFFE3E2E6)
val OnSurfaceVariantLight = Color(0xFF44474F)
val OnSurfaceVariantDark = Color(0xFFC4C6D0)
val OutlineLight = Color(0xFF74777F)
val OutlineDark = Color(0xFF8E9099)

/** [SurfaceDark] as the plain RGB `Long` that `@Preview(backgroundColor = …)` wants. */
const val DARK_BACKGROUND_COLOR_FOR_PREVIEW = 0x1A1B1FL

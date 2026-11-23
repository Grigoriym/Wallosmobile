package com.grappim.wallosmobile.uikit

import androidx.compose.ui.unit.dp

/**
 * Bottom content padding for a scrolling list on a screen that shows the shell's FAB. `Scaffold`
 * leaves the FAB out of the padding it hands to content, so without this the last row scrolls no
 * higher than the FAB and stays under it.
 *
 * 56dp FAB (`FabBaselineTokens.ContainerHeight`) + 16dp `Scaffold` places it above the bottom
 * edge + 16dp so the last row ends the usual screen padding above the FAB, not flush against it.
 */
val FabClearance = 88.dp

package com.grappim.wallosmobile.uikit.utils

import androidx.compose.ui.tooling.preview.AndroidUiModes
import androidx.compose.ui.tooling.preview.Preview
import com.grappim.wallosmobile.uikit.DARK_BACKGROUND_COLOR_FOR_PREVIEW

@Preview(
    showBackground = true,
    uiMode = AndroidUiModes.UI_MODE_NIGHT_NO,
    name = "Light theme"
)
@Preview(
    showBackground = true,
    backgroundColor = DARK_BACKGROUND_COLOR_FOR_PREVIEW,
    uiMode = AndroidUiModes.UI_MODE_NIGHT_YES,
    name = "Dark theme"
)
annotation class PreviewWallosDarkLight

package com.grappim.wallosmobile

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.grappim.wallosmobile.composeapp.WallosAppContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            WallosAppContent(onDarkThemeChange = ::applyEdgeToEdge)
        }
    }

    /**
     * `enableEdgeToEdge()`'s default styles detect dark mode from the *resource* configuration, so
     * the system-bar icons follow the device's night mode rather than the app's theme — invisible
     * the moment a stored Light or Dark disagrees with it (4.2). Only the detection changes here;
     * the scrims are the platform values the default already uses. Re-calling is what the API is
     * for — it re-applies to the same window.
     */
    private fun applyEdgeToEdge(darkTheme: Boolean) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
            navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { darkTheme }
        )
    }

    private companion object {
        // androidx's own DefaultLightScrim / DefaultDarkScrim, which are internal to it. They are
        // drawn on API 28 and below only (minSdk is 24), where the navigation bar can't be
        // transparent; from 29 the platform enforces its own contrast.
        private val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        private val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}

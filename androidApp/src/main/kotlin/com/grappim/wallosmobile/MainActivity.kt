package com.grappim.wallosmobile

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.lifecycle.lifecycleScope
import com.grappim.wallosmobile.composeapp.WallosAppContent
import com.grappim.wallosmobile.di.AppUpdateChecker
import com.grappim.wallosmobile.di.UpdateState
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.app_update_downloaded
import com.grappim.wallosmobile.strings.generated.resources.app_update_restart
import com.grappim.wallosmobile.uikit.widgets.snackbar.SnackbarHostController
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val appUpdateChecker: AppUpdateChecker by inject()
    private val snackbarHostController = SnackbarHostController()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        appUpdateChecker.checkAndRequestUpdate(this)
        observeUpdateState()

        setContent {
            WallosAppContent(
                onDarkThemeChange = ::applyEdgeToEdge,
                snackbarHostController = snackbarHostController
            )
        }
    }

    override fun onResume() {
        super.onResume()
        appUpdateChecker.registerUpdateListener()
        appUpdateChecker.checkUpdateStateOnResume()
    }

    override fun onPause() {
        super.onPause()
        appUpdateChecker.unregisterUpdateListener()
    }

    /**
     * Plain `lifecycleScope`, not a Compose `LaunchedEffect` — `AppUpdateChecker`/`UpdateState`
     * are `androidApp`-only types that can't be named in `composeApp`/`uikit` code (the Gradle
     * dependency runs the other way), so this stays here rather than crossing that boundary. Only
     * the plain `SnackbarHostController` (from `uikit`) is shared with the Compose shell, the same
     * instance `WallosAppContent` renders through `Scaffold`'s `snackbarHost` slot.
     */
    private fun observeUpdateState() {
        lifecycleScope.launch {
            appUpdateChecker.updateState.collectLatest { state ->
                when (state) {
                    UpdateState.UpdateDownloaded -> showRestartSnackbar()
                }
            }
        }
    }

    private suspend fun showRestartSnackbar() {
        val result = snackbarHostController.show(
            message = getString(RString.app_update_downloaded),
            actionLabel = getString(RString.app_update_restart),
            duration = SnackbarDuration.Indefinite
        )
        if (result == SnackbarResult.ActionPerformed) {
            appUpdateChecker.completeUpdate()
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

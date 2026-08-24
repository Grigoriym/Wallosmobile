package com.grappim.wallosmobile.uikit.widgets.snackbar

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.compositionLocalOf

val LocalSnackbarHostController = compositionLocalOf<SnackbarHostController> {
    error("SnackbarHostController not provided")
}

/**
 * The shell owns one of these and renders [hostState] through a `SnackbarHost`; anything reachable
 * through [LocalSnackbarHostController] calls [show] to surface a message — same shape as
 * [com.grappim.wallosmobile.uikit.widgets.topappbar.TopBarController], one level simpler because
 * `SnackbarHostState` already carries its own queueing state.
 */
class SnackbarHostController {
    val hostState = SnackbarHostState()

    suspend fun show(
        message: String,
        actionLabel: String? = null,
        duration: SnackbarDuration = SnackbarDuration.Short
    ): SnackbarResult = hostState.showSnackbar(message = message, actionLabel = actionLabel, duration = duration)
}

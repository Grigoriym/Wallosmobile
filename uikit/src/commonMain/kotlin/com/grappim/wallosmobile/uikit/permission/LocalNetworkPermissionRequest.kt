package com.grappim.wallosmobile.uikit.permission

import androidx.compose.runtime.Composable

/**
 * Asks for local network access when a server needs it, before anything is sent to that server
 * (plan §4.5c). [request] calls `onAllowed` when the app may go ahead, which includes every server
 * that isn't on the local network, and `onDenied` only when that server is certain to be blocked.
 */
fun interface LocalNetworkPermissionRequest {
    fun request(serverUrl: String, onAllowed: () -> Unit, onDenied: () -> Unit)
}

@Composable
expect fun rememberLocalNetworkPermissionRequest(): LocalNetworkPermissionRequest

/** Whether [serverUrl] is on the local network with the permission off, checked again on every resume. */
@Composable
expect fun rememberIsLocalNetworkBlocked(serverUrl: String): Boolean

/**
 * Calls [onAllow] when local network access turns on while this is composed, from the dialog or
 * from the system settings page, so a screen that failed while it was off loads again (plan §4.5c).
 */
@Composable
expect fun LocalNetworkAllowEffect(onAllow: () -> Unit)

/** Opens the system's page for this app, the way back once the permission dialog no longer appears. */
@Composable
expect fun rememberOpenAppSettings(): () -> Unit

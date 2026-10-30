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

package com.grappim.wallosmobile.core.storage

import kotlinx.coroutines.flow.StateFlow

/**
 * Whether the device currently has a network at all.
 *
 * A platform seam rather than an `expect`/`actual`, for the same reason as [SecretCipher]: the
 * implementation talks to `ConnectivityManager`, which a host test has no way to reach. As an
 * interface it can be faked, so everything that *reacts* to connectivity stays testable.
 *
 * This says nothing about whether the Wallos instance is reachable — a self-hosted server on a
 * LAN the phone has just left is "online" here and still fails the request.
 */
interface NetworkMonitor {

    /**
     * `true` while any network with internet capability is up. A [StateFlow], so a collector
     * composes with the current state instead of waiting a pass for the first emission.
     */
    val isOnline: StateFlow<Boolean>
}

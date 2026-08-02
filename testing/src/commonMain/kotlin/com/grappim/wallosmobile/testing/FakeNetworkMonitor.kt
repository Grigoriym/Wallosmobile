package com.grappim.wallosmobile.testing

import com.grappim.wallosmobile.core.storage.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Lets a test drive connectivity, which the real [NetworkMonitor] cannot do off a device.
 *
 * Unlike most fakes here it has no `error("… not set")`: an initial state of `true` is the
 * honest default rather than an unarranged one, and a test that cares says so.
 */
class FakeNetworkMonitor(initialOnline: Boolean = true) : NetworkMonitor {

    private val _isOnline = MutableStateFlow(initialOnline)
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    fun setOnline(online: Boolean) {
        _isOnline.value = online
    }
}

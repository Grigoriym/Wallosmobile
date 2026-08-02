package com.grappim.wallosmobile.core.storage

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Single

/**
 * `ACCESS_NETWORK_STATE` is declared in `:androidApp`'s manifest, which this module has no share
 * of — hence the suppression rather than a missing permission.
 */
@SuppressLint("MissingPermission")
@Single(binds = [NetworkMonitor::class])
class NetworkMonitorImpl(context: Context) : NetworkMonitor {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _isOnline = MutableStateFlow(checkCurrentConnectivity())
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    /**
     * Every network with internet capability, not just the active one: `onLost` for Wi-Fi while
     * mobile data is still up must not read as offline.
     */
    private val networks = mutableSetOf<Network>()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            networks += network
            _isOnline.value = true
        }

        override fun onLost(network: Network) {
            networks -= network
            _isOnline.value = networks.isNotEmpty()
        }
    }

    init {
        if (connectivityManager != null) {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
        }
    }

    /**
     * The value the flow starts at, since the callback only reports *changes*. A `@Single` that
     * lives as long as the process, so the callback is never unregistered.
     */
    private fun checkCurrentConnectivity(): Boolean {
        val manager = connectivityManager ?: return false
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

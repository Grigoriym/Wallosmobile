package com.grappim.wallosmobile.uikit.permission

import java.net.InetAddress
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocalNetworkAddressTest {

    private fun address(literal: String) = InetAddress.getByName(literal)

    @Test
    fun privateAndLinkLocalAddressesAreLocal() {
        listOf("192.168.0.241", "10.0.2.2", "172.16.0.1", "169.254.1.1", "fe80::1", "fd12:3456::1", "fc00::1")
            .forEach { assertTrue(address(it).isLocalNetworkAddress(), it) }
    }

    @Test
    fun publicAndLoopbackAddressesAreNotLocal() {
        listOf("1.1.1.1", "172.32.0.1", "127.0.0.1", "::1", "2606:4700:4700::1111")
            .forEach { assertFalse(address(it).isLocalNetworkAddress(), it) }
    }
}

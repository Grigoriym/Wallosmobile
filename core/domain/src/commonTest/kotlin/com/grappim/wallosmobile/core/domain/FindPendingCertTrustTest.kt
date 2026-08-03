package com.grappim.wallosmobile.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FindPendingCertTrustTest {

    @Test
    fun `finds nothing in an ordinary transport failure`() {
        assertNull(IllegalStateException("connection refused").findPendingCertTrust())
    }

    @Test
    fun `finds the certificate when it is the thrown exception itself`() {
        val exception = UntrustedCertificateException(pendingCertTrust)

        assertEquals(pendingCertTrust, exception.findPendingCertTrust())
    }

    @Test
    fun `finds the certificate however deep the tls stack buried it`() {
        // The real chain: SSLHandshakeException(CertificateException(UntrustedCertificateException)).
        val wrapped = IllegalStateException(IllegalStateException(UntrustedCertificateException(pendingCertTrust)))

        assertEquals(pendingCertTrust, wrapped.findPendingCertTrust())
    }

    private val pendingCertTrust = PendingCertTrust(
        host = "wallos.example.com",
        subject = "CN=wallos.example.com",
        issuer = "CN=Homelab CA",
        notBefore = "2026-01-01",
        notAfter = "2027-01-01",
        sha256Fingerprint = "AA:BB:CC"
    )
}
